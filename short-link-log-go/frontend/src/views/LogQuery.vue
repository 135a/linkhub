<template>
  <div class="log-page">
    <el-header class="header">
      <span class="brand">日志中心</span>
      <el-button @click="$router.push('/trace')">Trace 追踪</el-button>
      <el-button type="danger" @click="handleLogout">退出</el-button>
    </el-header>
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="搜索关键词" clearable />
        </el-form-item>
        <el-form-item label="服务">
          <el-select
            v-model="filters.service"
            placeholder="全部服务"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="svc in serviceOptions"
              :key="svc"
              :label="svc"
              :value="svc"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filters.level" placeholder="全部" clearable style="width: 120px">
            <el-option label="DEBUG" value="DEBUG" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="timeRange" type="datetimerange" start-placeholder="开始" end-placeholder="结束"
            style="width: 380px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">查询</el-button>
        </el-form-item>
      </el-form>
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
      <el-table-column prop="service" label="服务" width="160" />
      <el-table-column prop="trace_id" label="TraceID" width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="viewTrace(row.trace_id)" v-if="row.trace_id">{{ row.trace_id.slice(0, 8) }}...</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="消息" min-width="300">
        <template #default="{ row }">{{ highlightKeyword(row.message) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination class="pagination" v-model:current-page="page" :page-size="pageSize" :total="total"
      layout="total, prev, pager, next" @current-change="fetchLogs" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryLogs, listServices } from '../api/index.js'

const router = useRouter()
const loading = ref(false)
const logs = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(50)
const filters = reactive({ keyword: '', service: '', level: '' })
const timeRange = ref([])
const serviceOptions = ref([])

const formatTime = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}
const levelType = (l) => ({ INFO: '', WARN: 'warning', ERROR: 'danger', DEBUG: 'info' }[l] || '')
const highlightKeyword = (msg) => {
  if (!filters.keyword || !msg) return msg || ''
  return msg.replace(new RegExp(filters.keyword, 'gi'), (m) => `<mark>${m}</mark>`)
}
const viewTrace = (tid) => router.push(`/trace/${tid}`)
const handleLogout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

const fetchServiceOptions = async () => {
  try {
    const { data } = await listServices()
    serviceOptions.value = data.services || []
  } catch {
    // silently ignore — not critical
  }
}

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = { page: page.value, page_size: pageSize.value, ...filters }
    if (timeRange.value && timeRange.value.length === 2) {
      params.start_time = timeRange.value[0].toISOString()
      params.end_time = timeRange.value[1].toISOString()
    }
    const { data } = await queryLogs(params)
    logs.value = data.logs || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchServiceOptions()
  fetchLogs()
})
</script>

<style scoped>
.log-page { padding: 0; background: #f5f5f5; min-height: 100vh; }
.header { display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1px solid #eee; padding: 0 20px; }
.brand { font-size: 18px; font-weight: bold; }
.filter-card { margin: 12px; }
.pagination { display: flex; justify-content: center; padding: 16px 0; }
</style>

