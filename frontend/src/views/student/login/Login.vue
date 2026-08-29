<template>
  <div class="student-login-container">
    <div class="bg-circle-1"></div>
    <div class="bg-circle-2"></div>

    <div class="login-content">
      <div class="left-panel">
        <div class="logo-box">
          <img src="@/assets/lnu-logo.png" alt="Logo" class="school-logo" />
          <div class="divider"></div>
          <div class="platform-name">
            <h2>一站式服务大厅</h2>
            <p>One-stop Service Hall</p>
          </div>
        </div>
        <div class="welcome-text">
          <h3>智慧校园，触手可及</h3>
          <p>志愿服务 · 学科竞赛 · 学分认定 · 校园资讯</p>
        </div>
      </div>

      <div class="right-panel">
        <div class="login-box">
          <h3 class="box-title">统一身份认证</h3>

          <el-form :model="form" :rules="rules" ref="formRef" size="large" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="学号 / 工号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" @keyup.enter="handleLogin" />
            </el-form-item>

            <div class="extra-options">
              <el-checkbox v-model="form.remember">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码?</el-link>
            </div>

            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">立即登录</el-button>

            <div class="footer-links">
              <span>还没有账号？</span>
              <el-link type="primary">新生激活</el-link>
              <el-divider direction="vertical" />
              <el-link type="info" @click="$router.push('/admin/login')">管理端入口</el-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
    <div class="copyright">© 2025 辽宁大学信息化中心 | 技术支持：LNU Dev Team</div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '', remember: false })

const rules = {
  username: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        // 1. 调用后端接口
        const userInfo = await authApi.login({
          username: form.username,
          password: form.password
        })

        // 🟢 2. 存入 Pinia (包括 ID)
        userStore.login({
          id: userInfo.id,            // 🟢 关键修复：保存 ID
          tokenStr: userInfo.token,
          name: userInfo.name,
          dept: userInfo.college,
          role: userInfo.role,
          username: userInfo.username
        })

        ElMessage.success(`欢迎回来，${userInfo.name}`)
        router.push('/home')
      } catch (e: any) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.student-login-container { height: 100vh; width: 100%; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); background-image: url('https://images.unsplash.com/photo-1541339907198-e08756dedf3f?ixlib=rb-1.2.1&auto=format&fit=crop&w=1920&q=80'); background-size: cover; background-position: center; display: flex; justify-content: center; align-items: center; position: relative; overflow: hidden; }
.student-login-container::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 86, 210, 0.7); z-index: 0; }
.login-content { position: relative; z-index: 1; display: flex; width: 900px; height: 500px; background: white; border-radius: 16px; box-shadow: 0 20px 50px rgba(0,0,0,0.2); overflow: hidden; }
.left-panel { width: 50%; background: linear-gradient(135deg, #0056D2 0%, #0041a3 100%); padding: 40px; display: flex; flex-direction: column; justify-content: space-between; color: white; }
.logo-box { display: flex; align-items: center; gap: 15px; }
.school-logo { height: 50px; background: white; border-radius: 50%; padding: 2px; }
.divider { width: 1px; height: 40px; background: rgba(255,255,255,0.3); }
.platform-name h2 { margin: 0; font-size: 20px; letter-spacing: 1px; }
.platform-name p { margin: 0; font-size: 12px; opacity: 0.8; text-transform: uppercase; }
.welcome-text h3 { font-size: 28px; margin-bottom: 10px; font-weight: 300; }
.welcome-text p { opacity: 0.8; font-size: 14px; letter-spacing: 1px; }
.right-panel { width: 50%; padding: 40px; display: flex; align-items: center; justify-content: center; }
.login-box { width: 100%; max-width: 320px; }
.box-title { font-size: 22px; color: #333; margin-bottom: 30px; text-align: center; font-weight: 600; }
.submit-btn { width: 100%; font-weight: bold; height: 44px; font-size: 16px; margin-top: 10px; background-color: #0056D2; }
.submit-btn:hover { background-color: #0041a3; }
.extra-options { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; font-size: 14px; }
.footer-links { margin-top: 20px; text-align: center; font-size: 13px; color: #909399; }
.footer-links .el-link { margin: 0 5px; vertical-align: baseline; }
.copyright { position: absolute; bottom: 20px; color: rgba(255,255,255,0.6); font-size: 12px; z-index: 1; }
.bg-circle-1 { position: absolute; width: 400px; height: 400px; border-radius: 50%; background: rgba(255,255,255,0.1); top: -100px; right: -100px; z-index: 0; }
.bg-circle-2 { position: absolute; width: 300px; height: 300px; border-radius: 50%; background: rgba(255,255,255,0.1); bottom: -50px; left: -50px; z-index: 0; }
</style>
