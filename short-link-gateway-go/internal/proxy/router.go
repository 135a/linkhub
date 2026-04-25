package proxy

import (
	"net/http"
	"net/http/httputil"
	"net/url"
	"shortlink-gateway-go/internal/config"
	logclient "shortlink-gateway-go/internal/log"
	"shortlink-gateway-go/internal/nacos"
	"sort"
	"strings"
	"sync"

	"go.uber.org/zap"
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
		if logclient.Logger != nil {
			logclient.Logger.Error("failed to parse target", zap.String("target", target), zap.Error(err))
		}
		return
	}

	proxy := httputil.NewSingleHostReverseProxy(targetURL)
	proxy.ErrorHandler = func(w http.ResponseWriter, req *http.Request, err error) {
		if logclient.Logger != nil {
			logclient.Logger.Error("error forwarding", zap.String("target", target), zap.Error(err))
		}
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
			if logclient.Logger != nil {
				logclient.Logger.Error("failed to parse default target", zap.String("target", target), zap.Error(err))
			}
			continue
		}

		proxy := httputil.NewSingleHostReverseProxy(targetURL)
		proxy.ErrorHandler = func(w http.ResponseWriter, req *http.Request, err error) {
			if logclient.Logger != nil {
				logclient.Logger.Error("error forwarding", zap.Error(err))
			}
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

			if logclient.Logger != nil {
				logclient.Logger.Debug("updating route to Nacos address", zap.String("prefix", route.Prefix), zap.String("nacosURL", nacosURL))
			}
			route.UpdateProxy(nacosURL)
		} else {
			if logclient.Logger != nil {
				logclient.Logger.Debug("Nacos has no instances, using default target", zap.String("service", route.ServiceName), zap.String("defaultTarget", route.DefaultTarget))
			}
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
		if logclient.Logger != nil {
			logclient.Logger.Debug("route not found", zap.String("path", req.URL.Path))
		}
		http.NotFound(w, req)
		return
	}
	if logclient.Logger != nil {
		logclient.Logger.Debug("route matched", zap.String("path", req.URL.Path))
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
