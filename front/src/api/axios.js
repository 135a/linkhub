import axios from 'axios'
import {getToken, getUsername, removeKey, removeUsername} from '@/core/auth.js'
import {isNotEmpty} from '@/utils/plugins.js'
import router from "@/router";
import { ElMessage } from 'element-plus'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/short-link/v1'
// 创建实例
const http = axios.create({
    baseURL: baseURL,
    timeout: 15000
})
// 请求拦截 -->在请求发送之前做一些事情
http.interceptors.request.use(
    (config) => {
        config.headers.Token = isNotEmpty(getToken()) ? getToken() : ''
        config.headers.Username = isNotEmpty(getUsername()) ? getUsername() : ''
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)
// 响应拦截 -->在返回结果之前做一些事情
http.interceptors.response.use(
    (res) => {
        if (res.status == 0 || res.status == 200) {
            if (res.data && res.data.success === false) {
                ElMessage.error(res.data.message)
                return Promise.reject(res.data)
            }
            return Promise.resolve(res)
        }
        return Promise.reject(res)
    },
    (err) => {
        if (err.response && err.response.status === 401) {
            // 清除所有本地登录态，防止路由守卫误判
            localStorage.removeItem('token')
            localStorage.removeItem('username')
            removeKey()
            removeUsername()
            // 避免已经在登录页时重复跳转
            if (router.currentRoute.value.path !== '/login') {
                ElMessage.warning('登录已过期，请重新登录')
                router.push('/login')
            }
        }
        return Promise.reject(err)
    }
)
export default http
