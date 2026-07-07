package sag

import (
	"context"
	"errors"
	"fmt"
	"math/rand"
	"time"
)

type mockSAGAdapter struct{}

func NewMockSAGAdapter() SAGAdapter {
	return &mockSAGAdapter{}
}

func (m *mockSAGAdapter) ValidarDeclaracion(ctx context.Context, req SAGRequest) (*SAGResponse, error) {
	// Simulated latency 1000ms - 2000ms
	latency := time.Duration(1000+rand.Intn(1001)) * time.Millisecond
	select {
	case <-time.After(latency):
	case <-ctx.Done():
		return nil, ctx.Err()
	}

	// 3% random timeout error
	if rand.Float64() < 0.03 {
		return nil, errors.New("SAG service gateway timeout")
	}

	codigo := fmt.Sprintf("SAG-%06d", rand.Intn(1000000))

	if req.TieneProductosAnimales && req.TieneAlimentos {
		if rand.Float64() < 0.60 {
			return &SAGResponse{
				Estado:        "REQUIERE_REVISION",
				CodigoSAG:     codigo,
				Observaciones: "Presencia conjunta de alimentos y productos de origen animal requiere inspección física obligatoria.",
			}, nil
		}
	} else {
		if rand.Float64() < 0.20 {
			return &SAGResponse{
				Estado:        "REQUIERE_REVISION",
				CodigoSAG:     codigo,
				Observaciones: "Declaración seleccionada para revisión física preventiva por control aleatorio.",
			}, nil
		}
	}

	return &SAGResponse{
		Estado:        "APROBADO",
		CodigoSAG:     codigo,
		Observaciones: "Declaración aprobada automáticamente sin riesgo fitosanitario detectado.",
	}, nil
}
