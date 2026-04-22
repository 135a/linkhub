package proxy

import (
	"bytes"
	"io/ioutil"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"shortlink-gateway-go/internal/config"
	"strings"
)

type Router struct {
	routes []config.RouteConfig
}

func NewRouter(routes []config.RouteConfig) *Router {
	return &Router{routes: routes}
}

func (r *Router) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	target := r.matchRoute(req.URL.Path)
	if target == nil {
		http.NotFound(w, req)
		return
	}

	proxy := httputil.NewSingleHostReverseProxy(target)
	proxy.Director = func(pr *http.Request) {
		pr.URL.Scheme = target.Scheme
		pr.URL.Host = target.Host
		pr.Host = target.Host
		pr.Header.Set("X-Forwarded-For", req.RemoteAddr)
	}

	proxy.ModifyResponse = func(resp *http.Response) error {
		return nil
	}

	proxy.ErrorHandler = func(w http.ResponseWriter, req *http.Request, err error) {
		log.Printf("proxy error: %v", err)
		http.Error(w, "Bad Gateway", http.StatusBadGateway)
	}

	proxy.ServeHTTP(w, req)
}

func (r *Router) matchRoute(path string) *url.URL {
	for _, route := range r.routes {
		if strings.HasPrefix(path, route.PathPrefix) {
			if u, err := url.Parse(route.Target); err == nil {
				return u
			}
		}
	}
	return nil
}

func CopyRequest(req *http.Request, target *url.URL) (*http.Request, error) {
	var body []byte
	if req.Body != nil {
		body, _ = ioutil.ReadAll(req.Body)
		req.Body = ioutil.NopCloser(bytes.NewBuffer(body))
	}

	newReq, err := http.NewRequest(req.Method, target.String(), bytes.NewBuffer(body))
	if err != nil {
		return nil, err
	}

	newReq.Header = make(http.Header)
	for k, v := range req.Header {
		newReq.Header[k] = v
	}

	newReq.Host = target.Host
	return newReq, nil
}