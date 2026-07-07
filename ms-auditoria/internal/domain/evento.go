package domain

import "time"

type EventoAuditoria struct {
	ID          string    `json:"id"`
	Entidad     string    `json:"entidad" binding:"required"`
	EntidadID   string    `json:"entidad_id" binding:"required"`
	Accion      string    `json:"accion" binding:"required"`
	UsuarioID   string    `json:"usuario_id" binding:"required"`
	UsuarioRol  string    `json:"usuario_rol" binding:"required"`
	Detalle     string    `json:"detalle"`
	IPOrigen    string    `json:"ip_origen"`
	Timestamp   time.Time `json:"timestamp"`
}
