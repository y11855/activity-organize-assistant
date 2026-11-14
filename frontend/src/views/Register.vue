<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import { register } from '@/api/auth'
import { debounce } from '@/utils'
import { encryptPassword } from '@/utils/loginCrypto'

const router = useRouter()
const userStore = useUserStore()
const form = ref({ username: '', password: '', confirmPassword: '', nickname: '', email: '' })
const loading = ref(false)

const onRegister = debounce(async () => {
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(form.value.username)) {
    ElMessage.warning('用户名需为 4-20 位字母、数字或下划线')
    return
  }
  if (form.value.password.length < 6 || form.value.password.length > 20) {
    ElMessage.warning('密码长度需为 6-20 位')
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (form.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  if (loading.value) return

  loading.value = true
  let encryptedPassword: string
  try {
    // 与登录一致：密码先在前端用 RSA 公钥加密，请求体中不出现明文
    encryptedPassword = await encryptPassword(form.value.password)
  } catch {
    ElMessage.error('密码加密失败，请刷新页面后重试')
    loading.value = false
    return
  }
  try {
    const res = await register({
      username: form.value.username,
      password: encryptedPassword,
      nickname: form.value.nickname || undefined,
      email: form.value.email || undefined
    })
    userStore.setLoginInfo(res)
    ElMessage.success('注册成功')
    router.push('/dashboard')
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    loading.value = false
  }
}, 500)
</script>

<template>
  <div class="register">
    <div class="register__panel">
      <h1 class="register__title">创建账号</h1>
      <p class="register__subtitle">注册后即可创建并协作管理活动</p>
      <el-form :model="form" label-width="80px" @keyup.enter="onRegister">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="4-20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="选填，默认与用户名相同" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="选填，用于邮箱验证码登录" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="register__submit" :loading="loading" @click="onRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register__extra">
        <el-button link type="primary" @click="router.push('/login')">已有账号？返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.register {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f6f7f9;

  &__panel {
    width: 400px;
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
