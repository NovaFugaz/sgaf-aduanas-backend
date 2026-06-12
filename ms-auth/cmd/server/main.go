package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/sgaf/ms-auth/internal/config"
	"github.com/sgaf/ms-auth/internal/db"
	"github.com/sgaf/ms-auth/internal/handler"
	"github.com/sgaf/ms-auth/internal/middleware"
	"github.com/sgaf/ms-auth/internal/repository"
	"github.com/sgaf/ms-auth/internal/service"
	"go.uber.org/zap"
)

func main() {
	cfg := config.Load()
	logger := config.InitLogger()
	defer logger.Sync()

	// Initialize database connections
	pgPool, err := db.NewPostgresPool(cfg, logger)
	if err != nil {
		logger.Fatal("failed to initialize postgres", zap.Error(err))
	}
	defer pgPool.Close()

	redisClient, err := db.NewRedisClient(cfg, logger)
	if err != nil {
		logger.Fatal("failed to initialize redis", zap.Error(err))
	}
	defer redisClient.Close()

	// Initialize repositories and services
	userRepo := repository.NewUserRepository(pgPool)
	authService := service.NewAuthService(userRepo, redisClient, cfg, logger)

	// Setup Gin engine
	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	r.Use(middleware.LoggingMiddleware(logger))
	r.Use(gin.Recovery())

	// Public routes
	r.POST("/api/auth/login", handler.NewAuthHandler(authService, logger).Login)
	r.POST("/api/auth/refresh", handler.NewAuthHandler(authService, logger).Refresh)
	r.GET("/health", handler.HealthHandler(pgPool, redisClient))

	// Protected routes
	r.POST("/api/auth/logout", middleware.RequireAuth(cfg, redisClient, logger), handler.NewAuthHandler(authService, logger).Logout)
	r.GET("/api/auth/validate", middleware.RequireAuth(cfg, redisClient, logger), handler.NewAuthHandler(authService, logger).Validate)
	r.GET("/api/auth/me", middleware.RequireAuth(cfg, redisClient, logger), handler.NewAuthHandler(authService, logger).Me)

	// Start server
	srv := &http.Server{
		Addr:              ":" + cfg.Port,
		Handler:           r,
		ReadHeaderTimeout: 5 * time.Second,
	}

	go func() {
		logger.Info("server starting", zap.String("addr", srv.Addr))
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("server error", zap.Error(err))
		}
	}()

	// Graceful shutdown
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
	<-sigChan

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		logger.Error("server shutdown error", zap.Error(err))
	}
	logger.Info("server stopped")
}
