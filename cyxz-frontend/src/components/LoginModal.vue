<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :width="420"
    :close-on-click-modal="false"
    align-center
    destroy-on-close
    class="login-dialog"
  >
    <template #header>
      <div class="dialog-header">
        <span :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</span>
        <span class="divider">|</span>
        <span :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</span>
      </div>
    </template>

    <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
      <el-form-item v-if="tab === 'login'" prop="username" label="手机号">
        <el-input v-model="form.username" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>
      <el-form-item v-if="tab === 'register'" prop="username" label="手机号">
        <el-input v-model="form.username" placeholder="请输入手机号注册" maxlength="11" />
      </el-form-item>

      <el-form-item prop="password" :label="tab === 'register' ? '设置密码' : '密码'">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>

      <el-form-item v-if="tab === 'register'" prop="confirmPassword" label="确认密码">
        <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
      </el-form-item>

      <el-form-item prop="captcha" label="验证码">
        <div class="captcha-row">
          <el-input v-model="form.captcha" placeholder="请输入验证码" maxlength="4" />
          <img :src="captchaImage" @click="loadCaptcha" class="captcha-img" alt="验证码" />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="handleSubmit" class="submit-btn">
          {{ tab === 'login' ? '登录' : '注册' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login, register, getCaptcha } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/api/auth'
import { getUserProfile } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ 'update:visible': [val: boolean] }>()

const userStore = useUserStore()
const router = useRouter()

const tab = ref<'login' | 'register'>('login')
const captchaImage = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<LoginRequest & RegisterRequest>({
  username: '',
  password: '',
  confirmPassword: '',
  captcha: '',
  captchaUuid: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    {
      validator: (_rule, value, cb) => {
        if (tab.value === 'register' && value !== form.password) {
          cb(new Error('两次密码不一致'))
        } else {
          cb()
        }
      },
      trigger: 'blur',
    },
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

watch(() => props.visible, (val) => {
  if (val) {
    tab.value = 'login'
    form.username = ''
    form.password = ''
    form.confirmPassword = ''
    form.captcha = ''
    form.captchaUuid = ''
    loadCaptcha()
  }
})

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    const data = res.data as any
    captchaImage.value = data.data?.image || data.image
    form.captchaUuid = data.data?.uuid || data.uuid
  } catch {
    ElMessage.error('获取验证码失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (tab.value === 'login') {
      const res = await login({
        username: form.username,
        password: form.password,
        captcha: form.captcha,
        captchaUuid: form.captchaUuid,
      })
      const data = res.data as any
      if (data.code === 200) {
        userStore.setToken(data.data.accessToken)
        ElMessage.success('登录成功')
        try {
          const profileRes = await getUserProfile(data.data.userId)
          const pdata = (profileRes.data as any).data || profileRes.data
          userStore.setUserInfo({ userId: data.data.userId, ...pdata })
        } catch {
          userStore.setUserInfo({ userId: data.data.userId, nickname: form.username } as any)
        }
        emit('update:visible', false)
        router.push(`/user/${data.data.userId}`)
      } else {
        ElMessage.error(data.message || '登录失败')
      }
    } else {
      const res = await register({
        username: form.username,
        password: form.password,
        confirmPassword: form.confirmPassword,
        captcha: form.captcha,
        captchaUuid: form.captchaUuid,
      })
      const data = res.data as any
      if (data.code === 200) {
        ElMessage.success('注册成功，请登录')
        tab.value = 'login'
        loadCaptcha()
      } else {
        ElMessage.error(data.message || '注册失败')
      }
    }
  } catch {
    ElMessage.error(tab.value === 'login' ? '登录失败' : '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-dialog :deep(.el-dialog__header) { margin: 0; padding: 20px 24px 12px; }
.login-dialog :deep(.el-dialog__body) { padding: 8px 24px 0; }
.login-dialog :deep(.el-dialog__footer) { padding: 12px 24px 20px; }

.dialog-header {
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.dialog-header span {
  cursor: pointer;
  transition: color 0.2s;
  color: var(--text-dim);
}

.dialog-header span.active {
  color: var(--pink);
}

.dialog-header .divider {
  margin: 0 12px;
  cursor: default;
  color: var(--border);
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-img {
  height: 40px;
  width: 120px;
  border-radius: 8px;
  cursor: pointer;
  border: 1.5px solid var(--border);
}

.submit-btn {
  width: 100%;
  height: 44px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: none;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.25);
}

.submit-btn:hover {
  box-shadow: 0 6px 24px rgba(255, 107, 157, 0.35);
}
</style>
