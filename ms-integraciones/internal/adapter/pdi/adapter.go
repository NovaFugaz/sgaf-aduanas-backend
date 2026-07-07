package pdi

import "context"

type PDIResponse struct {
	Habilitado bool   `json:"habilitado"`
	Nombre     string `json:"nombre,omitempty"`
	Alertas    int    `json:"alertas"`
	Alerta     string `json:"alerta,omitempty"`
}

type PDIAdapter interface {
	ConsultarRUT(ctx context.Context, rut string) (*PDIResponse, error)
}
