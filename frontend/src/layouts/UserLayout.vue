<template>
  <div class="user-layout">
    <div class="nav-bar">
      <div class="nav-content">
        <div class="brand" @click="router.push('/home')">
          <img src="@/assets/lnu-logo.png" alt="Logo" class="logo-img" />
          <span class="brand-text">一站式服务大厅</span>
        </div>

        <div class="menu">
          <router-link to="/home" class="menu-item" active-class="active">首页</router-link>

          <router-link to="/competitions" class="menu-item" active-class="active">学科竞赛</router-link>
          <router-link to="/mycompetitions" class="menu-item" active-class="active">我的竞赛</router-link>

          <router-link to="/activities" class="menu-item" active-class="active">志愿活动</router-link>
          <router-link to="/my-participations" class="menu-item" active-class="active">我的志愿</router-link>

          <router-link to="/application" class="menu-item" active-class="active">认定申请</router-link>
          <router-link to="/notices" class="menu-item" active-class="active">通知公告</router-link>
        </div>

        <div class="user-actions">
          <router-link to="/profile" class="profile-link mr-4">个人中心</router-link>

          <el-dropdown>
            <span class="el-dropdown-link">
              <el-avatar :size="32" style="background: #0056D2; color: white;">
                {{ userStore.name?.[0] || userStore.username?.[0] || '生' }}
              </el-avatar>
              <span class="username">{{ userStore.name || userStore.username }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">账号设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <div class="page-content">
      <router-view />
    </div>

    <div class="footer">
      <p>© 2025 辽宁大学 | 智慧校园服务平台</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.user-layout { min-height: 100vh; background-color: #f5f7fa; display: flex; flex-direction: column; }

/* 导航栏固定样式 */
.nav-bar {
  height: 64px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}

.nav-content {
  width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 品牌 Logo */
.brand { display: flex; align-items: center; cursor: pointer; }
.logo-img { height: 36px; margin-right: 10px; }
.brand-text { font-size: 18px; font-weight: bold; color: #303133; }

/* 菜单项 */
.menu { display: flex; gap: 24px; /*稍微加大间距让视觉更舒展*/ }
.menu-item {
  text-decoration: none;
  color: #606266;
  font-size: 15px;
  padding: 0 4px;
  height: 64px;
  line-height: 64px;
  border-bottom: 3px solid transparent; /* 预留边框位置，防止hover时抖动 */
  transition: all 0.2s ease-in-out;
  font-weight: 500;
}

.menu-item:hover {
  color: #0056D2;
}

/* 激活状态样式 */
.menu-item.active {
  color: #0056D2;
  border-bottom-color: #0056D2;
  font-weight: bold;
}

/* 用户操作区 */
.user-actions { display: flex; align-items: center; }
.profile-link { text-decoration: none; color: #606266; font-size: 14px; margin-right: 20px; transition: color 0.3s; }
.profile-link:hover { color: #0056D2; }

.el-dropdown-link { display: flex; align-items: center; cursor: pointer; outline: none; }
.username { margin-left: 8px; font-size: 14px; color: #303133; font-weight: 500; }

/* 页面主体内容区 */
.page-content { width: 1200px; margin: 24px auto; flex: 1; }

/* 页脚 */
.footer {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 12px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
  margin-top: auto;
}
</style>
