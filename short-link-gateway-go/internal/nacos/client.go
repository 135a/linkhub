package nacos

import (
	"fmt"
	"log"
	"math/rand"
	"shortlink-gateway-go/internal/config"
	"sync"
	"time"

	"github.com/nacos-group/nacos-sdk-go/v2/clients"
	"github.com/nacos-group/nacos-sdk-go/v2/clients/naming_client"
	"github.com/nacos-group/nacos-sdk-go/v2/common/constant"
	"github.com/nacos-group/nacos-sdk-go/v2/vo"
)

type ServiceInstance struct {
	IP       string
	Port     uint64
	Healthy  bool
	Weight   float64
}

type NacosClient struct {
	namingClient naming_client.INamingClient
	cache        map[string][]ServiceInstance
	mu           sync.RWMutex
	stopCh       chan struct{}
}

func NewNacosClient(cfg *config.NacosConfig) *NacosClient {
	if !cfg.Enabled {
		log.Println("[Nacos] disabled, skipping initialization")
		return nil
	}

	sc := []constant.ServerConfig{
		*constant.NewServerConfig(cfg.Host, cfg.Port),
	}

	cc := constant.ClientConfig{
		NamespaceId:         cfg.Namespace,
		TimeoutMs:           cfg.TimeoutMs,
		NotLoadCacheAtStart: true,
		LogDir:              "/tmp/nacos/log",
		CacheDir:            "/tmp/nacos/cache",
		LogLevel:            "warn",
	}

	namingClient, err := clients.NewNamingClient(
		vo.NacosClientParam{
			ClientConfig:  &cc,
			ServerConfigs: sc,
		},
	)

	if err != nil {
		log.Printf("[Nacos] failed to create client: %v, will use default targets", err)
		return nil
	}

	log.Printf("[Nacos] connected to %s:%d", cfg.Host, cfg.Port)

	client := &NacosClient{
		namingClient: namingClient,
		cache:        make(map[string][]ServiceInstance),
		stopCh:       make(chan struct{}),
	}

	go client.startRefresh(cfg.RefreshInterval)

	return client
}

func (c *NacosClient) startRefresh(intervalSeconds int) {
	if intervalSeconds <= 0 {
		intervalSeconds = 30
	}

	ticker := time.NewTicker(time.Duration(intervalSeconds) * time.Second)
	defer ticker.Stop()

	c.refreshAll()

	for {
		select {
		case <-ticker.C:
			c.refreshAll()
		case <-c.stopCh:
			return
		}
	}
}

func (c *NacosClient) refreshAll() {
	cfg := config.Get()
	if cfg == nil {
		return
	}

	for _, route := range cfg.Routes {
		if route.ServiceName == "" {
			continue
		}

		instances, err := c.fetchInstances(route.ServiceName)
		if err != nil {
			log.Printf("[Nacos] failed to fetch instances for %s: %v", route.ServiceName, err)
			continue
		}

		c.mu.Lock()
		c.cache[route.ServiceName] = instances
		c.mu.Unlock()

		log.Printf("[Nacos] refreshed %d instances for %s", len(instances), route.ServiceName)
	}
}

func (c *NacosClient) fetchInstances(serviceName string) ([]ServiceInstance, error) {
	result, err := c.namingClient.SelectInstances(vo.SelectInstancesParam{
		ServiceName: serviceName,
		HealthyOnly: true,
	})

	if err != nil {
		return nil, err
	}

	instances := make([]ServiceInstance, 0, len(result))
	for _, inst := range result {
		instances = append(instances, ServiceInstance{
			IP:      inst.Ip,
			Port:    inst.Port,
			Healthy: inst.Healthy,
			Weight:  inst.Weight,
		})
	}

	return instances, nil
}

func (c *NacosClient) GetServiceAddress(serviceName string) (string, bool) {
	c.mu.RLock()
	defer c.mu.RUnlock()

	instances, exists := c.cache[serviceName]
	if !exists || len(instances) == 0 {
		return "", false
	}

	instance := instances[rand.Intn(len(instances))]
	address := instance.IP
	if instance.Port > 0 && instance.Port < 1024 {
		address = address
	}
	return address, true
}

func (c *NacosClient) GetServiceURL(serviceName string) (string, bool) {
	c.mu.RLock()
	defer c.mu.RUnlock()

	instances, exists := c.cache[serviceName]
	if !exists || len(instances) == 0 {
		return "", false
	}

	instance := instances[rand.Intn(len(instances))]
	address := instance.IP
	if instance.Port > 0 {
		address = fmt.Sprintf("%s:%d", address, instance.Port)
	}
	return address, true
}

func (c *NacosClient) Stop() {
	close(c.stopCh)
}
