<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo-area">
        <img src="@/assets/lnu-logo.png" alt="Logo" class="logo-img" />
        <span class="logo-text">管理控制台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <el-sub-menu index="/admin/activity">
          <template #title>
            <el-icon><Flag /></el-icon>
            <span>志愿活动管理</span>
          </template>
          <el-menu-item index="/admin/activity/internal">校内志愿招募</el-menu-item>
          <el-menu-item index="/admin/activity/external">校外实践通知</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/admin/competition">
          <template #title>
            <el-icon><Trophy /></el-icon>
            <span>学科竞赛管理</span>
          </template>
          <el-menu-item index="/admin/competition/internal">校内赛事管理</el-menu-item>
          <el-menu-item index="/admin/competition/external">官方赛事通知</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/admin/audit">
          <template #title>
            <el-icon><Stamp /></el-icon>
            <span>学分认定审核</span>
          </template>
          <el-menu-item index="/admin/audit/volunteer">志愿工时认定</el-menu-item>
          <el-menu-item index="/admin/audit/competition">竞赛获奖认定</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/admin/users-manage">
          <template #title>
            <el-icon><User /></el-icon>
            <span>账户权限管理</span>
          </template>
          <el-menu-item index="/admin/users">学生信息查询</el-menu-item>

          <el-menu-item index="/admin/users/cadre">干部任命管理</el-menu-item>
        </el-sub-menu>

      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="breadcrumb">
        </div>
        <div class="user-info">
          <span class="mr-4 text-sm text-gray-600">
            欢迎，{{ userStore.name || '管理员' }}
            <span v-if="userStore.isCadre" style="color: #E6A23C; margin-left:5px;">(学生干部)</span>
          </span>
          <el-button type="danger" link size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Odometer, Flag, Trophy, Stamp, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 确保菜单高亮对应当前路由
const activeMenu = computed(() => route.path)

const handleLogout = () => {
  userStore.logout()
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.aside { background-color: #304156; color: white; display: flex; flex-direction: column; }
.logo-area { height: 60px; display: flex; align-items: center; justify-content: center; background-color: #2b3a4d; }
.logo-img { height: 32px; margin-right: 10px; }
.logo-text { font-weight: bold; font-size: 16px; }
.el-menu-vertical { border-right: none; flex: 1; }
.header { background: white; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; height: 60px; }
.main { background-color: #f0f2f5; padding: 20px; }
.mr-4 { margin-right: 16px; }
.text-sm { font-size: 14px; }
.text-gray-600 { color: #606266; }
</style>
