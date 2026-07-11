<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="auth-overlay" @click.self="close">
        <div class="auth-modal">
          <!-- 左侧：平台介绍 -->
          <div class="auth-left">
            <div class="auth-left-inner">
              <div class="brand">
                <h1>次元小站</h1>
                <p class="brand-en">CYXZ Community</p>
              </div>
              <div class="features">
                <div class="feature-item">
                  <img src="@/assets/icons/edit.svg" alt="edit" class="feature-icon" />
                  <div>
                    <h3>创作分享</h3>
                    <p>发布绘画、摄影、Cosplay，展示你的创意世界</p>
                  </div>
                </div>
                <div class="feature-item">
                  <img src="@/assets/icons/handshake.svg" alt="handshake" class="feature-icon" />
                  <div>
                    <h3>同好社区</h3>
                    <p>找到志同道合的伙伴，一起追番聊番</p>
                  </div>
                </div>
                <div class="feature-item">
                  <img src="@/assets/icons/sparkle.svg" alt="sparkle" class="feature-icon" />
                  <div>
                    <h3>发现灵感</h3>
                    <p>探索海量优质内容，每一次浏览都是惊喜</p>
                  </div>
                </div>
              </div>
              <div class="left-footer">
                <div class="footer-divider"></div>
                <p class="footer-quote">"每一个热爱，都值得被看见"</p>
                <div class="footer-tags">
                  <span>绘画</span>
                  <span>摄影</span>
                  <span>Cosplay</span>
                  <span>追番</span>
                  <span>同人</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：登录注册表单 -->
          <div class="auth-right">
            <button class="close-btn" @click="close">✕</button>

            <h2 class="form-title">
              {{ tab === 'login' ? '欢迎回来' : '加入次元小站' }}
            </h2>
            <p class="form-subtitle">
              {{ tab === 'login' ? '登录你的账号，继续探索' : '创建账号，开启你的创作之旅' }}
            </p>

            <el-form
              :model="form"
              :rules="rules"
              ref="formRef"
              label-position="top"
              @submit.prevent
              class="auth-form"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="form.username"
                  placeholder="请输入账号"
                  maxlength="11"
                  size="large"
                  prefix-icon="User"
                />
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="form.password"
                  type="password"
                  :placeholder="tab === 'register' ? '设置密码（至少6位）' : '请输入密码'"
                  show-password
                  size="large"
                  prefix-icon="Lock"
                />
              </el-form-item>

              <el-form-item v-if="tab === 'register'" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  show-password
                  size="large"
                  prefix-icon="Lock"
                />
              </el-form-item>

              <el-form-item prop="captcha">
                <div class="captcha-row">
                  <el-input
                    v-model="form.captcha"
                    placeholder="请输入验证码"
                    maxlength="4"
                    size="large"
                    prefix-icon="Key"
                  />
                  <img
                    :src="captchaImage"
                    @click="loadCaptcha"
                    class="captcha-img"
                    alt="验证码"
                  />
                </div>
              </el-form-item>

              <el-button
                type="primary"
                :loading="submitting"
                @click="handleSubmit"
                class="submit-btn"
                size="large"
              >
                {{ tab === 'login' ? '登 录' : '注 册' }}
              </el-button>
            </el-form>

            <div class="form-footer">
              <span v-if="tab === 'login'">
                还没有账号？
                <a @click="tab = 'register'">立即注册</a>
              </span>
              <span v-else>
                已有账号？
                <a @click="tab = 'login'">去登录</a>
              </span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
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
    { required: true, message: '请输入账号', trigger: 'blur' },
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

function close() {
  emit('update:visible', false)
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
/* Overlay */
.auth-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Modal */
.auth-modal {
  width: 860px;
  max-width: 95vw;
  height: 520px;
  max-height: 90vh;
  background: white;
  border-radius: 20px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.15);
  position: relative;
}

/* ===== Left Side ===== */
.auth-left {
  width: 380px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #ff6b9d 0%, #c084fc 50%, #60a5fa 100%);
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
.auth-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(255,255,255,0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.1) 0%, transparent 40%);
}
.auth-left-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.brand {
  margin-bottom: 40px;
}
.brand h1 {
  color: white;
  font-size: 28px;
  font-weight: 800;
  margin: 0;
  letter-spacing: 2px;
}
.brand-en {
  color: rgba(255,255,255,0.75);
  font-size: 12px;
  letter-spacing: 3px;
  margin-top: 4px;
}

.features {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.feature-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.feature-icon {
  font-size: 24px;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}
.feature-item h3 {
  color: white;
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 4px;
}
.feature-item p {
  color: rgba(255,255,255,0.8);
  font-size: 12px;
  margin: 0;
  line-height: 1.5;
}

.left-footer {
  margin-top: auto;
}
.footer-divider {
  width: 40px;
  height: 2px;
  background: rgba(255,255,255,0.4);
  border-radius: 2px;
  margin-bottom: 16px;
}
.footer-quote {
  color: rgba(255,255,255,0.85);
  font-size: 13px;
  font-style: italic;
  margin: 0 0 14px;
  letter-spacing: 1px;
}
.footer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.footer-tags span {
  padding: 4px 14px;
  border-radius: 20px;
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(4px);
  color: rgba(255,255,255,0.9);
  font-size: 12px;
  font-weight: 500;
}

/* ===== Right Side ===== */
.auth-right {
  flex: 1;
  padding: 24px 44px 40px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow-y: auto;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 20px;
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.close-btn:hover {
  background: #f5f0f7;
  color: var(--text);
}

.form-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  margin: 8px 0 6px;
}
.form-subtitle {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0 0 28px;
}

/* Form */
.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
.auth-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: none;
  border: 1.5px solid var(--border);
  padding: 4px 14px;
  height: 46px;
  transition: border-color 0.2s;
}
.auth-form :deep(.el-input__wrapper:hover) {
  border-color: #e0b0d0;
}
.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--pink);
  box-shadow: 0 0 0 3px rgba(255,107,157,0.1);
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.captcha-row :deep(.el-input__wrapper) {
  flex: 1;
}
.captcha-img {
  height: 46px;
  width: 120px;
  border-radius: 12px;
  cursor: pointer;
  border: 1.5px solid var(--border);
  flex-shrink: 0;
  transition: border-color 0.2s;
}
.captcha-img:hover {
  border-color: var(--pink);
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: none;
  box-shadow: 0 4px 20px rgba(255,107,157,0.3);
  margin-top: 8px;
}
.submit-btn:hover {
  box-shadow: 0 6px 28px rgba(255,107,157,0.4);
}

.form-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-dim);
}
.form-footer a {
  color: var(--pink);
  cursor: pointer;
  font-weight: 600;
  text-decoration: none;
}
.form-footer a:hover {
  text-decoration: underline;
}

/* Animation */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .auth-modal {
    flex-direction: column;
    height: auto;
    max-height: 95vh;
  }
  .auth-left {
    width: 100%;
    padding: 32px 28px;
  }
  .features { display: none; }
  .brand { margin-bottom: 0; }
  .auth-right {
    padding: 28px 24px;
  }
}
</style>
