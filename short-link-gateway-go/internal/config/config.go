package config

import (
	"fmt"
	"os"
	"sync"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server       ServerConfig       `yaml:"server"`
	Nacos        NacosConfig        `yaml:"nacos"`
	Routes       []RouteConfig      `yaml:"routes"`
	RateLimit    RateLimitGlobal    `yaml:"ratelimit"`
	Redis        RedisConfig        `yaml:"redis"`
	CORS         CORSConfig         `yaml:"cors"`
	LogCollector LogCollectorConfig `yaml:"log_collector"`
}

type ServerConfig struct {
	Port string `yaml:"port"`
}

type NacosConfig struct {
	Enabled         bool   `yaml:"enabled"`
	Host            string `yaml:"host"`
	Port            uint64 `yaml:"port"`
	Namespace       string `yaml:"namespace"`
	TimeoutMs       uint64 `yaml:"timeout_ms"`
	RefreshInterval int    `yaml:"refresh_interval"`
}

type RouteConfig struct {
	PathPrefix    string          `yaml:"path_prefix"`
	ServiceName   string          `yaml:"service_name"`
	Target        string          `yaml:"target"`
	DefaultTarget string          `yaml:"default_target"`
	RateLimit     RateLimitConfig `yaml:"rate_limit"`
}

type RateLimitConfig struct {
	Enabled bool `yaml:"enabled"`
	QPS     int  `yaml:"qps"`
}

type RateLimitGlobal struct {
	Algorithm string `yaml:"algorithm"`
}

type RedisConfig struct {
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	Password string `yaml:"password"`
	DB       int    `yaml:"db"`
}

type CORSConfig struct {
	AllowedOrigins   []string `yaml:"allowed_origins"`
	AllowedMethods   []string `yaml:"allowed_methods"`
	AllowedHeaders   []string `yaml:"allowed_headers"`
	ExposeHeaders    []string `yaml:"expose_headers"`
	AllowCredentials bool     `yaml:"allow_credentials"`
	MaxAge           int      `yaml:"max_age"`
}

type LogCollectorConfig struct {
	Endpoint string `yaml:"endpoint"`
	Level    string `yaml:"level"`
}

var (
	cfg  *Config
	once sync.Once
)

func Load(path string) (*Config, error) {
	var loadErr error
	once.Do(func() {
		cfg = &Config{}
		data, err := os.ReadFile(path)
		if err != nil {
			loadErr = fmt.Errorf("failed to read config file: %w", err)
			return
		}
		if err := yaml.Unmarshal(data, cfg); err != nil {
			loadErr = fmt.Errorf("failed to parse config file: %w", err)
			return
		}
		if cfg.Server.Port == "" {
			cfg.Server.Port = "8080"
		}
		if cfg.Nacos.Port == 0 {
			cfg.Nacos.Port = 8848
		}
		if cfg.Nacos.TimeoutMs == 0 {
			cfg.Nacos.TimeoutMs = 5000
		}
		if cfg.Nacos.RefreshInterval == 0 {
			cfg.Nacos.RefreshInterval = 30
		}
		if cfg.RateLimit.Algorithm == "" {
			cfg.RateLimit.Algorithm = "token_bucket"
		}
		if envAlgorithm := os.Getenv("RATELIMIT_ALGORITHM"); envAlgorithm != "" {
			cfg.RateLimit.Algorithm = envAlgorithm
		}
	})
	return cfg, loadErr
}

func Get() *Config {
	return cfg
}

func GetRedisAddr() string {
	return fmt.Sprintf("%s:%d", cfg.Redis.Host, cfg.Redis.Port)
}

func GetLogCollectorEndpoint() string {
	return cfg.LogCollector.Endpoint
}
