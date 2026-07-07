package config

import (
	"fmt"
	"os"
	"strconv"

	"go.uber.org/zap"
)

type Config struct {
	Port            int
	PostgresDSN     string
	RedisURL        string
	JWTSecret       string
	Environment     string
	AccessTokenTTL  int
	RefreshTokenTTL int
	RateLimitPerMin int
}

func Load() *Config {
	port := parseEnvInt("PORT", 8080)
	postgresDSN := os.Getenv("POSTGRES_DSN")
	if postgresDSN == "" {
		postgresDSN = "postgres://sgaf:changeme@localhost:5432/sgaf_main"
	}

	redisURL := os.Getenv("REDIS_URL")
	if redisURL == "" {
		redisURL = "redis://localhost:6379"
	}

	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		panic("JWT_SECRET environment variable is required and must be at least 32 bytes")
	}
	if len(jwtSecret) < 32 {
		panic("JWT_SECRET must be at least 32 bytes long")
	}

	environment := os.Getenv("ENVIRONMENT")
	if environment == "" {
		environment = "development"
	}

	return &Config{
		Port:            port,
		PostgresDSN:     postgresDSN,
		RedisURL:        redisURL,
		JWTSecret:       jwtSecret,
		Environment:     environment,
		AccessTokenTTL:  15 * 60, // 15 minutes
		RefreshTokenTTL: 7 * 24 * 60 * 60, // 7 days
		RateLimitPerMin: 10,
	}
}

func parseEnvInt(key string, defaultVal int) int {
	if val := os.Getenv(key); val != "" {
		if intVal, err := strconv.Atoi(val); err == nil {
			return intVal
		}
	}
	return defaultVal
}

func (c *Config) String() string {
	return fmt.Sprintf("Config{Port: %d, Env: %s, PostgresDSN: %s, RedisURL: %s}", c.Port, c.Environment, c.PostgresDSN, c.RedisURL)
}

func InitLogger() *zap.Logger {
	var logger *zap.Logger
	var err error

	environment := os.Getenv("ENVIRONMENT")
	if environment == "production" {
		logger, err = zap.NewProduction()
	} else {
		logger, err = zap.NewDevelopment()
	}

	if err != nil {
		panic(fmt.Sprintf("failed to create logger: %v", err))
	}

	return logger
}
