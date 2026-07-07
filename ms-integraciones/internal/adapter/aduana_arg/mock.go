package aduana_arg

import (
	"context"
	"errors"
	"math/rand"
	"regexp"
	"time"
)

type mockAduanaArgAdapter struct {
	newFormatRegex *regexp.Regexp
	oldFormatRegex *regexp.Regexp
	owners         []string
}

func NewMockAduanaArgAdapter() AduanaArgAdapter {
	return &mockAduanaArgAdapter{
		newFormatRegex: regexp.MustCompile(`^[A-Z]{2}\d{3}[A-Z]{2}$`),
		oldFormatRegex: regexp.MustCompile(`^[A-Z]{3}\d{3}$`),
		owners: []string{
			"Mateo Fernández", "Sofía Rodríguez", "Santiago Bianchi", "Valentina Rossi",
			"Bautista López", "Martina Gómez", "Joaquín Díaz", "Catalina Álvarez",
			"Felipe Romero", "Delfina Sosa",
		},
	}
}

func (m *mockAduanaArgAdapter) checkPlate(patente string) (bool, string) {
	if patente == "INVALID00" {
		return false, "Vehículo con restricción aduanera"
	}
	if m.newFormatRegex.MatchString(patente) || m.oldFormatRegex.MatchString(patente) {
		return true, ""
	}
	return false, "Formato de patente argentina inválido"
}

func (m *mockAduanaArgAdapter) ConsultarVehiculo(ctx context.Context, patente string) (*VehiculoResponse, error) {
	latency := time.Duration(1500+rand.Intn(1001)) * time.Millisecond
	select {
	case <-time.After(latency):
	case <-ctx.Done():
		return nil, ctx.Err()
	}

	if rand.Float64() < 0.08 {
		return nil, errors.New("Aduana Argentina connection timeout")
	}

	habilitado, motivo := m.checkPlate(patente)
	owner := m.owners[rand.Intn(len(m.owners))]

	return &VehiculoResponse{
		Patente:    patente,
		Habilitado: habilitado,
		Titular:    owner,
		Motivo:     motivo,
	}, nil
}

func (m *mockAduanaArgAdapter) VerificarHabilitacion(ctx context.Context, patente string) (*HabilitacionResponse, error) {
	latency := time.Duration(1500+rand.Intn(1001)) * time.Millisecond
	select {
	case <-time.After(latency):
	case <-ctx.Done():
		return nil, ctx.Err()
	}

	if rand.Float64() < 0.08 {
		return nil, errors.New("Aduana Argentina connection timeout")
	}

	habilitado, motivo := m.checkPlate(patente)
	dias := 0
	if habilitado {
		dias = 180
	}

	return &HabilitacionResponse{
		Patente:       patente,
		Habilitado:    habilitado,
		DiasRestantes: dias,
		Motivo:        motivo,
	}, nil
}
