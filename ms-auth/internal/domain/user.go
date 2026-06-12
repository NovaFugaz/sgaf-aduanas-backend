package domain

import (
	"time"

	"github.com/golang-jwt/jwt/v5"
)

const (
	RoleAdministrador = "ADMINISTRADOR"
	RoleFuncionario   = "FUNCIONARIO"
	RolePasajero      = "PASAJERO"
)

type User struct {
	ID           string    `db:"id"`
	RUN          string    `db:"run"`
	Nombre       string    `db:"nombre"`
	Correo       string    `db:"correo"`
	PasswordHash string    `db:"password_hash"`
	Rol          string    `db:"rol"`
	Aduana       *string   `db:"aduana"`
	Activo       bool      `db:"activo"`
	CreatedAt    time.Time `db:"created_at"`
	UpdatedAt    time.Time `db:"updated_at"`
}

type UserResponse struct {
	ID     string  `json:"id"`
	Nombre string  `json:"nombre"`
	Rol    string  `json:"rol"`
	Aduana *string `json:"aduana"`
}

func (u *User) ToResponse() *UserResponse {
	return &UserResponse{
		ID:     u.ID,
		Nombre: u.Nombre,
		Rol:    u.Rol,
		Aduana: u.Aduana,
	}
}

type JWTClaims struct {
	JTI    string  `json:"jti"`
	Sub    string  `json:"sub"`
	RUN    string  `json:"run"`
	Nombre string  `json:"nombre"`
	Rol    string  `json:"rol"`
	Aduana *string `json:"aduana,omitempty"`
	IAT    int64   `json:"iat"`
	EXP    int64   `json:"exp"`
}

// Implement jwt.Claims interface
func (c *JWTClaims) GetExpirationTime() (*jwt.NumericDate, error) {
	return jwt.NewNumericDate(time.Unix(c.EXP, 0)), nil
}

func (c *JWTClaims) GetIssuedAt() (*jwt.NumericDate, error) {
	return jwt.NewNumericDate(time.Unix(c.IAT, 0)), nil
}

func (c *JWTClaims) GetNotBefore() (*jwt.NumericDate, error) {
	return nil, nil
}

func (c *JWTClaims) GetIssuer() (string, error) {
	return "sgaf-auth", nil
}

func (c *JWTClaims) GetSubject() (string, error) {
	return c.Sub, nil
}

func (c *JWTClaims) GetAudience() (jwt.ClaimStrings, error) {
	return jwt.ClaimStrings{"sgaf"}, nil
}
