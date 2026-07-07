package db

import (
	"context"
	_ "embed"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

//go:embed migrations/001_create_audit_tables.sql
var migrationSQL string

func InitDB(ctx context.Context, dsn string) (*pgxpool.Pool, error) {
	config, err := pgxpool.ParseConfig(dsn)
	if err != nil {
		return nil, fmt.Errorf("error parsing database DSN: %w", err)
	}

	// Concurrency constraint: pgx connection pool with MaxConns=10
	config.MaxConns = 10
	config.MinConns = 2
	config.MaxConnIdleTime = 5 * time.Minute

	pool, err := pgxpool.NewWithConfig(ctx, config)
	if err != nil {
		return nil, fmt.Errorf("error creating pgx pool: %w", err)
	}

	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, fmt.Errorf("error pinging database: %w", err)
	}

	// Run embed migration automatically on startup
	_, err = pool.Exec(ctx, migrationSQL)
	if err != nil {
		pool.Close()
		return nil, fmt.Errorf("error running database migration: %w", err)
	}

	return pool, nil
}
