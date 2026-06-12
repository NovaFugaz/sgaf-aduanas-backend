package handler

import (
	"context"
	"net"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
	"github.com/sgaf/ms-auth/internal/domain"
	"github.com/sgaf/ms-auth/internal/service"
	"go.uber.org/zap"
)

type Response struct {
	Data  interface{} `json:"data"`
	Error *ErrorInfo  `json:"error"`
}

type ErrorInfo struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

type AuthHandler struct {
	authService *service.AuthService
	logger      *zap.Logger
}

func NewAuthHandler(authService *service.AuthService, logger *zap.Logger) *AuthHandler {
	return &AuthHandler{
		authService: authService,
		logger:      logger,
	}
}

func (h *AuthHandler) Login(c *gin.Context) {
	var req service.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "INVALID_REQUEST",
				Message: "missing required fields",
			},
		})
		return
	}

	ip := getClientIP(c)
	resp, err := h.authService.Login(c.Request.Context(), &req, ip)
	if err != nil {
		errCode := err.Error()
		statusCode := http.StatusUnauthorized
		message := "authentication failed"

		if errCode == "USER_INACTIVE" {
			statusCode = http.StatusForbidden
			message = "user is inactive"
		} else if errCode == "RATE_LIMITED" {
			statusCode = http.StatusTooManyRequests
			message = "too many login attempts"
		}

		c.JSON(statusCode, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    errCode,
				Message: message,
			},
		})
		return
	}

	c.JSON(http.StatusOK, Response{
		Data:  resp,
		Error: nil,
	})
}

func (h *AuthHandler) Refresh(c *gin.Context) {
	var req service.RefreshRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "INVALID_REQUEST",
				Message: "missing required fields",
			},
		})
		return
	}

	resp, err := h.authService.Refresh(c.Request.Context(), &req)
	if err != nil {
		c.JSON(http.StatusUnauthorized, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "INVALID_TOKEN",
				Message: "token refresh failed",
			},
		})
		return
	}

	c.JSON(http.StatusOK, Response{
		Data:  resp,
		Error: nil,
	})
}

func (h *AuthHandler) Logout(c *gin.Context) {
	claims, exists := c.Get("claims")
	if !exists {
		c.JSON(http.StatusUnauthorized, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "UNAUTHORIZED",
				Message: "no claims in context",
			},
		})
		return
	}

	jwtClaims := claims.(*domain.JWTClaims)
	err := h.authService.Logout(c.Request.Context(), jwtClaims.JTI)
	if err != nil {
		c.JSON(http.StatusInternalServerError, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "LOGOUT_FAILED",
				Message: "failed to logout",
			},
		})
		return
	}

	c.Status(http.StatusNoContent)
}

func (h *AuthHandler) Validate(c *gin.Context) {
	claims, exists := c.Get("claims")
	if !exists {
		c.JSON(http.StatusUnauthorized, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "UNAUTHORIZED",
				Message: "no claims in context",
			},
		})
		return
	}

	c.JSON(http.StatusOK, Response{
		Data:  claims,
		Error: nil,
	})
}

func (h *AuthHandler) Me(c *gin.Context) {
	claims, exists := c.Get("claims")
	if !exists {
		c.JSON(http.StatusUnauthorized, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "UNAUTHORIZED",
				Message: "no claims in context",
			},
		})
		return
	}

	jwtClaims := claims.(*domain.JWTClaims)
	resp, err := h.authService.GetUserByID(c.Request.Context(), jwtClaims.Sub)
	if err != nil {
		c.JSON(http.StatusNotFound, Response{
			Data: nil,
			Error: &ErrorInfo{
				Code:    "USER_NOT_FOUND",
				Message: "user not found",
			},
		})
		return
	}

	c.JSON(http.StatusOK, Response{
		Data:  resp,
		Error: nil,
	})
}

func HealthHandler(pgPool *pgxpool.Pool, redisClient *redis.Client) gin.HandlerFunc {
	return func(c *gin.Context) {
		ctx := context.Background()
		health := map[string]string{
			"status": "ok",
		}

		// Check PostgreSQL
		err := pgPool.Ping(ctx)
		if err != nil {
			health["postgres"] = "error"
		} else {
			health["postgres"] = "ok"
		}

		// Check Redis
		err = redisClient.Ping(ctx).Err()
		if err != nil {
			health["redis"] = "error"
		} else {
			health["redis"] = "ok"
		}

		c.JSON(http.StatusOK, health)
	}
}

func getClientIP(c *gin.Context) string {
	ip := c.ClientIP()
	// Remove port if present
	if host, _, err := net.SplitHostPort(ip); err == nil {
		ip = host
	}
	return ip
}
