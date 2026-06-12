package service

import (
	"context"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"
	"github.com/sgaf/ms-auth/internal/config"
	"github.com/sgaf/ms-auth/internal/domain"
	"github.com/sgaf/ms-auth/internal/repository"
	"go.uber.org/zap"
	"golang.org/x/crypto/bcrypt"
)

const (
	refreshTokenPrefix = "refresh:"
	blocklistPrefix    = "blocklist:"
	rateLimitPrefix    = "ratelimit:"
	maxLoginAttempts   = 10
	rateLimitWindow    = 60 // seconds
)

type AuthService struct {
	userRepo    *repository.UserRepository
	redisClient *redis.Client
	config      *config.Config
	logger      *zap.Logger
}

func NewAuthService(userRepo *repository.UserRepository, redisClient *redis.Client, config *config.Config, logger *zap.Logger) *AuthService {
	return &AuthService{
		userRepo:    userRepo,
		redisClient: redisClient,
		config:      config,
		logger:      logger,
	}
}

type LoginRequest struct {
	RUN      string `json:"run" binding:"required"`
	Password string `json:"password" binding:"required"`
}

type LoginResponse struct {
	AccessToken  string               `json:"access_token"`
	RefreshToken string               `json:"refresh_token"`
	ExpiresIn    int                  `json:"expires_in"`
	User         *domain.UserResponse `json:"user"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refresh_token" binding:"required"`
}

func (s *AuthService) CheckRateLimit(ctx context.Context, ip string) error {
	key := rateLimitPrefix + ip
	count, err := s.redisClient.Incr(ctx, key).Result()
	if err != nil {
		s.logger.Error("failed to check rate limit", zap.Error(err))
		return err
	}

	if count == 1 {
		s.redisClient.Expire(ctx, key, time.Duration(rateLimitWindow)*time.Second)
	}

	if count > maxLoginAttempts {
		s.logger.Warn("rate limit exceeded", zap.String("ip", ip), zap.Int64("count", count))
		return fmt.Errorf("rate limit exceeded")
	}

	return nil
}

func (s *AuthService) Login(ctx context.Context, req *LoginRequest, ip string) (*LoginResponse, error) {
	// Check rate limit
	if err := s.CheckRateLimit(ctx, ip); err != nil {
		s.logger.Info("login rate limited", zap.String("run_suffix", run_suffix(req.RUN)))
		return nil, fmt.Errorf("RATE_LIMITED")
	}

	// Find user by RUN
	user, err := s.userRepo.FindByRUN(ctx, req.RUN)
	if err != nil {
		s.logger.Info("login failed: user not found", zap.String("run_suffix", run_suffix(req.RUN)))
		return nil, fmt.Errorf("INVALID_CREDENTIALS")
	}

	// Check if user is active
	if !user.Activo {
		s.logger.Info("login failed: user inactive", zap.String("run_suffix", run_suffix(user.RUN)))
		return nil, fmt.Errorf("USER_INACTIVE")
	}

	// Verify password
	err = bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password))
	if err != nil {
		s.logger.Info("login failed: invalid password", zap.String("run_suffix", run_suffix(req.RUN)))
		return nil, fmt.Errorf("INVALID_CREDENTIALS")
	}

	// Generate tokens
	accessToken, _, err := s.generateAccessToken(user)
	if err != nil {
		return nil, err
	}

	refreshToken, refreshJTI, err := s.generateRefreshToken(user)
	if err != nil {
		return nil, err
	}

	// Store refresh token in Redis
	refreshKey := refreshTokenPrefix + refreshJTI
	err = s.redisClient.Set(ctx, refreshKey, user.ID, time.Duration(s.config.RefreshTokenTTL)*time.Second).Err()
	if err != nil {
		s.logger.Error("failed to store refresh token", zap.Error(err))
		return nil, err
	}

	s.logger.Info("user logged in successfully", zap.String("run_suffix", run_suffix(user.RUN)), zap.String("role", user.Rol))

	return &LoginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
		ExpiresIn:    s.config.AccessTokenTTL,
		User:         user.ToResponse(),
	}, nil
}

func (s *AuthService) Refresh(ctx context.Context, req *RefreshRequest) (*LoginResponse, error) {
	// Parse refresh token
	token, err := jwt.ParseWithClaims(req.RefreshToken, &domain.JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(s.config.JWTSecret), nil
	})

	if err != nil || !token.Valid {
		s.logger.Info("refresh failed: invalid token", zap.Error(err))
		return nil, fmt.Errorf("INVALID_TOKEN")
	}

	claims := token.Claims.(*domain.JWTClaims)

	// Check if refresh token exists in Redis
	refreshKey := refreshTokenPrefix + claims.JTI
	userID, err := s.redisClient.Get(ctx, refreshKey).Result()
	if err == redis.Nil {
		s.logger.Info("refresh failed: token revoked or expired", zap.String("jti", claims.JTI))
		return nil, fmt.Errorf("INVALID_TOKEN")
	} else if err != nil {
		s.logger.Error("failed to check refresh token", zap.Error(err))
		return nil, err
	}

	// Fetch user
	user, err := s.userRepo.FindByID(ctx, userID)
	if err != nil {
		s.logger.Info("refresh failed: user not found", zap.String("user_id", userID))
		return nil, fmt.Errorf("USER_NOT_FOUND")
	}

	if !user.Activo {
		s.logger.Info("refresh failed: user inactive", zap.String("run_suffix", run_suffix(user.RUN)))
		return nil, fmt.Errorf("USER_INACTIVE")
	}

	// Revoke old refresh token
	s.redisClient.Del(ctx, refreshKey)

	// Generate new tokens
	accessToken, _, err := s.generateAccessToken(user)
	if err != nil {
		return nil, err
	}

	refreshToken, newRefreshJTI, err := s.generateRefreshToken(user)
	if err != nil {
		return nil, err
	}

	// Store new refresh token in Redis
	newRefreshKey := refreshTokenPrefix + newRefreshJTI
	err = s.redisClient.Set(ctx, newRefreshKey, user.ID, time.Duration(s.config.RefreshTokenTTL)*time.Second).Err()
	if err != nil {
		s.logger.Error("failed to store new refresh token", zap.Error(err))
		return nil, err
	}

	s.logger.Info("token refreshed successfully", zap.String("run_suffix", run_suffix(user.RUN)))

	return &LoginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
		ExpiresIn:    s.config.AccessTokenTTL,
		User:         user.ToResponse(),
	}, nil
}

func (s *AuthService) Logout(ctx context.Context, jti string) error {
	// Get token remaining lifetime
	key := blocklistPrefix + jti
	remainingTTL := s.config.AccessTokenTTL // default to access token TTL

	// Add to blocklist
	err := s.redisClient.Set(ctx, key, "1", time.Duration(remainingTTL)*time.Second).Err()
	if err != nil {
		s.logger.Error("failed to add token to blocklist", zap.Error(err))
		return err
	}

	s.logger.Info("user logged out", zap.String("jti", jti))
	return nil
}

func (s *AuthService) ValidateToken(ctx context.Context, tokenString string) (*domain.JWTClaims, error) {
	// Parse token
	token, err := jwt.ParseWithClaims(tokenString, &domain.JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(s.config.JWTSecret), nil
	})

	if err != nil || !token.Valid {
		return nil, fmt.Errorf("INVALID_TOKEN")
	}

	claims := token.Claims.(*domain.JWTClaims)

	// Check blocklist (fast Redis check)
	blocklistKey := blocklistPrefix + claims.JTI
	blocked, err := s.redisClient.Exists(ctx, blocklistKey).Result()
	if err != nil {
		s.logger.Error("failed to check blocklist", zap.Error(err))
		return nil, err
	}

	if blocked > 0 {
		return nil, fmt.Errorf("TOKEN_REVOKED")
	}

	return claims, nil
}

func (s *AuthService) generateAccessToken(user *domain.User) (string, string, error) {
	jti := uuid.New().String()
	now := time.Now()
	exp := now.Add(time.Duration(s.config.AccessTokenTTL) * time.Second)

	claims := &domain.JWTClaims{
		JTI:    jti,
		Sub:    user.ID,
		RUN:    user.RUN,
		Nombre: user.Nombre,
		Rol:    user.Rol,
		Aduana: user.Aduana,
		IAT:    now.Unix(),
		EXP:    exp.Unix(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(s.config.JWTSecret))
	if err != nil {
		return "", "", fmt.Errorf("failed to sign access token: %w", err)
	}

	return tokenString, jti, nil
}

func (s *AuthService) generateRefreshToken(user *domain.User) (string, string, error) {
	jti := uuid.New().String()
	now := time.Now()
	exp := now.Add(time.Duration(s.config.RefreshTokenTTL) * time.Second)

	claims := &domain.JWTClaims{
		JTI: jti,
		Sub: user.ID,
		IAT: now.Unix(),
		EXP: exp.Unix(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(s.config.JWTSecret))
	if err != nil {
		return "", "", fmt.Errorf("failed to sign refresh token: %w", err)
	}

	return tokenString, jti, nil
}

func run_suffix(run string) string {
	if len(run) >= 3 {
		return "..." + run[len(run)-3:]
	}
	return run
}

func (s *AuthService) GetUserByID(ctx context.Context, userID string) (*domain.UserResponse, error) {
	user, err := s.userRepo.FindByID(ctx, userID)
	if err != nil {
		return nil, err
	}
	return user.ToResponse(), nil
}
