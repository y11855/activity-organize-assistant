<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()
const form = ref({ username: '', password: '' })

function onLogin() {
  userStore.setToken('demo-token')
  userStore.setUserInfo({
    id: 1,
    username: 'admin',
    nickname: '管理员',
    roles: ['admin']
  })
  router.push('/dashboard')
}
</script>

<template>
  <div class="login">
    <el-card class="login__card">
      <h2>实训 AI 评价系统</h2>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onLogin">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  &__card {
    width: 400px;
  }
}
</style>
