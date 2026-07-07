package domain

import (
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
)

// Role constants
const (
	RoleADMINISTRADOR = "ADMINISTRADOR"
	RoleFUNCIONARIO   = "FUNCIONARIO"
	RolePASAJERO      = "PASAJERO"
)

// User represents a system user
type User struct {
	ID           uuid.UUID
	RUN          string
	Nombre       string
	Correo       string
	PasswordHash string
	Rol          string
	Aduana       *string // nullable for PASAJERO
	Activo       bool
	CreatedAt    time.Time
	UpdatedAt    time.Time
}

// Claims represents JWT token claims
type Claims struct {
	JTI    string `json:"jti"`
	Sub    string `json:"sub"`
	RUN    string `json:"run"`
	Nombre string `json:"nombre"`
	Rol    string `json:"rol"`
	Aduana string `json:"aduana,omitempty"`
	Iat    int64  `json:"iat"`
	Exp    int64  `json:"exp"`
}

// UserResponse is the user info sent to clients (no password)
type UserResponse struct {
	ID     string `json:"id"`
	RUN    string `json:"run"`
	Nombre string `json:"nombre"`
	Correo string `json:"correo"`
	Rol    string `json:"rol"`
	Aduana string `json:"aduana,omitempty"`
	Activo bool   `json:"activo"`
}

func (u *User) ToResponse() *UserResponse {
	if u == nil {
		return nil
	}

	resp := &UserResponse{
		ID:     u.ID.String(),
		RUN:    u.RUN,
		Nombre: u.Nombre,
		Correo: u.Correo,
		Rol:    u.Rol,
		Activo: u.Activo,
	}
	if u.Aduana != nil {
		resp.Aduana = *u.Aduana
	}
	return resp
}

// JWTClaims represents JWT token claims
type JWTClaims struct {
	jwt.RegisteredClaims
	RUN    string `json:"run"`
	Nombre string `json:"nombre"`
	Rol    string `json:"rol"`
	Aduana string `json:"aduana,omitempty"`
}

// LoginRequest is the request body for login
type LoginRequest struct {
	RUN      string `json:"run" binding:"required"`
	Password string `json:"password" binding:"required"`
}

// LoginResponse is the response for successful login
type LoginResponse struct {
	AccessToken  string       `json:"access_token"`
	RefreshToken string       `json:"refresh_token"`
	ExpiresIn    int          `json:"expires_in"`
	User         UserResponse `json:"user"`
}

// RefreshRequest is the request body for token refresh
type RefreshRequest struct {
	RefreshToken string `json:"refresh_token" binding:"required"`
}

// RefreshResponse is the response for token refresh
type RefreshResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    int    `json:"expires_in"`
}

// HealthResponse is the response for health check
type HealthResponse struct {
	Status   string `json:"status"`
	Postgres string `json:"postgres"`
	Redis    string `json:"redis"`
}

// ErrorResponse is the standard error response
type ErrorResponse struct {
	Code    string `json:"code"`
	Message string `json:"message"`
	Field   string `json:"field,omitempty"`
}

// APIResponse is the standard response envelope
type APIResponse struct {
	Data  interface{}    `json:"data"`
	Error *ErrorResponse `json:"error"`
}
