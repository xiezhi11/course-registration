<template>
  <div id="app">
    <el-container v-if="isLoggedIn">
      <el-header class="header">
        <div class="header-content">
          <div class="logo">课程报名管理系统</div>
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            class="menu"
            router
          >
            <el-menu-item index="/courses">课程列表</el-menu-item>
            <el-menu-item index="/my-registrations">我的报名</el-menu-item>
            <el-menu-item v-if="isAdmin" index="/admin">审核管理</el-menu-item>
          </el-menu>
          <div class="user-info">
            <span>{{ currentUser?.name }} ({{ currentUser?.role === 'ADMIN' ? '管理员' : '员工' }})</span>
            <el-button type="text" @click="logout">退出</el-button>
          </div>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
    <router-view v-else />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'

const router = useRouter()
const route = useRoute()
const store = useStore()

const isLoggedIn = computed(() => store.getters.isLoggedIn)
const currentUser = computed(() => store.getters.currentUser)
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')

const activeMenu = computed(() => route.path)

const logout = () => {
  store.dispatch('logout')
  router.push('/login')
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}

#app {
  min-height: 100vh;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  height: 60px;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  margin-right: 40px;
}

.menu {
  flex: 1;
  border-bottom: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
}

.main {
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}
</style>
