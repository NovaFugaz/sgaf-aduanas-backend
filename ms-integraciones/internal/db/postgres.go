package db

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

func InitDB(ctx context.Context, dsn string) (*pgxpool.Pool, error) {
	config, err := pgxpool.ParseConfig(dsn)
	if err != nil {
		return nil, fmt.Errorf("error parsing database DSN: %w", err)
	}

	config.MaxConns = 25
	config.MinConns = 5
	config.MaxConnIdleTime = 10 * time.Minute

	pool, err := pgxpool.NewWithConfig(ctx, config)
	if err != nil {
		return nil, fmt.Errorf("error creating pgx pool: %w", err)
	}

	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, fmt.Errorf("error pinging database: %w", err)
	}

	createTableQuery := `
	CREATE TABLE IF NOT EXISTS integration_logs (
		id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
		sistema VARCHAR(50) NOT NULL,
		operacion VARCHAR(100) NOT NULL,
		request_data JSONB,
		response_data JSONB,
		estado VARCHAR(20) NOT NULL,
		latencia_ms INTEGER,
		tramite_id UUID,
		created_at TIMESTAMPTZ DEFAULT NOW()
	);`

	_, err = pool.Exec(ctx, createTableQuery)
	if err != nil {
		pool.Close()
		return nil, fmt.Errorf("error ensuring integration_logs table exists: %w", err)
	}

	return pool, nil
}
