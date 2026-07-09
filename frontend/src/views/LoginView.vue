<template>
  <main class="login-page">
    <section class="login-visual" aria-label="Xuan ERP">
      <RouterLink class="login-brand" to="/login">
        <span class="login-brand-mark">X</span>
        <span>Xuan ERP</span>
      </RouterLink>

      <div class="login-people-wrap">
        <AnimatedPeople />
      </div>

      <div class="login-legal">
        <span>Privacy Policy</span>
        <span>Terms of Service</span>
      </div>
    </section>

    <section class="login-form-side">
      <div class="login-card">
        <div class="login-mobile-brand">
          <span class="login-brand-mark">X</span>
          <span>Xuan ERP</span>
        </div>

        <div class="login-title">
          <h1>{{ t('login.title') }}</h1>
          <p>{{ t('login.subtitle') }}</p>
        </div>

        <form class="login-form" @submit.prevent="submitLogin">
          <label class="login-field">
            <span>{{ t('login.tenantId') }}</span>
            <input
              v-model.trim="form.tenantId"
              inputmode="numeric"
              autocomplete="off"
              :placeholder="t('login.tenantPlaceholder')"
            />
          </label>

          <label class="login-field">
            <span>{{ t('login.username') }}</span>
            <input
              v-model.trim="form.username"
              autocomplete="username"
              :placeholder="t('login.usernamePlaceholder')"
            />
          </label>

          <label class="login-field">
            <span>{{ t('login.password') }}</span>
            <span class="password-wrap">
              <input
                v-model="form.password"
                class="password-input"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="••••••••"
              />
              <button class="password-toggle" type="button" aria-label="toggle password" @click="showPassword = !showPassword">
                <Eye :size="22" />
              </button>
            </span>
          </label>

          <div class="login-meta-row">
            <label class="remember-tenant">
              <input v-model="rememberTenant" type="checkbox" />
              <span>{{ t('login.rememberTenant') }}</span>
            </label>
            <button class="login-link-button" type="button">{{ t('login.contactAdmin') }}</button>
          </div>

          <button class="login-action" type="submit" :disabled="loading">
            <span class="idle">{{ loading ? 'Loading...' : t('login.submit') }}</span>
            <span class="hover">
              {{ loading ? 'Loading...' : t('login.submit') }}
              <ArrowRight :size="18" />
            </span>
          </button>
        </form>

        <button class="login-action tenant-action" type="button">
          <span class="idle">{{ t('login.backendTarget') }}</span>
          <span class="hover">{{ t('login.backendHint') }}</span>
        </button>

        <p class="login-note">
          {{ t('login.accountPrefix') }}
          <strong>{{ t('login.accountAdmin') }}</strong>
          {{ t('login.accountSuffix') }}
        </p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { ArrowRight, Eye } from 'lucide-vue-next';
import AnimatedPeople from '@/components/login/AnimatedPeople.vue';
import { useAuthStore } from '@/stores/auth';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const loading = ref(false);
const showPassword = ref(false);
const rememberTenant = ref(localStorage.getItem('xuan-remember-tenant') === 'true');
const form = reactive({
  tenantId: localStorage.getItem('xuan-login-tenant-id') || '',
  username: localStorage.getItem('xuan-login-username') || '',
  password: '',
});

async function submitLogin() {
  const tenantId = Number(form.tenantId);
  if (form.tenantId === '' || Number.isNaN(tenantId) || !form.username || !form.password) {
    ElMessage.warning(t('login.required'));
    return;
  }

  loading.value = true;
  try {
    await authStore.login({
      tenantId,
      username: form.username,
      password: form.password,
    });
    if (rememberTenant.value) {
      localStorage.setItem('xuan-remember-tenant', 'true');
      localStorage.setItem('xuan-login-tenant-id', form.tenantId);
      localStorage.setItem('xuan-login-username', form.username);
    } else {
      localStorage.removeItem('xuan-remember-tenant');
      localStorage.removeItem('xuan-login-tenant-id');
      localStorage.removeItem('xuan-login-username');
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard';
    await router.push(redirect);
  } catch {
    ElMessage.error(t('login.failed'));
  } finally {
    loading.value = false;
  }
}
</script>
