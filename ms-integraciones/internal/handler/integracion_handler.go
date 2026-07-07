package handler

import (
	"fmt"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/sony/gobreaker"
	"github.com/sgaf/ms-integraciones/internal/adapter/sag"
	"github.com/sgaf/ms-integraciones/internal/service"
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

type IntegracionHandler struct {
	svc service.IntegracionService
}

func NewIntegracionHandler(svc service.IntegracionService) *IntegracionHandler {
	return &IntegracionHandler{svc: svc}
}

func (h *IntegracionHandler) handleError(c *gin.Context, err error, systemName string) {
	if err == gobreaker.ErrOpenState {
		c.JSON(http.StatusServiceUnavailable, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "CIRCUIT_OPEN",
				Message: fmt.Sprintf("Sistema %s no disponible temporalmente. Intente en 15 segundos.", systemName),
			},
		})
		return
	}

	c.JSON(http.StatusInternalServerError, APIResponse{
		Data: nil,
		Error: &ErrorResponse{
			Code:    "INTEGRATION_ERROR",
			Message: err.Error(),
		},
	})
}

func (h *IntegracionHandler) ConsultarPDI(c *gin.Context) {
	var req struct {
		RUT string `json:"rut" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "BAD_REQUEST",
				Message: "El RUT es requerido",
			},
		})
		return
	}

	res, latency, err := h.svc.ConsultarPDI(c.Request.Context(), req.RUT)
	c.Header("X-Integration-Latency-Ms", strconv.Itoa(latency))

	if err != nil {
		h.handleError(c, err, "PDI")
		return
	}

	c.JSON(http.StatusOK, APIResponse{
		Data:  res,
		Error: nil,
	})
}

func (h *IntegracionHandler) ValidarSAG(c *gin.Context) {
	var req sag.SAGRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "BAD_REQUEST",
				Message: "Datos de declaración inválidos",
			},
		})
		return
	}

	res, latency, err := h.svc.ValidarSAG(c.Request.Context(), req)
	c.Header("X-Integration-Latency-Ms", strconv.Itoa(latency))

	if err != nil {
		h.handleError(c, err, "SAG")
		return
	}

	c.JSON(http.StatusOK, APIResponse{
		Data:  res,
		Error: nil,
	})
}

func (h *IntegracionHandler) ConsultarAduanaArg(c *gin.Context) {
	var req struct {
		Patente      string `json:"patente" binding:"required"`
		TipoConsulta string `json:"tipo_consulta"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, APIResponse{
			Data: nil,
			Error: &ErrorResponse{
				Code:    "BAD_REQUEST",
				Message: "La patente es requerida",
			},
		})
		return
	}

	res, latency, err := h.svc.ConsultarAduanaArg(c.Request.Context(), req.Patente, req.TipoConsulta)
	c.Header("X-Integration-Latency-Ms", strconv.Itoa(latency))

	if err != nil {
		h.handleError(c, err, "Aduana Argentina")
		return
	}

	c.JSON(http.StatusOK, APIResponse{
		Data:  res,
		Error: nil,
	})
}

func (h *IntegracionHandler) GetEstado(c *gin.Context) {
	c.JSON(http.StatusOK, h.svc.GetEstado())
}

func (h *IntegracionHandler) Health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status": "UP",
		"circuit_breakers": gin.H{
			"pdi":              h.svc.GetCBPDI().State().String(),
			"sag":              h.svc.GetCBSAG().State().String(),
			"aduana_argentina": h.svc.GetCBAduana().State().String(),
		},
	})
}
