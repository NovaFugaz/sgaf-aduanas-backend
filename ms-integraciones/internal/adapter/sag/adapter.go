package sag

import "context"

type SAGRequest struct {
	TramiteID               string `json:"tramite_id"`
	TieneAlimentos          bool   `json:"tiene_alimentos"`
	TieneProductosVegetales bool   `json:"tiene_productos_vegetales"`
	TieneProductosAnimales  bool   `json:"tiene_productos_animales"`
	TieneMascotas           bool   `json:"tiene_mascotas"`
	Descripcion             string `json:"descripcion"`
}

type SAGResponse struct {
	Estado        string `json:"estado"`
	CodigoSAG     string `json:"codigo_sag"`
	Observaciones string `json:"observaciones"`
}

type SAGAdapter interface {
	ValidarDeclaracion(ctx context.Context, req SAGRequest) (*SAGResponse, error)
}
