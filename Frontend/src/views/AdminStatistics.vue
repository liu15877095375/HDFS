<template>
  <div class="statistics-container">
    <div class="header">
      <h2>用户行为数据统计</h2>
    </div>
    
    <div class="stats-grid">
      <div class="stat-card total">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalOperations }}</div>
          <div class="stat-label">总操作次数</div>
        </div>
      </div>
      
      <div class="stat-card today">
        <div class="stat-icon">🔥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayOperations }}</div>
          <div class="stat-label">今日操作</div>
        </div>
      </div>
      
      <div class="stat-card upload">
        <div class="stat-icon">📤</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.uploadCount }}</div>
          <div class="stat-label">上传次数</div>
        </div>
      </div>
      
      <div class="stat-card download">
        <div class="stat-icon">📥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.downloadCount }}</div>
          <div class="stat-label">下载次数</div>
        </div>
      </div>
      
      <div class="stat-card delete">
        <div class="stat-icon">🗑️</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.deleteCount }}</div>
          <div class="stat-label">删除次数</div>
        </div>
      </div>
      
      <div class="stat-card share">
        <div class="stat-icon">🔗</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.shareCount }}</div>
          <div class="stat-label">分享次数</div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <h3>近7日操作趋势</h3>
        <div class="bar-chart">
          <div 
            v-for="(item, index) in stats.dailyTrend" 
            :key="index" 
            class="bar-item"
          >
            <div 
              class="bar" 
              :style="{ height: getBarHeight(item.count) + '%' }"
            >
              <span class="bar-value">{{ item.count }}</span>
            </div>
            <span class="bar-label">{{ formatDate(item.date) }}</span>
          </div>
        </div>
      </div>
      
      <div class="chart-card">
        <h3>操作类型分布</h3>
        <div class="pie-chart-container">
          <div class="pie-chart">
            <svg viewBox="0 0 100 100">
              <circle
                v-for="(item, index) in pieData"
                :key="index"
                :cx="50"
                :cy="50"
                :r="40"
                :fill="pieColors[index]"
                :stroke="pieColors[index]"
                :stroke-width="20"
                :stroke-dasharray="item.dashArray"
                :stroke-dashoffset="item.dashOffset"
                transform="rotate(-90 50 50)"
                style="fill: none;"
              />
            </svg>
            <div class="pie-center">
              <span class="pie-total">{{ stats.totalOperations }}</span>
              <span class="pie-label">总操作</span>
            </div>
          </div>
          <div class="legend">
            <div 
              v-for="(item, index) in stats.typeDistribution" 
              :key="index" 
              class="legend-item"
            >
              <span class="legend-color" :style="{ backgroundColor: pieColors[index] }"></span>
              <span class="legend-text">{{ item.typeName }} ({{ item.count }})</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chart-card">
      <h3>活跃用户排行</h3>
      <div class="ranking-list">
        <div 
          v-for="(user, index) in stats.topActiveUsers" 
          :key="user.userId" 
          class="ranking-item"
        >
          <span class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
          <span class="user-name">{{ user.username }}</span>
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :style="{ width: getUserProgress(user.operationCount) + '%' }"
            ></div>
          </div>
          <span class="operation-count">{{ user.operationCount }} 次</span>
        </div>
        <div v-if="stats.topActiveUsers.length === 0" class="empty-state">
          暂无数据
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '@/utils/api'

const stats = reactive({
  totalOperations: 0,
  todayOperations: 0,
  uploadCount: 0,
  downloadCount: 0,
  deleteCount: 0,
  moveCount: 0,
  shareCount: 0,
  dailyTrend: [],
  typeDistribution: [],
  topActiveUsers: []
})

const pieColors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452'
]

const maxBarValue = ref(1)

const pieData = computed(() => {
  const total = stats.totalOperations || 1
  let currentOffset = 0
  return stats.typeDistribution.map((item, index) => {
    const percentage = (item.count / total) * 100
    const dashArray = `${percentage * 2.51} 251`
    const dashOffset = -currentOffset * 2.51
    currentOffset += percentage
    return { dashArray, dashOffset }
  })
})

const loadStatistics = async () => {
  try {
    const res = await api.get('/admin/statistics/operation')
    if (res.code === 200) {
      Object.assign(stats, res.data)
      if (stats.dailyTrend.length > 0) {
        maxBarValue.value = Math.max(...stats.dailyTrend.map(item => item.count), 1)
      }
    }
  } catch (err) {
    console.error('获取统计数据失败:', err)
  }
}

const getBarHeight = (count) => {
  return (count / maxBarValue.value) * 100
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const getUserProgress = (count) => {
  if (stats.topActiveUsers.length === 0) return 0
  const maxCount = Math.max(...stats.topActiveUsers.map(u => u.operationCount), 1)
  return (count / maxCount) * 100
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-container {
  padding: 48px;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.statistics-container::before {
  content: '';
  position: absolute;
  width: 1000px;
  height: 1000px;
  background: radial-gradient(circle, rgba(255,255,255,0.03) 0%, transparent 70%);
  border-radius: 50%;
  top: -300px;
  right: -300px;
  animation: float 12s ease-in-out infinite;
}

.statistics-container::after {
  content: '';
  position: absolute;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(255,255,255,0.03) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -200px;
  left: -200px;
  animation: float 14s ease-in-out infinite reverse;
}

.statistics-container > * {
  position: relative;
  z-index: 1;
}

.header {
  margin-bottom: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 24px 32px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  animation: fadeInUp 0.5s ease-out;
}

.header h2 {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  color: white;
  box-shadow: var(--shadow-lg);
  transition: all var(--transition-normal);
  animation: fadeInUp 0.5s ease-out both;
}

.stat-card:nth-child(1) { animation-delay: 0.1s; }
.stat-card:nth-child(2) { animation-delay: 0.2s; }
.stat-card:nth-child(3) { animation-delay: 0.3s; }
.stat-card:nth-child(4) { animation-delay: 0.4s; }
.stat-card:nth-child(5) { animation-delay: 0.5s; }
.stat-card:nth-child(6) { animation-delay: 0.6s; }

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-2xl);
}

.stat-card.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card.today {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card.upload {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card.download {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-card.delete {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-card.share {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
}

.stat-icon {
  font-size: 40px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  font-size: 14px;
  opacity: 0.8;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: var(--shadow-xl);
  animation: fadeInUp 0.5s ease-out both;
}

.chart-card:nth-child(1) { animation-delay: 0.7s; }
.chart-card:nth-child(2) { animation-delay: 0.8s; }
.chart-card:nth-child(3) { animation-delay: 0.9s; }

.chart-card h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 24px 0;
}

.bar-chart {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 220px;
  padding: 20px 0;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar {
  width: 36px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  position: relative;
  transition: height 0.5s ease;
  min-height: 12px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.bar-value {
  position: absolute;
  top: -28px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
  white-space: nowrap;
}

.bar-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 12px;
  font-weight: 500;
}

.pie-chart-container {
  display: flex;
  align-items: center;
  gap: 40px;
}

.pie-chart {
  position: relative;
  width: 180px;
  height: 180px;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.pie-total {
  display: block;
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.pie-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.legend {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.legend-item:hover {
  background: var(--primary-50);
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.legend-text {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.ranking-item:hover {
  background: var(--primary-50);
  transform: translateX(4px);
}

.rank-badge {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: white;
  box-shadow: var(--shadow-md);
}

.rank-1 {
  background: linear-gradient(135deg, #ffd700 0%, #ffb700 100%);
}

.rank-2 {
  background: linear-gradient(135deg, #c0c0c0 0%, #a0a0a0 100%);
}

.rank-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%);
}

.rank-badge:not(.rank-1):not(.rank-2):not(.rank-3) {
  background: var(--text-secondary);
}

.user-name {
  width: 120px;
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 600;
}

.progress-bar {
  flex: 1;
  height: 10px;
  background: var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--radius-md);
  transition: width 0.5s ease;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.operation-count {
  width: 90px;
  text-align: right;
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 600;
}

.empty-state {
  text-align: center;
  color: var(--text-secondary);
  padding: 32px 20px;
  font-size: 15px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .charts-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
