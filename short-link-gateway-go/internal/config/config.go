package config

import (
	"fmt"
	"os"
	"sync"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server   ServerConfig   `yaml:"server"`
	Routes   []RouteConfig  `yaml:"routes"`
	Redis    RedisConfig    `yaml:"redis"`
	CORS     CORSConfig     `yaml:"cors"`
	Log      LogConfig      `yaml:"log"`
}

type ServerConfig struct {
	Port string `yaml:"port"`
}

type RouteConfig struct {
	PathPrefix string         `yaml:"path_prefix"`
	Target     string         `yaml:"target"`
	RateLimit  RateLimitConfig `yaml:"rate_limit"`
}

type RateLimitConfig struct {
	Enabled bool `yaml:"enabled"`
	QPS     int  `yaml:"qps"`
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

type LogConfig struct {
	LogstashHost string `yaml:"logstash_host"`
	LogstashPort string `yaml:"logstash_port"`
	Level        string `yaml:"level"`
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
	})
	return cfg, loadErr
}

func Get() *Config {
	return cfg
}

func GetRedisAddr() string {
	return fmt.Sprintf("%s:%d", cfg.Redis.Host, cfg.Redis.Port)
}

func GetLogstashURL() string {
	return fmt.Sprintf("http://%s:%s", cfg.Log.LogstashHost, cfg.Log.LogstashPort)
}