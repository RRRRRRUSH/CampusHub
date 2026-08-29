<template>
  <div class="profile-container">
    <el-row :gutter="20">

      <el-col :span="8">
        <el-card shadow="hover" class="box-card mb-4">
          <div class="user-header">
            <el-avatar :size="100" class="avatar-img">
              {{ userInfo.name?.[0] || '学' }}
            </el-avatar>
            <h2 class="mt-4">{{ userInfo.name }}</h2>
            <p class="text-gray">{{ userInfo.username }} | {{ userInfo.role === 'STUDENT' ? '普通学生' : '教职工' }}</p>
            <el-tag v-if="userInfo.isCadre" type="warning" class="mt-2">学生干部</el-tag>
          </div>

          <el-divider />

          <div class="stats-box">
            <div class="stat-item">
              <div class="label">竞赛学分</div>
              <div class="value text-blue">{{ userInfo.scoreCompetition || 0.0 }}</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="label">志愿工时</div>
              <div class="value text-green">{{ userInfo.scoreVolunteer || 0.0 }}</div>
            </div>
          </div>
        </el-card>

        <el-alert
          v-if="!isProfileComplete"
          title="请完善个人信息"
          type="error"
          description="检测到您尚未绑定班级或邮箱，这将影响认定审核与密码找回，请尽快完善。"
          show-icon
          :closable="false"
        />
      </el-col>

      <el-col :span="16">
        <el-card shadow="hover">
          <el-tabs v-model="activeTab">

            <el-tab-pane label="基本资料" name="info">
              <el-form :model="form" :rules="rules" ref="infoFormRef" label-width="100px" class="py-4">
                <el-form-item label="学号/工号">
                  <el-input v-model="form.username" disabled />
                </el-form-item>
                <el-form-item label="真实姓名">
                  <el-input v-model="form.name" disabled />
                </el-form-item>

                <el-divider content-position="left">学籍信息绑定</el-divider>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="所属学院" prop="college">
                      <el-input v-model="form.college" placeholder="例: 信息学院" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="专业名称" prop="major">
                      <el-input v-model="form.major" placeholder="例: 软件工程" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="年级" prop="grade">
                      <el-input v-model="form.grade" placeholder="例: 2021级" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="行政班级" prop="className">
                      <el-input v-model="form.className" placeholder="例: 软工2101班" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item>
                  <el-button type="primary" @click="updateProfile" :loading="loading">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="账号安全" name="security">
              <div class="security-item">
                <div class="sec-icon"><el-icon><Message /></el-icon></div>
                <div class="sec-content">
                  <div class="sec-title">安全邮箱</div>
                  <div class="sec-desc">
                    {{ userInfo.email ? `已绑定：${userInfo.email}` : '未绑定邮箱，用于找回密码' }}
                  </div>
                </div>
                <div class="sec-action">
                  <el-button link type="primary" @click="dialogEmailVisible = true">
                    {{ userInfo.email ? '修改' : '去绑定' }}
                  </el-button>
                </div>
              </div>

              <el-divider />

              <div class="security-item">
                <div class="sec-icon"><el-icon><Lock /></el-icon></div>
                <div class="sec-content">
                  <div class="sec-title">登录密码</div>
                  <div class="sec-desc">定期修改密码可以提高账号安全性</div>
                </div>
                <div class="sec-action">
                  <el-button link type="primary" @click="dialogPwdVisible = true">修改</el-button>
                </div>
              </div>
            </el-tab-pane>

          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogEmailVisible" title="绑定安全邮箱" width="400px">
      <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-width="80px">
        <el-form-item label="邮箱地址" prop="email">
          <el-input v-model="emailForm.email" placeholder="请输入您的常用邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogEmailVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="handleBindEmail">确认绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogPwdVisible" title="修改登录密码" width="400px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input v-model="pwdForm.oldPwd" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="pwdForm.newPwd" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="pwdForm.confirmPwd" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确认修改</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user-center.ts' // 🟢 引入 API
import { ElMessage } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const activeTab = ref('info')
const loading = ref(false)
const infoFormRef = ref()

const userInfo = ref<any>({})
const form = reactive({
  id: 0,
  username: '',
  name: '',
  college: '',
  major: '',
  grade: '',
  className: ''
})

const rules = {
  college: [{ required: true, message: '请输入学院', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }]
}

// 首次登录判定：如果班级或学院为空
const isProfileComplete = computed(() => {
  return userInfo.value.className && userInfo.value.college && userInfo.value.email
})

// 1. 获取最新用户信息
const fetchUserInfo = async () => {
  if (!userStore.id) return
  try {
    const res = await userApi.getInfo(userStore.id)
    userInfo.value = res
    // 同步到表单
    Object.assign(form, res)

    // 强制引导：如果信息不全，自动切到信息 Tab
    if (!res.className) {
      ElMessage.warning('检测到您是首次登录或信息未完善，请绑定班级信息！')
      activeTab.value = 'info'
    }
  } catch (e) { console.error(e) }
}

// 2. 更新资料
const updateProfile = async () => {
  await infoFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        await userApi.updateInfo(form)
        ElMessage.success('个人资料已更新')
        // 更新 Store 中的缓存信息 (可选)
        userStore.name = form.name
        userStore.department = form.college
        fetchUserInfo()
      } catch (e: any) {
        ElMessage.error('保存失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// --- 邮箱绑定逻辑 ---
const dialogEmailVisible = ref(false)
const binding = ref(false)
const emailFormRef = ref()
const emailForm = reactive({ email: '' })
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const handleBindEmail = async () => {
  await emailFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      binding.value = true
      try {
        await userApi.bindEmail({ userId: userStore.id!, email: emailForm.email })
        ElMessage.success('邮箱绑定成功')
        dialogEmailVisible.value = false
        fetchUserInfo()
      } catch (e: any) {
        ElMessage.error(e.message || '绑定失败')
      } finally {
        binding.value = false
      }
    }
  })
}

// --- 修改密码逻辑 ---
const dialogPwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPwd: '', newPwd: '', confirmPwd: '' })
const pwdRules = {
  oldPwd: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPwd: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== pwdForm.newPwd) callback(new Error('两次输入密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const handleChangePwd = async () => {
  await pwdFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      pwdLoading.value = true
      try {
        await userApi.updatePwd({
          userId: userStore.id,
          oldPassword: pwdForm.oldPwd,
          newPassword: pwdForm.newPwd
        })
        ElMessage.success('密码修改成功，请重新登录')
        userStore.logout()
      } catch (e: any) {
        ElMessage.error(e.message || '修改失败')
      } finally {
        pwdLoading.value = false
      }
    }
  })
}

onMounted(fetchUserInfo)
</script>

<style scoped>
.profile-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.user-header { text-align: center; padding: 20px 0; }
.avatar-img { background-color: #0056D2; color: white; font-size: 32px; font-weight: bold; }
.text-gray { color: #909399; font-size: 14px; margin-top: 8px; }
.mb-4 { margin-bottom: 20px; }
.mt-4 { margin-top: 16px; }
.mt-2 { margin-top: 8px; }
.py-4 { padding-top: 20px; padding-bottom: 20px; }

/* 统计卡片样式 */
.stats-box { display: flex; justify-content: space-around; align-items: center; padding: 10px 0; }
.stat-item { text-align: center; }
.stat-item .label { font-size: 13px; color: #909399; margin-bottom: 5px; }
.stat-item .value { font-size: 24px; font-weight: bold; }
.text-blue { color: #0056D2; }
.text-green { color: #67C23A; }
.stat-divider { width: 1px; height: 40px; background: #E4E7ED; }

/* 安全列表样式 */
.security-item { display: flex; align-items: center; padding: 15px 0; }
.sec-icon { font-size: 24px; color: #909399; margin-right: 15px; background: #f4f4f5; padding: 10px; border-radius: 50%; }
.sec-content { flex: 1; }
.sec-title { font-size: 16px; color: #303133; margin-bottom: 5px; }
.sec-desc { font-size: 13px; color: #909399; }
</style>
