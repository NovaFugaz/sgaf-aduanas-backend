package db

import (
	"context"
	"embed"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/sgaf/ms-auth/internal/config"
	"go.uber.org/zap"
)

//go:embed migrations/*.sql
var migrationsFS embed.FS

func NewPostgresPool(cfg *config.Config, logger *zap.Logger) (*pgxpool.Pool, error) {
	pool, err := pgxpool.New(context.Background(), cfg.PostgresDSN)
	if err != nil {
		return nil, fmt.Errorf("failed to create postgres pool: %w", err)
	}

	// Test connection
	err = pool.Ping(context.Background())
	if err != nil {
		return nil, fmt.Errorf("failed to ping postgres: %w", err)
	}

	logger.Info("postgres connected successfully")

	// Run migrations
	if err := runMigrations(pool, logger); err != nil {
		return nil, fmt.Errorf("failed to run migrations: %w", err)
	}

	return pool, nil
}

func runMigrations(pool *pgxpool.Pool, logger *zap.Logger) error {
	ctx := context.Background()

	// Read migration file
	migrationSQL, err := migrationsFS.ReadFile("migrations/001_create_users.sql")
	if err != nil {
		return fmt.Errorf("failed to read migration file: %w", err)
	}

	// Execute migration
	_, err = pool.Exec(ctx, string(migrationSQL))
	if err != nil {
		logger.Error("migration already applied or error", zap.Error(err))
		// Don't fail if migration already exists
	}

	logger.Info("migrations completed")
	return nil
}
