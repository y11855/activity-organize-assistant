<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import { login } from '@/api/auth'
import { debounce } from '@/utils'
import { encryptPassword } from '@/utils/loginCrypto'

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
  let encryptedPassword: string
  try {
    // 密码先在前端用 RSA 公钥加密，请求体中不再出现明文
    encryptedPassword = await encryptPassword(form.value.password)
  } catch {
    ElMessage.error('密码加密失败，请刷新页面后重试')
    loading.value = false
    return
  }
  try {
    const res = await login({ username: form.value.username, password: encryptedPassword })
    userStore.setLoginInfo(res)
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
    <div class="login__panel">
      <h1 class="login__title">活动管家</h1>
      <p class="login__subtitle">社团活动组织与协作</p>
      <el-form :model="form" label-width="72px" @keyup.enter="onLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login__submit" :loading="loading" @click="onLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login__extra">
        <el-button link type="primary" @click="router.push('/register')">没有账号？注册</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f6f7f9;

  &__panel {
    width: 360px;
    padding: 36px 32px 24px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    box-shadow: 0 4px 20px rgba(31, 45, 61, 0.05);
  }

  &__title {
    margin: 0;
    text-align: center;
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    letter-spacing: 1px;
  }

  &__subtitle {
    margin: 8px 0 28px;
    text-align: center;
    font-size: 13px;
    color: #909399;
  }

  &__submit {
    width: 100%;
  }

  &__extra {
    display: flex;
    justify-content: center;
    margin-top: 4px;
  }
}
</style>
