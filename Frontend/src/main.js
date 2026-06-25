import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/theme.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

app.use(router) // 必须启用路由
app.use(ElementPlus) // 引入 Element Plus

app.mount('#app')
