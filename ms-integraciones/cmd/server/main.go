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
	"github.com/sgaf/ms-integraciones/internal/adapter/aduana_arg"
	"github.com/sgaf/ms-integraciones/internal/adapter/pdi"
	"github.com/sgaf/ms-integraciones/internal/adapter/sag"
	"github.com/sgaf/ms-integraciones/internal/config"
	"github.com/sgaf/ms-integraciones/internal/db"
	"github.com/sgaf/ms-integraciones/internal/handler"
	"github.com/sgaf/ms-integraciones/internal/service"
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

	logger.Info("starting ms-integraciones",
		zap.Int("port", cfg.Port),
		zap.String("env", cfg.Environment),
	)

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	dbPool, err := db.InitDB(ctx, cfg.PostgresDSN)
	if err != nil {
		logger.Fatal("failed to initialize database connection pool", zap.Error(err))
	}
	defer dbPool.Close()
	logger.Info("database pool initialized")

	logRepo := db.NewIntegracionLogRepo(dbPool)
	pdiMock := pdi.NewMockPDIAdapter()
	sagMock := sag.NewMockSAGAdapter()
	argMock := aduana_arg.NewMockAduanaArgAdapter()

	svc := service.NewIntegracionService(pdiMock, sagMock, argMock, logRepo)
	h := handler.NewIntegracionHandler(svc)

	r := gin.New()
	r.Use(gin.Recovery())

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

	api := r.Group("/api/integraciones")
	{
		api.POST("/pdi/consultar-rut", h.ConsultarPDI)
		api.POST("/sag/validar-declaracion", h.ValidarSAG)
		api.POST("/aduana-argentina/consultar-vehiculo", h.ConsultarAduanaArg)
		api.GET("/estado", h.GetEstado)
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
