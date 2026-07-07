package config

import (
	"os"
	"strconv"
)

type Config struct {
	Port        int
	PostgresDSN string
	Environment string
}

func Load() *Config {
	portStr := os.Getenv("PORT")
	port, err := strconv.Atoi(portStr)
	if err != nil || port <= 0 {
		port = 8083
	}

	postgresDSN := os.Getenv("POSTGRES_DSN")
	if postgresDSN == "" {
		postgresDSN = "postgres://sgaf:changeme@postgres:5432/sgaf_main"
	}

	env := os.Getenv("ENVIRONMENT")
	if env == "" {
		env = "development"
	}

	return &Config{
		Port:        port,
		PostgresDSN: postgresDSN,
		Environment: env,
	}
}
