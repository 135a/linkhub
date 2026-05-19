import { createStore } from 'vuex'

// 创建一个新的 store 实例
const store = createStore({
  state() {
    return {
      // domain: 'nurl.ink'
      domain: import.meta.env.VITE_DOMAIN || 'link.example.com'
    }
  }
})

export default store
