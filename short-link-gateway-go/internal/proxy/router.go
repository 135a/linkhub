package proxy

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"shortlink-gateway-go/internal/config"
	"shortlink-gateway-go/internal/nacos"
	"sort"
	"strings"
	"sync"
)

type RouteEntry struct {
	Prefix        string
	ServiceName   string
	DefaultTarget string
	Proxy         *httputil.ReverseProxy
	mu            sync.RWMutex
}

func (re *RouteEntry) UpdateProxy(target string) {
	targetURL, err := url.Parse(target)
	if err != nil {
		log.Printf("[proxy] failed to parse target %s: %v", target, err)
		return
	}

	proxy := httputil.NewSingleHostReverseProxy(targetURL)
	proxy.ErrorHandler = func(w http.ResponseWriter, req *http.Request, err error) {
		log.Printf("[proxy] error forwarding to %s: %v", target, err)
		http.Error(w, "Bad Gateway", http.StatusBadGateway)
	}

	re.mu.Lock()
	re.Proxy = proxy
	re.mu.Unlock()
}

func (re *RouteEntry) GetProxy() *httputil.ReverseProxy {
	re.mu.RLock()
	defer re.mu.RUnlock()
	return re.Proxy
}

type Router struct {
	routes      []RouteEntry
	nacosClient *nacos.NacosClient
}

func NewRouter(routes []config.RouteConfig, nacosClient *nacos.NacosClient) *Router {
	entries := make([]RouteEntry, 0, len(routes))
	for _, r := range routes {
		target := r.DefaultTarget
		if r.Target != "" {
			target = r.Target
		}

		targetURL, err := url.Parse(target)
		if err != nil {
			log.Printf("[proxy] failed to parse default target %s: %v", target, err)
			continue
		}

		proxy := httputil.NewSingleHostReverseProxy(targetURL)
		proxy.ErrorHandler = func(w http.ResponseWriter, req *http.Request, err error) {
			log.Printf("[proxy] error forwarding: %v", err)
			http.Error(w, "Bad Gateway", http.StatusBadGateway)
		}

		entries = append(entries, RouteEntry{
			Prefix:        r.PathPrefix,
			ServiceName:   r.ServiceName,
			DefaultTarget: target,
			Proxy:         proxy,
		})
	}

	sort.Slice(entries, func(i, j int) bool {
		return len(entries[i].Prefix) > len(entries[j].Prefix)
	})

	router := &Router{
		routes:      entries,
		nacosClient: nacosClient,
	}

	if nacosClient != nil {
		router.refreshProxies()
	}

	return router
}

func (r *Router) refreshProxies() {
	if r.nacosClient == nil {
		return
	}

	for i := range r.routes {
		route := &r.routes[i]
		if route.ServiceName == "" {
			continue
		}

		if nacosURL, ok := r.nacosClient.GetServiceURL(route.ServiceName); ok {
			if !strings.HasPrefix(nacosURL, "http://") && !strings.HasPrefix(nacosURL, "https://") {
				nacosURL = "http://" + nacosURL
			}

			log.Printf("[proxy] updating route %s to Nacos address: %s", route.Prefix, nacosURL)
			route.UpdateProxy(nacosURL)
		} else {
			log.Printf("[proxy] Nacos has no instances for %s, using default: %s", route.ServiceName, route.DefaultTarget)
			route.UpdateProxy(route.DefaultTarget)
		}
	}
}

func (r *Router) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	if r.nacosClient != nil {
		r.refreshProxies()
	}

	proxy := r.matchRoute(req.URL.Path)
	if proxy == nil {
		http.NotFound(w, req)
		return
	}
	proxy.ServeHTTP(w, req)
}

func (r *Router) matchRoute(path string) *httputil.ReverseProxy {
	for i := range r.routes {
		if len(path) >= len(r.routes[i].Prefix) && path[:len(r.routes[i].Prefix)] == r.routes[i].Prefix {
			return r.routes[i].GetProxy()
		}
	}
	return nil
}
