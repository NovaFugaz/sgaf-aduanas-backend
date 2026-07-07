package pdi

import (
	"context"
	"errors"
	"math/rand"
	"time"
)

type mockPDIAdapter struct {
	names  []string
	alerts []string
}

func NewMockPDIAdapter() PDIAdapter {
	return &mockPDIAdapter{
		names: []string{
			"Juan Perez", "Maria Gonzalez", "Carlos Rodriguez", "Ana Morales",
			"Luis Soto", "Pedro Muñoz", "Sofia Silva", "Diego Contreras",
			"Laura Rojas", "Gabriel Diaz",
		},
		alerts: []string{
			"Orden de arraigo nacional activo",
			"Documento reportado como extraviado",
			"Alerta migratoria vigente",
			"Restricción judicial de salida",
		},
	}
}

func (m *mockPDIAdapter) ConsultarRUT(ctx context.Context, rut string) (*PDIResponse, error) {
	// Simulated latency 800ms - 1600ms
	latency := time.Duration(800+rand.Intn(801)) * time.Millisecond
	select {
	case <-time.After(latency):
	case <-ctx.Done():
		return nil, ctx.Err()
	}

	// 5% random connection error
	if rand.Float64() < 0.05 {
		return nil, errors.New("PDI connection error: network timeout")
	}

	if rut == "12345678-9" {
		return &PDIResponse{
			Habilitado: true,
			Nombre:     "Juan Carlos Bodoque",
			Alertas:    0,
		}, nil
	}

	if rut == "99999999-9" {
		return &PDIResponse{
			Habilitado: false,
			Alerta:     "Orden de arraigo nacional activo",
			Alertas:    1,
		}, nil
	}

	if rand.Float64() < 0.85 {
		name := m.names[rand.Intn(len(m.names))]
		return &PDIResponse{
			Habilitado: true,
			Nombre:     name,
			Alertas:    0,
		}, nil
	}

	alert := m.alerts[rand.Intn(len(m.alerts))]
	return &PDIResponse{
		Habilitado: false,
		Alerta:     alert,
		Alertas:    1,
	}, nil
}
