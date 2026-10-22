<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import { login } from '@/api/auth'
import { debounce } from '@/utils'

const router = useRouter()
const userStore = useUserStore()
const form = ref({ username: '', password: '' })
const loading = ref(false)

const onLogin = debounce(async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  if (loading.value) return

  loading.value = true
  try {
    const res = await login(form.value)
    userStore.setToken(res.token)
    userStore.setUserInfo({
      id: res.id,
      username: res.username,
      nickname: res.nickname,
      roles: res.role ? [res.role] : []
    })
    ElMessage.success(`欢迎，${res.nickname || res.username}`)
    router.push('/activities')
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    loading.value = false
  }
}, 500)
</script>

<template>
  <div class="login">
    <el-card class="login__card">
      <h2>活动管家</h2>
      <el-form :model="form" label-width="80px" @keyup.enter="onLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onLogin">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="login__tip">测试账号：admin / 123456（成员：zhangsan、lisi、wangwu、zhaoliu / 123456）</div>
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

  &__tip {
    color: #999;
    font-size: 12px;
    text-align: center;
  }
}
</style>
