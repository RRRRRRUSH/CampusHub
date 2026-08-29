<template>
  <div class="login-container">
    <div class="login-box">
      <div class="header">
        <img src="@/assets/lnu-logo.png" alt="Logo" class="logo" />
        <div class="title">LNU 综合管理平台</div>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" size="large">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="管理员账号 / 学生干部学号"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>

      <div class="footer">
        <el-link type="info" @click="$router.push('/login')">切换至学生端</el-link>
      </div>
    </div>
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

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        // 1. 调用 API
        const userInfo = await authApi.login(form)

        // 🟢 2. 权限校验 (修改点)
        // 允许非学生角色，或者 是学生但拥有干部身份 (isCadre=true)
        if (userInfo.role === 'STUDENT' && !userInfo.isCadre) {
          ElMessage.error('权限拒绝：普通学生账号请前往学生端登录')
          return
        }

        // 🟢 3. 存入 Pinia
        userStore.login({
          id: userInfo.id,
          name: userInfo.name,
          dept: userInfo.college,
          role: userInfo.role,
          tokenStr: userInfo.token,
          username: userInfo.username,
          isCadre: userInfo.isCadre // 🟢 保存干部状态
        })

        ElMessage.success(`登录成功，欢迎 ${userInfo.name}`)

        // 🟢 4. 路由跳转
        // 如果是干部，直接跳到认定审核页 (因为他们可能没有其他权限)
        if (userInfo.role === 'STUDENT' && userInfo.isCadre) {
          router.push('/admin/audit/check')
        } else {
          router.push('/admin/dashboard')
        }

      } catch (error: any) {
        console.error(error)
        ElMessage.error(error.message || '登录失败，请检查账号密码')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #2d3a4b;
  background-image: url('https://images.unsplash.com/photo-1497294815431-9365093b7331?ixlib=rb-1.2.1&auto=format&fit=crop&w=1950&q=80');
  background-size: cover;
  position: relative;
}
.login-container::before {
  content: ''; position: absolute; top:0; left:0; width:100%; height:100%;
  background: rgba(45, 58, 75, 0.8);
}
.login-box {
  position: relative;
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}
.header { text-align: center; margin-bottom: 30px; }
.logo { height: 50px; margin-bottom: 10px; }
.title { font-size: 20px; font-weight: bold; color: #333; }
.w-full { width: 100%; }
.footer { margin-top: 20px; text-align: center; }
</style>
