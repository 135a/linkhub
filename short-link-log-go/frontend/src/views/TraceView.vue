<template>
  <div class="trace-page">
    <el-header class="header">
      <span class="brand">Trace 追踪</span>
      <router-link to="/"><el-button>返回日志</el-button></router-link>
      <el-button type="danger" @click="handleLogout">退出</el-button>
    </el-header>
    <el-card class="search-card">
      <el-input v-model="traceId" placeholder="输入 TraceID 查看完整链路日志" @keyup.enter="fetchTrace" style="max-width: 600px">
        <template #append>
          <el-button type="primary" @click="fetchTrace">查询</el-button>
        </template>
      </el-input>
    </el-card>
    <el-table :data="logs" stripe border v-loading="loading" style="margin-top: 12px">
      <el-table-column prop="timestamp" label="时间" width="200">
        <template #default="{ row }">{{ formatTime(row.timestamp) }}</template>
      </el-table-column>
      <el-table-column prop="level" label="级别" width="80">
        <template #default="{ row }">
          <el-tag :type="levelType(row.level)" size="small">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="service" label="服务" width="140" />
      <el-table-column prop="message" label="消息" min-width="300" />
    </el-table>
    <p v-if="traceId && !loading && logs.length === 0" class="empty">未找到该 TraceID 的日志</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryLogs } from '../api/index.js'

const route = useRoute()
const router = useRouter()
const traceId = ref(route.params.traceId || '')
const loading = ref(false)
const logs = ref([])

const formatTime = (t) => new Date(t).toLocaleString('zh-CN')
const levelType = (l) => ({ INFO: '', WARN: 'warning', ERROR: 'danger', DEBUG: 'info' }[l] || '')
const handleLogout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

const fetchTrace = async () => {
  if (!traceId.value) return
  loading.value = true
  try {
    const { data } = await queryLogs({ trace_id: traceId.value, page: 1, page_size: 200 })
    logs.value = data.logs || []
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (traceId.value) fetchTrace()
})
</script>

<style scoped>
.trace-page { padding: 0; background: #f5f5f5; min-height: 100vh; }
.header { display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1px solid #eee; padding: 0 20px; }
.brand { font-size: 18px; font-weight: bold; }
.search-card { margin: 12px; }
.empty { text-align: center; padding: 40px; color: #999; }
</style>
