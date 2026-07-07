package main

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"github.com/sgaf/ms-auditoria/internal/config"
	"github.com/sgaf/ms-auditoria/internal/db"
	"github.com/sgaf/ms-auditoria/internal/handler"
	"github.com/sgaf/ms-auditoria/internal/repository"
	"github.com/sgaf/ms-auditoria/internal/service"
)

func main() {
	cfg := config.Load()

	var logger *zap.Logger
	var err error
	if cfg.Environment == "production" {
		logger, err = zap.NewProduction()
		gin.SetMode(gin.ReleaseMode)
	} else {
		logger, err = zap.NewDevelopment()
		gin.SetMode(gin.DebugMode)
	}
	if err != nil {
		fmt.Printf("failed to initialize logger: %v\n", err)
		os.Exit(1)
	}
	defer logger.Sync()

	logger.Info("starting ms-auditoria",
		zap.Int("port", cfg.Port),
		zap.String("env", cfg.Environment),
	)

	// Database initialization
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	dbPool, err := db.InitDB(ctx, cfg.PostgresDSN)
	if err != nil {
		logger.Fatal("failed to initialize database connection pool", zap.Error(err))
	}
	defer dbPool.Close()
	logger.Info("database pool initialized successfully")

	repo := repository.NewEventoRepo(dbPool)
	svc := service.NewAuditoriaService(repo, logger)
	h := handler.NewAuditoriaHandler(svc)

	r := gin.New()
	r.Use(gin.Recovery())

	// Middleware to log HTTP requests
	r.Use(func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path
		query := c.Request.URL.RawQuery

		c.Next()

		latency := time.Since(start)
		status := c.Writer.Status()

		logger.Info("http request",
			zap.Int("status", status),
			zap.String("method", c.Request.Method),
			zap.String("path", path),
			zap.String("query", query),
			zap.String("ip", c.ClientIP()),
			zap.Duration("latency", latency),
		)
	})

	api := r.Group("/api/auditoria")
	{
		api.POST("/eventos", h.RegistrarEvento)
		api.GET("/eventos", h.ObtenerEventos)
		api.GET("/eventos/resumen", h.ObtenerResumen)
		api.GET("/eventos/exportar", h.Exportar)
	}
	r.GET("/health", h.Health)

	srv := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.Port),
		Handler: r,
	}

	go func() {
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("listen error", zap.Error(err))
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	logger.Info("shutting down server...")

	ctxShutdown, cancelShutdown := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancelShutdown()

	if err := srv.Shutdown(ctxShutdown); err != nil {
		logger.Fatal("server forced to shutdown", zap.Error(err))
	}

	logger.Info("server exited gracefully")
}
