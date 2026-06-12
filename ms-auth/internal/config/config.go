package config

import (
	"log"
	"os"

	"go.uber.org/zap"
)

type Config struct {
	Port            string
	PostgresDSN     string
	RedisURL        string
	JWTSecret       string
	AccessTokenTTL  int // seconds
	RefreshTokenTTL int // seconds
	Environment     string
}

func Load() *Config {
	cfg := &Config{
		Port:            getEnv("PORT", "8080"),
		PostgresDSN:     getEnv("POSTGRES_DSN", "postgres://sgaf:changeme@postgres:5432/sgaf_main"),
		RedisURL:        getEnv("REDIS_URL", "redis://redis:6379"),
		JWTSecret:       os.Getenv("JWT_SECRET"),
		AccessTokenTTL:  900,    // 15 minutes
		RefreshTokenTTL: 604800, // 7 days
		Environment:     getEnv("ENVIRONMENT", "development"),
	}

	if cfg.JWTSecret == "" || len(cfg.JWTSecret) < 32 {
		log.Fatal("JWT_SECRET must be set and at least 32 characters long")
	}

	return cfg
}

func getEnv(key, defaultVal string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultVal
}

func InitLogger() *zap.Logger {
	var logger *zap.Logger
	var err error

	if os.Getenv("ENVIRONMENT") == "production" {
		logger, err = zap.NewProduction()
	} else {
		logger, err = zap.NewDevelopment()
	}

	if err != nil {
		log.Fatal("failed to create logger", err)
	}

	return logger
}
