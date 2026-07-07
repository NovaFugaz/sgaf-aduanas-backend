package service

import (
	"context"
	"encoding/json"
	"errors"
	"strings"
	"sync"
	"time"

	"github.com/sony/gobreaker"
	"github.com/sgaf/ms-integraciones/internal/adapter/aduana_arg"
	"github.com/sgaf/ms-integraciones/internal/adapter/pdi"
	"github.com/sgaf/ms-integraciones/internal/adapter/sag"
	"github.com/sgaf/ms-integraciones/internal/db"
)

type QueryRecord struct {
	Timestamp time.Time
	IsError   bool
}

type AdapterTracker struct {
	LastQuery time.Time
	History   []QueryRecord
	mu        sync.RWMutex
}

func (t *AdapterTracker) Record(isError bool) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.LastQuery = time.Now()
	t.History = append(t.History, QueryRecord{
		Timestamp: t.LastQuery,
		IsError:   isError,
	})
}

func (t *AdapterTracker) GetStats() (time.Time, float64) {
	t.mu.Lock()
	defer t.mu.Unlock()

	fiveMinsAgo := time.Now().Add(-5 * time.Minute)
	var active []QueryRecord
	errorsCount := 0

	for _, r := range t.History {
		if r.Timestamp.After(fiveMinsAgo) {
			active = append(active, r)
			if r.IsError {
				errorsCount++
			}
		}
	}

	t.History = active

	total := len(active)
	if total == 0 {
		return t.LastQuery, 0.0
	}
	return t.LastQuery, float64(errorsCount) / float64(total)
}

type IntegracionService interface {
	ConsultarPDI(ctx context.Context, rut string) (*pdi.PDIResponse, int, error)
	ValidarSAG(ctx context.Context, req sag.SAGRequest) (*sag.SAGResponse, int, error)
	ConsultarAduanaArg(ctx context.Context, patente string, tipoConsulta string) (interface{}, int, error)
	GetEstado() map[string]interface{}
	GetCBPDI() *gobreaker.CircuitBreaker
	GetCBSAG() *gobreaker.CircuitBreaker
	GetCBAduana() *gobreaker.CircuitBreaker
}

type integracionService struct {
	pdiAdapter pdi.PDIAdapter
	sagAdapter sag.SAGAdapter
	argAdapter aduana_arg.AduanaArgAdapter
	logRepo    db.IntegracionLogRepo

	cbPDI    *gobreaker.CircuitBreaker
	cbSAG    *gobreaker.CircuitBreaker
	cbAduana *gobreaker.CircuitBreaker

	trackerPDI    *AdapterTracker
	trackerSAG    *AdapterTracker
	trackerAduana *AdapterTracker
}

func NewIntegracionService(
	pdiAdapter pdi.PDIAdapter,
	sagAdapter sag.SAGAdapter,
	argAdapter aduana_arg.AduanaArgAdapter,
	logRepo db.IntegracionLogRepo,
) IntegracionService {
	cbSettings := func(name string) gobreaker.Settings {
		return gobreaker.Settings{
			Name:        name,
			MaxRequests: 2,
			Interval:    30 * time.Second,
			Timeout:     15 * time.Second,
			ReadyToTrip: func(counts gobreaker.Counts) bool {
				return counts.ConsecutiveFailures >= 3
			},
		}
	}

	return &integracionService{
		pdiAdapter:    pdiAdapter,
		sagAdapter:    sagAdapter,
		argAdapter:    argAdapter,
		logRepo:       logRepo,
		cbPDI:         gobreaker.NewCircuitBreaker(cbSettings("pdi")),
		cbSAG:         gobreaker.NewCircuitBreaker(cbSettings("sag")),
		cbAduana:      gobreaker.NewCircuitBreaker(cbSettings("aduana_argentina")),
		trackerPDI:    &AdapterTracker{},
		trackerSAG:    &AdapterTracker{},
		trackerAduana: &AdapterTracker{},
	}
}

func (s *integracionService) logCall(ctx context.Context, sistema, operacion string, reqData interface{}, respData interface{}, err error, latenciaMs int, tramiteID *string) {
	var reqBytes, respBytes []byte
	if reqData != nil {
		reqBytes, _ = json.Marshal(reqData)
	}
	if respData != nil {
		respBytes, _ = json.Marshal(respData)
	}

	estado := "EXITO"
	if err != nil {
		if errors.Is(err, gobreaker.ErrOpenState) {
			estado = "CIRCUIT_OPEN"
		} else if strings.Contains(err.Error(), "timeout") || errors.Is(err, context.DeadlineExceeded) {
			estado = "TIMEOUT"
		} else {
			estado = "ERROR"
		}
	}

	logEntry := &db.IntegrationLog{
		Sistema:      sistema,
		Operacion:    operacion,
		RequestData:  reqBytes,
		ResponseData: respBytes,
		Estado:       estado,
		LatenciaMs:   latenciaMs,
		TramiteID:    tramiteID,
	}

	// Logging to DB is logged but failures do not block the handler response
	_ = s.logRepo.Insert(ctx, logEntry)
}

func (s *integracionService) ConsultarPDI(ctx context.Context, rut string) (*pdi.PDIResponse, int, error) {
	start := time.Now()
	reqBody := map[string]string{"rut": rut}

	res, err := s.cbPDI.Execute(func() (interface{}, error) {
		return s.pdiAdapter.ConsultarRUT(ctx, rut)
	})

	latencia := int(time.Since(start).Milliseconds())
	s.trackerPDI.Record(err != nil)

	var pdiResp *pdi.PDIResponse
	if err == nil {
		pdiResp = res.(*pdi.PDIResponse)
	}

	s.logCall(ctx, "PDI", "CONSULTAR_RUT", reqBody, pdiResp, err, latencia, nil)

	if err != nil {
		return nil, latencia, err
	}
	return pdiResp, latencia, nil
}

func (s *integracionService) ValidarSAG(ctx context.Context, req sag.SAGRequest) (*sag.SAGResponse, int, error) {
	start := time.Now()
	var tramiteID *string
	if req.TramiteID != "" {
		tramiteID = &req.TramiteID
	}

	res, err := s.cbSAG.Execute(func() (interface{}, error) {
		return s.sagAdapter.ValidarDeclaracion(ctx, req)
	})

	latencia := int(time.Since(start).Milliseconds())
	s.trackerSAG.Record(err != nil)

	var sagResp *sag.SAGResponse
	if err == nil {
		sagResp = res.(*sag.SAGResponse)
	}

	s.logCall(ctx, "SAG", "VALIDAR_DECLARACION", req, sagResp, err, latencia, tramiteID)

	if err != nil {
		return nil, latencia, err
	}
	return sagResp, latencia, nil
}

func (s *integracionService) ConsultarAduanaArg(ctx context.Context, patente string, tipoConsulta string) (interface{}, int, error) {
	start := time.Now()
	reqBody := map[string]string{"patente": patente, "tipo_consulta": tipoConsulta}

	var res interface{}
	var err error

	if strings.ToLower(tipoConsulta) == "habilitacion" {
		res, err = s.cbAduana.Execute(func() (interface{}, error) {
			return s.argAdapter.VerificarHabilitacion(ctx, patente)
		})
	} else {
		res, err = s.cbAduana.Execute(func() (interface{}, error) {
			return s.argAdapter.ConsultarVehiculo(ctx, patente)
		})
	}

	latencia := int(time.Since(start).Milliseconds())
	s.trackerAduana.Record(err != nil)

	s.logCall(ctx, "ADUANA_ARG", "CONSULTAR_VEHICULO", reqBody, res, err, latencia, nil)

	if err != nil {
		return nil, latencia, err
	}
	return res, latencia, nil
}

func (s *integracionService) GetCBPDI() *gobreaker.CircuitBreaker {
	return s.cbPDI
}

func (s *integracionService) GetCBSAG() *gobreaker.CircuitBreaker {
	return s.cbSAG
}

func (s *integracionService) GetCBAduana() *gobreaker.CircuitBreaker {
	return s.cbAduana
}

func (s *integracionService) getAdapterState(cb *gobreaker.CircuitBreaker, tracker *AdapterTracker) map[string]interface{} {
	stats := make(map[string]interface{})

	cbState := cb.State()
	estado := "CONECTADO"
	if cbState == gobreaker.StateOpen {
		estado = "CIRCUITO_ABIERTO"
	} else if cbState == gobreaker.StateHalfOpen {
		estado = "DEGRADADO"
	}

	lastQuery, errRate := tracker.GetStats()
	
	// If error rate is high, report as DEGRADADO
	if estado == "CONECTADO" && errRate > 0.10 {
		estado = "DEGRADADO"
	}

	stats["estado"] = estado
	if !lastQuery.IsZero() {
		stats["ultima_consulta"] = lastQuery.Format(time.RFC3339)
	} else {
		stats["ultima_consulta"] = nil
	}
	stats["tasa_error_5min"] = errRate

	return stats
}

func (s *integracionService) GetEstado() map[string]interface{} {
	return map[string]interface{}{
		"pdi":              s.getAdapterState(s.cbPDI, s.trackerPDI),
		"sag":              s.getAdapterState(s.cbSAG, s.trackerSAG),
		"aduana_argentina": s.getAdapterState(s.cbAduana, s.trackerAduana),
	}
}
