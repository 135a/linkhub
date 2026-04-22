import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export function register(data) {
  return api.post('/auth/register', data)
}

export function login(data) {
  return api.post('/auth/login', data)
}

export function queryLogs(params) {
  return api.get('/logs/query', { params })
}

export function healthCheck() {
  return api.get('/health')
}