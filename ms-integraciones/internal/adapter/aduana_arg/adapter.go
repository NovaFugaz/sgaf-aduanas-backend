package aduana_arg

import "context"

type VehiculoResponse struct {
	Patente    string `json:"patente"`
	Habilitado bool   `json:"habilitado"`
	Titular    string `json:"titular"`
	Motivo     string `json:"motivo,omitempty"`
}

type HabilitacionResponse struct {
	Patente       string `json:"patente"`
	Habilitado    bool   `json:"habilitado"`
	DiasRestantes int    `json:"dias_restantes"`
	Motivo        string `json:"motivo,omitempty"`
}

type AduanaArgAdapter interface {
	ConsultarVehiculo(ctx context.Context, patente string) (*VehiculoResponse, error)
	VerificarHabilitacion(ctx context.Context, patente string) (*HabilitacionResponse, error)
}
