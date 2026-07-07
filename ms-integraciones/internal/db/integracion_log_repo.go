package db

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type IntegrationLog struct {
	ID           string
	Sistema      string // 'PDI', 'SAG', 'ADUANA_ARG'
	Operacion    string
	RequestData  []byte // JSON data
	ResponseData []byte // JSON data
	Estado       string // 'EXITO', 'ERROR', 'TIMEOUT', 'CIRCUIT_OPEN'
	LatenciaMs   int
	TramiteID    *string // Nullable UUID
	CreatedAt    time.Time
}

type IntegracionLogRepo interface {
	Insert(ctx context.Context, log *IntegrationLog) error
}

type PostgresIntegracionLogRepo struct {
	pool *pgxpool.Pool
}

func NewIntegracionLogRepo(pool *pgxpool.Pool) IntegracionLogRepo {
	return &PostgresIntegracionLogRepo{pool: pool}
}

func (r *PostgresIntegracionLogRepo) Insert(ctx context.Context, log *IntegrationLog) error {
	query := `
	INSERT INTO integration_logs (sistema, operacion, request_data, response_data, estado, latencia_ms, tramite_id)
	VALUES ($1, $2, $3, $4, $5, $6, $7)`

	_, err := r.pool.Exec(ctx, query,
		log.Sistema,
		log.Operacion,
		log.RequestData,
		log.ResponseData,
		log.Estado,
		log.LatenciaMs,
		log.TramiteID,
	)
	if err != nil {
		return fmt.Errorf("error inserting integration log: %w", err)
	}
	return nil
}
