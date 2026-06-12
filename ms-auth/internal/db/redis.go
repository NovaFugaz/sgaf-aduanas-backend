package db

import (
	"context"
	"fmt"

	"github.com/redis/go-redis/v9"
	"github.com/sgaf/ms-auth/internal/config"
	"go.uber.org/zap"
)

func NewRedisClient(cfg *config.Config, logger *zap.Logger) (*redis.Client, error) {
	opt, err := redis.ParseURL(cfg.RedisURL)
	if err != nil {
		return nil, fmt.Errorf("failed to parse redis URL: %w", err)
	}

	client := redis.NewClient(opt)

	// Test connection
	err = client.Ping(context.Background()).Err()
	if err != nil {
		return nil, fmt.Errorf("failed to ping redis: %w", err)
	}

	logger.Info("redis connected successfully")
	return client, nil
}
