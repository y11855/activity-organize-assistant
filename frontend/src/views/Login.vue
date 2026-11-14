<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import { login, loginByEmailCode, getCaptcha, sendEmailCode, sendResetCode, resetPassword } from '@/api/auth'
import { debounce } from '@/utils'
import { encryptPassword } from '@/utils/loginCrypto'

const router = useRouter()
const userStore = useUserStore()

// 登录方式：password 密码登录 | email 邮箱验证码登录
const loginMode = ref<'password' | 'email'>('password')
const loading = ref(false)

// 密码登录表单
const pwdForm = ref({ username: '', password: '', captchaCode: '' })
const captchaKey = ref('')
const captchaImg = ref('')

// 邮箱验证码登录表单
const codeForm = ref({ email: '', code: '' })
const codeSending = ref(false)

/** 倒计时控制器：start 立即置灰并递减，stop 取消并恢复可点击 */
const createCountdown = (total = 60) => {
  const seconds = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  const stop = () => {
    if (timer) clearInterval(timer)
    timer = null
    seconds.value = 0
  }

  const start = () => {
    stop()
    seconds.value = total
    timer = setInterval(() => {
      seconds.value--
      if (seconds.value <= 0) stop()
    }, 1000)
  }

  return { seconds, start, stop }
}

const { seconds: countdown, start: startCodeCountdown, stop: stopCodeCountdown } = createCountdown()

// 加载图形验证码
const loadCaptcha = async () => {
  try {
    const res = await getCaptcha()
    captchaKey.value = res.key
    captchaImg.value = res.imageBase64
  } catch {
    ElMessage.error('验证码加载失败')
  }
}

// 发送邮箱验证码：点击即置灰倒计时，防止重复点击/重复发送
const onSendCode = async () => {
  if (codeSending.value || countdown.value > 0) return
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(codeForm.value.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  codeSending.value = true
  startCodeCountdown()
  try {
    await sendEmailCode(codeForm.value.email)
    ElMessage.success('验证码已发送，请查收邮箱')
  } catch {
    // 发送失败时恢复按钮，允许立即重试
    stopCodeCountdown()
  } finally {
    codeSending.value = false
  }
}

// 密码登录
const onPwdLogin = debounce(async () => {
  if (!pwdForm.value.username || !pwdForm.value.password || !pwdForm.value.captchaCode) {
    ElMessage.warning('请输入用户名、密码和验证码')
    return
  }
  if (loading.value) return
  loading.value = true
  let encryptedPassword: string
  try {
    encryptedPassword = await encryptPassword(pwdForm.value.password)
  } catch {
    ElMessage.error('密码加密失败，请刷新页面后重试')
    loading.value = false
    return
  }
  try {
    const res = await login({
      username: pwdForm.value.username,
      password: encryptedPassword,
      captchaKey: captchaKey.value,
      captchaCode: pwdForm.value.captchaCode
    })
    userStore.setLoginInfo(res)
    ElMessage.success(`欢迎，${res.nickname || res.username}`)
    router.push('/dashboard')
  } catch {
    loadCaptcha() // 登录失败刷新验证码
  } finally {
    loading.value = false
  }
}, 500)

// 邮箱验证码登录
const onCodeLogin = debounce(async () => {
  if (!codeForm.value.email || !codeForm.value.code) {
    ElMessage.warning('请输入邮箱和验证码')
    return
  }
  if (loading.value) return
  loading.value = true
  try {
    const res = await loginByEmailCode({ email: codeForm.value.email, code: codeForm.value.code })
    userStore.setLoginInfo(res)
    ElMessage.success(`欢迎，${res.nickname || res.username}`)
    router.push('/dashboard')
  } catch {
    // 拦截器统一提示
  } finally {
    loading.value = false
  }
}, 500)

// ===== 忘记密码 =====
const resetVisible = ref(false)
const resetLoading = ref(false)
const resetCodeSending = ref(false)
const { seconds: resetCountdown, start: startResetCountdown, stop: stopResetCountdown } = createCountdown()
const resetForm = ref({ email: '', code: '', password: '', confirmPassword: '' })

function openResetDialog() {
  resetForm.value = { email: '', code: '', password: '', confirmPassword: '' }
  stopResetCountdown()
  resetVisible.value = true
}

// 发送重置密码验证码：同样点击即置灰倒计时
const onSendResetCode = async () => {
  if (resetCodeSending.value || resetCountdown.value > 0) return
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(resetForm.value.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  resetCodeSending.value = true
  startResetCountdown()
  try {
    await sendResetCode(resetForm.value.email)
    ElMessage.success('验证码已发送，请查收邮箱')
  } catch {
    stopResetCountdown()
  } finally {
    resetCodeSending.value = false
  }
}

const onResetPassword = debounce(async () => {
  if (!resetForm.value.email || !resetForm.value.code) {
    ElMessage.warning('请输入邮箱和验证码')
    return
  }
  if (resetForm.value.password.length < 6 || resetForm.value.password.length > 20) {
    ElMessage.warning('密码长度需为 6-20 位')
    return
  }
  if (resetForm.value.password !== resetForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (resetLoading.value) return
  resetLoading.value = true
  let encryptedPassword: string
  try {
    encryptedPassword = await encryptPassword(resetForm.value.password)
  } catch {
    ElMessage.error('密码加密失败，请刷新页面后重试')
    resetLoading.value = false
    return
  }
  try {
    await resetPassword({
      email: resetForm.value.email,
      code: resetForm.value.code,
      password: encryptedPassword
    })
    resetVisible.value = false
    ElMessage.success('密码已重置，请使用新密码登录')
    loginMode.value = 'password'
    loadCaptcha()
  } catch {
    // 拦截器统一提示
  } finally {
    resetLoading.value = false
  }
}, 500)

onMounted(loadCaptcha)
</script>

<template>
  <div class="login">
    <div class="login__panel">
      <h1 class="login__title">活动管家</h1>
      <p class="login__subtitle">社团活动组织与协作</p>

      <el-tabs v-model="loginMode" class="login__tabs">
        <el-tab-pane label="密码登录" name="password" />
        <el-tab-pane label="邮箱验证码登录" name="email" />
      </el-tabs>

      <!-- 密码登录 -->
      <el-form v-show="loginMode === 'password'" :model="pwdForm" label-width="72px" @keyup.enter="onPwdLogin">
        <el-form-item label="用户名">
          <el-input v-model="pwdForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="pwdForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="login__captcha">
            <el-input v-model="pwdForm.captchaCode" placeholder="请输入验证码" />
            <img :src="captchaImg" class="login__captcha-img" @click="loadCaptcha" title="点击刷新" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login__submit" :loading="loading" @click="onPwdLogin">登录</el-button>
        </el-form-item>
      </el-form>

      <!-- 邮箱验证码登录 -->
      <el-form v-show="loginMode === 'email'" :model="codeForm" label-width="72px" @keyup.enter="onCodeLogin">
        <el-form-item label="邮箱">
          <el-input v-model="codeForm.email" placeholder="请输入已绑定的邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="login__captcha">
            <el-input v-model="codeForm.code" placeholder="请输入6位验证码" />
            <el-button
              type="primary"
              plain
              :loading="codeSending"
              :disabled="countdown > 0"
              @click="onSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login__submit" :loading="loading" @click="onCodeLogin">登录</el-button>
        </el-form-item>
        <div class="login__tip">首次使用请先用密码登录，并在右上角头像菜单中绑定邮箱</div>
      </el-form>

      <div class="login__extra">
        <el-button link type="primary" @click="openResetDialog">忘记密码？</el-button>
        <el-button link type="primary" @click="router.push('/register')">没有账号？注册</el-button>
      </div>
    </div>

    <!-- 忘记密码 -->
    <el-dialog v-model="resetVisible" title="忘记密码" width="440px">
      <p class="reset__tip">请输入已绑定账号的邮箱，通过邮箱验证码重置密码。未绑定邮箱请联系管理员重置。</p>
      <el-form :model="resetForm" label-width="72px" @keyup.enter="onResetPassword">
        <el-form-item label="邮箱">
          <el-input v-model="resetForm.email" placeholder="请输入已绑定的邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="login__captcha">
            <el-input v-model="resetForm.code" placeholder="请输入6位验证码" />
            <el-button
              type="primary"
              plain
              :loading="resetCodeSending"
              :disabled="resetCountdown > 0"
              @click="onSendResetCode"
            >
              {{ resetCountdown > 0 ? `${resetCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.password" type="password" placeholder="请输入新密码（6-20位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="resetForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="onResetPassword">重置密码</el-button>
      </template>
    </el-dialog>
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
    width: 380px;
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
    margin: 8px 0 20px;
    text-align: center;
    font-size: 13px;
    color: #909399;
  }

  &__tabs {
    :deep(.el-tabs__nav) {
      width: 100%;
    }
    :deep(.el-tabs__item) {
      width: 50%;
      text-align: center;
    }
  }

  &__captcha {
    display: flex;
    gap: 10px;
    width: 100%;
    .el-input {
      flex: 1;
    }
  }

  &__captcha-img {
    width: 110px;
    height: 40px;
    border-radius: 4px;
    cursor: pointer;
    border: 1px solid #dcdfe6;
  }

  &__submit {
    width: 100%;
  }

  &__extra {
    display: flex;
    justify-content: space-between;
    margin-top: 4px;
  }

  &__tip {
    margin: -8px 0 12px;
    font-size: 12px;
    line-height: 1.5;
    color: #909399;
    text-align: center;
  }
}

.reset {
  &__tip {
    margin: 0 0 16px;
    font-size: 13px;
    line-height: 1.6;
    color: #909399;
  }
}
</style>
