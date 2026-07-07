package handler

import (
	"encoding/csv"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/sgaf/ms-auditoria/internal/domain"
	"github.com/sgaf/ms-auditoria/internal/service"
)

type ErrorResponse struct {
	Code    string  `json:"code"`
	Message string  `json:"message"`
	Field   *string `json:"field,omitempty"`
}

type APIResponse struct {
	Data  interface{}    `json:"data"`
	Error *ErrorResponse `json:"error"`
}

type AuditoriaHandler struct {
	svc service.AuditoriaService
}

func NewAuditoriaHandler(svc service.AuditoriaService) *AuditoriaHandler {
	return &AuditoriaHandler{svc: svc}
}

func (h *AuditoriaHandler) requireAdmin(c *gin.Context) bool {
	rol := c.GetHeader("X-User-Rol")
	if rol == "" {
		c.JSON(http.StatusForbidden, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "FORBIDDEN",
				Message: "Acceso denegado: falta cabecera de rol de usuario",
			},
		})
		return false
	}
	if rol != "ADMINISTRADOR" {
		c.JSON(http.StatusForbidden, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "FORBIDDEN",
				Message: "Acceso denegado: rol insuficiente",
			},
		})
		return false
	}
	return true
}

func parseTime(s string, defaultVal time.Time) time.Time {
	if s == "" {
		return defaultVal
	}
	t, err := time.Parse(time.RFC3339, s)
	if err == nil {
		return t
	}
	t, err = time.Parse("2006-01-02", s)
	if err == nil {
		return t
	}
	return defaultVal
}

func (h *AuditoriaHandler) RegistrarEvento(c *gin.Context) {
	var req domain.EventoAuditoria
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "BAD_REQUEST",
				Message: "Datos del evento de auditoría inválidos: " + err.Error(),
			},
		})
		return
	}

	// Extract origin IP
	ip := c.GetHeader("X-Forwarded-For")
	if ip == "" {
		ip = c.ClientIP()
	}
	req.IPOrigen = ip

	persisted, err := h.svc.RegistrarEvento(c.Request.Context(), &req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "INTERNAL_SERVER_ERROR",
				Message: "Error al guardar registro de auditoría: " + err.Error(),
			},
		})
		return
	}

	if !persisted {
		// 202 Accepted: event accepted but not persisted in database (due to timeout/exhaustion)
		c.JSON(http.StatusAccepted, APIResponse{
			Data:  "Evento aceptado para registro local",
			Error: nil,
		})
		return
	}

	c.JSON(http.StatusCreated, APIResponse{
		Data:  "Evento registrado exitosamente",
		Error: nil,
	})
}

func (h *AuditoriaHandler) ObtenerEventos(c *gin.Context) {
	if !h.requireAdmin(c) {
		return
	}

	entidad := c.Query("entidad")
	accion := c.Query("accion")
	usuarioID := c.Query("usuario_id")
	desdeStr := c.Query("desde")
	hastaStr := c.Query("hasta")

	page, _ := strconv.Atoi(c.DefaultQuery("page", "0"))
	size, _ := strconv.Atoi(c.DefaultQuery("size", "50"))

	// Default desde: last 24 hours
	desde := parseTime(desdeStr, time.Now().Add(-24*time.Hour))
	hasta := parseTime(hastaStr, time.Time{})

	filters := map[string]interface{}{
		"entidad":    entidad,
		"accion":     accion,
		"usuario_id": usuarioID,
		"desde":      desde,
		"hasta":      hasta,
	}

	events, total, err := h.svc.ObtenerEventos(c.Request.Context(), filters, page, size)
	if err != nil {
		c.JSON(http.StatusInternalServerError, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "INTERNAL_SERVER_ERROR",
				Message: err.Error(),
			},
		})
		return
	}

	c.JSON(http.StatusOK, APIResponse{
		Data: gin.H{
			"eventos": events,
			"total":   total,
			"pagina":  page,
			"tamano":  size,
		},
		Error: nil,
	})
}

func (h *AuditoriaHandler) ObtenerResumen(c *gin.Context) {
	if !h.requireAdmin(c) {
		return
	}

	totalHoy, porAccion, porEntidad, activos, err := h.svc.ObtenerResumen(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "INTERNAL_SERVER_ERROR",
				Message: err.Error(),
			},
		})
		return
	}

	c.JSON(http.StatusOK, APIResponse{
		Data: gin.H{
			"total_hoy":            totalHoy,
			"por_accion":           porAccion,
			"por_entidad":          porEntidad,
			"usuarios_activos_hoy": activos,
		},
		Error: nil,
	})
}

func (h *AuditoriaHandler) Exportar(c *gin.Context) {
	if !h.requireAdmin(c) {
		return
	}

	entidad := c.Query("entidad")
	accion := c.Query("accion")
	usuarioID := c.Query("usuario_id")
	desdeStr := c.Query("desde")
	hastaStr := c.Query("hasta")
	formato := c.DefaultQuery("formato", "json")

	desde := parseTime(desdeStr, time.Time{})
	hasta := parseTime(hastaStr, time.Time{})

	filters := map[string]interface{}{
		"entidad":    entidad,
		"accion":     accion,
		"usuario_id": usuarioID,
		"desde":      desde,
		"hasta":      hasta,
	}

	rows, err := h.svc.Exportar(c.Request.Context(), filters)
	if err != nil {
		c.JSON(http.StatusInternalServerError, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "INTERNAL_SERVER_ERROR",
				Message: err.Error(),
			},
		})
		return
	}
	defer rows.Close()

	desdeFilename := "inicio"
	if desdeStr != "" {
		desdeFilename = desdeStr
	}
	hastaFilename := "fin"
	if hastaStr != "" {
		hastaFilename = hastaStr
	}

	if formato == "csv" {
		c.Header("Content-Type", "text/csv")
		c.Header("Content-Disposition", fmt.Sprintf(`attachment; filename="auditoria_%s_%s.csv"`, desdeFilename, hastaFilename))
		c.Writer.WriteHeader(http.StatusOK)

		writer := csv.NewWriter(c.Writer)
		// Write headers
		_ = writer.Write([]string{"timestamp", "entidad", "entidad_id", "accion", "usuario_id", "usuario_rol", "detalle", "ip_origen"})
		writer.Flush()

		for rows.Next() {
			var t time.Time
			var ent, act string
			var entIDNull, usrIDNull, rolNull, detNull, ipNull *string
			err := rows.Scan(&t, &ent, &entIDNull, &act, &usrIDNull, &rolNull, &detNull, &ipNull)
			if err == nil {
				entID := ""
				if entIDNull != nil {
					entID = *entIDNull
				}
				usrID := ""
				if usrIDNull != nil {
					usrID = *usrIDNull
				}
				rol := ""
				if rolNull != nil {
					rol = *rolNull
				}
				det := ""
				if detNull != nil {
					det = *detNull
				}
				ip := ""
				if ipNull != nil {
					ip = *ipNull
				}

				_ = writer.Write([]string{
					t.Format(time.RFC3339),
					ent,
					entID,
					act,
					usrID,
					rol,
					det,
					ip,
				})
				writer.Flush()
			}
		}
		return
	}

	// JSON format export (without pagination, up to 10k rows)
	events := []*domain.EventoAuditoria{}
	for rows.Next() {
		var t time.Time
		var ent, act string
		var entIDNull, usrIDNull, rolNull, detNull, ipNull *string
		err := rows.Scan(&t, &ent, &entIDNull, &act, &usrIDNull, &rolNull, &detNull, &ipNull)
		if err == nil {
			e := &domain.EventoAuditoria{
				Timestamp: t,
				Entidad:   ent,
				Accion:    act,
			}
			if entIDNull != nil {
				e.EntidadID = *entIDNull
			}
			if usrIDNull != nil {
				e.UsuarioID = *usrIDNull
			}
			if rolNull != nil {
				e.UsuarioRol = *rolNull
			}
			if detNull != nil {
				e.Detalle = *detNull
			}
			if ipNull != nil {
				e.IPOrigen = *ipNull
			}
			events = append(events, e)
		}
	}

	c.Header("Content-Disposition", fmt.Sprintf(`attachment; filename="auditoria_%s_%s.json"`, desdeFilename, hastaFilename))
	c.JSON(http.StatusOK, APIResponse{
		Data:  events,
		Error: nil,
	})
}

func (h *AuditoriaHandler) Health(c *gin.Context) {
	ctx, cancel := context.WithTimeout(c.Request.Context(), 1*time.Second)
	defer cancel()

	postgresStatus := "ok"
	records, err := h.svc.ObtenerEventos(ctx, map[string]interface{}{}, 0, 1)
	if err != nil && (len(records) == 0 && err.Error() != "") {
		postgresStatus = "error"
	}

	countToday, err := h.svc.ObtenerResumen(ctx)
	if err != nil {
		countToday = 0
	}

	c.JSON(http.StatusOK, gin.H{
		"status":        "ok",
		"postgres":      postgresStatus,
		"records_today": countToday,
	})
}
