package middleware

import (
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/redis/go-redis/v9"
	"github.com/sgaf/ms-auth/internal/config"
	"github.com/sgaf/ms-auth/internal/domain"
	"go.uber.org/zap"
)

func RequireAuth(cfg *config.Config, redisClient *redis.Client, logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "missing authorization header",
				},
			})
			c.Abort()
			return
		}

		// Extract Bearer token
		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || parts[0] != "Bearer" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "invalid authorization header format",
				},
			})
			c.Abort()
			return
		}

		tokenString := parts[1]

		// Parse and validate token
		token, err := jwt.ParseWithClaims(tokenString, &domain.JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, jwt.ErrSignatureInvalid
			}
			return []byte(cfg.JWTSecret), nil
		})

		if err != nil || !token.Valid {
			logger.Debug("token validation failed", zap.Error(err))
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "invalid or expired token",
				},
			})
			c.Abort()
			return
		}

		claims := token.Claims.(*domain.JWTClaims)

		// Check blocklist
		blocklistKey := "blocklist:" + claims.JTI
		blocked, err := redisClient.Exists(c.Request.Context(), blocklistKey).Result()
		if err != nil {
			logger.Error("failed to check blocklist", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "SERVER_ERROR",
					"message": "failed to validate token",
				},
			})
			c.Abort()
			return
		}

		if blocked > 0 {
			logger.Debug("token revoked", zap.String("jti", claims.JTI))
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "token has been revoked",
				},
			})
			c.Abort()
			return
		}

		// Set claims in context for use by handlers
		c.Set("claims", claims)
		c.Next()
	}
}

func RequireRole(cfg *config.Config, redisClient *redis.Client, logger *zap.Logger, roles ...string) gin.HandlerFunc {
	return func(c *gin.Context) {
		// First validate token
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "missing authorization header",
				},
			})
			c.Abort()
			return
		}

		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || parts[0] != "Bearer" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "invalid authorization header format",
				},
			})
			c.Abort()
			return
		}

		tokenString := parts[1]

		token, err := jwt.ParseWithClaims(tokenString, &domain.JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, jwt.ErrSignatureInvalid
			}
			return []byte(cfg.JWTSecret), nil
		})

		if err != nil || !token.Valid {
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "invalid or expired token",
				},
			})
			c.Abort()
			return
		}

		claims := token.Claims.(*domain.JWTClaims)

		// Check blocklist
		blocklistKey := "blocklist:" + claims.JTI
		blocked, err := redisClient.Exists(c.Request.Context(), blocklistKey).Result()
		if err != nil {
			logger.Error("failed to check blocklist", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "SERVER_ERROR",
					"message": "failed to validate token",
				},
			})
			c.Abort()
			return
		}

		if blocked > 0 {
			c.JSON(http.StatusUnauthorized, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "UNAUTHORIZED",
					"message": "token has been revoked",
				},
			})
			c.Abort()
			return
		}

		// Check role
		roleAllowed := false
		for _, role := range roles {
			if claims.Rol == role {
				roleAllowed = true
				break
			}
		}

		if !roleAllowed {
			logger.Warn("insufficient role", zap.String("user_role", claims.Rol), zap.Strings("required_roles", roles))
			c.JSON(http.StatusForbidden, gin.H{
				"data": nil,
				"error": gin.H{
					"code":    "FORBIDDEN",
					"message": "insufficient permissions",
				},
			})
			c.Abort()
			return
		}

		c.Set("claims", claims)
		c.Next()
	}
}

func LoggingMiddleware(logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		method := c.Request.Method
		path := c.Request.RequestURI
		startTime := time.Now()

		c.Next()

		status := c.Writer.Status()
		latency := time.Since(startTime).Seconds() * 1000 // convert to milliseconds

		userID := "anonymous"
		if claims, exists := c.Get("claims"); exists {
			if jwtClaims, ok := claims.(*domain.JWTClaims); ok {
				userID = jwtClaims.Sub
			}
		}

		logger.Info("http request",
			zap.String("method", method),
			zap.String("path", path),
			zap.Int("status", status),
			zap.Float64("latency_ms", latency),
			zap.String("user_id", userID),
		)
	}
}
