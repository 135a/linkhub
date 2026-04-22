package config

import "os"

type Config struct {
	Port           string
	ClickHouseHost string
	ClickHousePort string
	MySQLHost      string
	MySQLPort      string
	MySQLUser      string
	MySQLPassword  string
	MySQLDatabase  string
	JWTSecret      string
}

func Load() *Config {
	return &Config{
		Port:           getEnv("PORT", "8081"),
		ClickHouseHost: getEnv("CLICKHOUSE_HOST", "localhost"),
		ClickHousePort: getEnv("CLICKHOUSE_PORT", "8123"),
		MySQLHost:      getEnv("MYSQL_HOST", "localhost"),
		MySQLPort:      getEnv("MYSQL_PORT", "3306"),
		MySQLUser:      getEnv("MYSQL_USER", "root"),
		MySQLPassword:  getEnv("MYSQL_PASSWORD", "root"),
		MySQLDatabase:  getEnv("MYSQL_DATABASE", "log_system"),
		JWTSecret:      getEnv("JWT_SECRET", "shortlink-log-secret-key"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
