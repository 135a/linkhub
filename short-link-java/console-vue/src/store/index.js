import { createStore } from 'vuex'

// 创建一个新的 store 实例
const store = createStore({
  state() {
    return {
      // 创建短链表单默认域名占位；实际域名以服务端/部署配置为准，不在此写第三方营销域
      domain: ''
    }
  }
})

export default store
