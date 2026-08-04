<template>
  <main class="login-page">
    <section class="login-visual" :aria-label="appFrameworkConfig.shell.appName">
      <RouterLink class="login-brand" :to="appFrameworkConfig.routes.loginPath">
        <span class="login-brand-mark">X</span>
        <span>{{ appFrameworkConfig.shell.appName }}</span>
      </RouterLink>

      <div class="login-people-wrap">
        <AnimatedPeople
          :is-typing="isTyping"
          :show-password="showPassword"
          :password-length="form.password.length"
        />
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
          <span>{{ appFrameworkConfig.shell.appName }}</span>
        </div>

        <div class="login-title">
          <h1>{{ t('login.title') }}</h1>
          <p>{{ t('login.subtitle') }}</p>
        </div>

        <form class="login-form" @submit.prevent="submitLogin">
          <div v-if="loginProfiles.length" class="login-field">
            <span>{{ t('login.recentAccount') }}</span>
            <RecentAccountSearchSelect
              v-model="selectedHistoryKey"
              :profiles="loginProfiles"
              :placeholder="t('login.recentAccountPlaceholder')"
              :disabled="loading"
              :remove-label="t('login.removeCurrentAccount')"
              :empty-label="t('login.recentAccountEmpty')"
              @select="applyLoginProfileSelection"
              @remove="removeLoginProfileItem"
            />
            <div class="login-history-actions">
              <span class="login-history-spacer" />
              <div class="login-history-links">
                <button
                  class="login-history-link"
                  type="button"
                  :disabled="loading || !loginProfiles.length"
                  @click="clearAllLoginProfiles"
                >
                  {{ t('login.clearHistory') }}
                </button>
              </div>
            </div>
          </div>

          <label class="login-field">
            <span>{{ t('login.tenantCode') }}</span>
            <input
              ref="tenantInputRef"
              v-model.trim="form.tenantCode"
              :disabled="loading"
              autocomplete="off"
              :placeholder="t('login.tenantPlaceholder')"
              @focus="startTyping"
              @blur="stopTyping"
            />
          </label>

          <label class="login-field">
            <span>{{ t('login.username') }}</span>
            <input
              ref="usernameInputRef"
              v-model.trim="form.username"
              :disabled="loading"
              autocomplete="username"
              :placeholder="t('login.usernamePlaceholder')"
              @focus="startTyping"
              @blur="stopTyping"
            />
          </label>

          <label class="login-field">
            <span>{{ t('login.password') }}</span>
            <span class="password-wrap">
              <input
                ref="passwordInputRef"
                v-model="form.password"
                class="password-input"
                :type="showPassword ? 'text' : 'password'"
                :disabled="loading"
                autocomplete="current-password"
                placeholder="••••••••"
                @focus="startTyping"
                @blur="handlePasswordBlur"
                @keydown="updateCapsLockState"
                @keyup="updateCapsLockState"
              />
              <button
                class="password-toggle"
                type="button"
                aria-label="toggle password"
                :disabled="loading"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" :size="22" />
                <Eye v-else :size="22" />
              </button>
            </span>
            <p v-if="capsLockOn" class="login-field-note caps-lock-warning">{{ t('login.capsLockOn') }}</p>
          </label>

          <div class="login-meta-block">
            <div class="login-meta-row">
              <label class="remember-password">
                <input v-model="rememberPassword" :disabled="loading" type="checkbox" />
                <span>{{ t('login.rememberPassword') }}</span>
              </label>
              <button class="login-link-button" type="button" :disabled="loading">{{ t('login.contactAdmin') }}</button>
            </div>
            <p class="remember-password-hint">{{ t('login.rememberPasswordHint') }}</p>
          </div>

          <button class="login-action" type="submit" :disabled="loading">
            <span class="idle">{{ loading ? t('login.submitting') : t('login.submit') }}</span>
            <span class="hover">
              {{ loading ? t('login.submitting') : t('login.submit') }}
              <ArrowRight :size="18" />
            </span>
          </button>
        </form>

        <button class="login-action tenant-action" type="button" :disabled="loading">
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
import { isAxiosError } from 'axios';
import { nextTick, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus/es/components/message/index';
import { ElMessageBox } from 'element-plus/es/components/message-box/index';
import { ArrowRight, Eye, EyeOff } from 'lucide-vue-next';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import AnimatedPeople from '@/components/login/AnimatedPeople.vue';
import RecentAccountSearchSelect from '@/components/login/RecentAccountSearchSelect.vue';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';
import {
  clearLoginProfiles,
  deleteLoginProfile,
  findLoginProfile,
  loadLoginProfiles,
  saveLoginProfile,
} from '@/utils/loginProfiles';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const loginProfiles = ref(loadLoginProfiles());
const initialProfile = loginProfiles.value[0];

const loading = ref(false);
const showPassword = ref(false);
const isTyping = ref(false);
const capsLockOn = ref(false);
const selectedHistoryKey = ref(initialProfile?.key || '');
const rememberPassword = ref(Boolean(initialProfile?.password));
const tenantInputRef = ref<HTMLInputElement | null>(null);
const usernameInputRef = ref<HTMLInputElement | null>(null);
const passwordInputRef = ref<HTMLInputElement | null>(null);
const form = reactive({
  tenantCode: initialProfile?.tenantCode || '',
  username: initialProfile?.username || '',
  password: initialProfile?.password || '',
});

function startTyping() {
  isTyping.value = true;
}

function stopTyping() {
  isTyping.value = false;
}

function refreshLoginProfiles(preferredKey = '') {
  loginProfiles.value = loadLoginProfiles();
  selectedHistoryKey.value = preferredKey && loginProfiles.value.some((profile) => profile.key === preferredKey)
    ? preferredKey
    : '';
}

async function focusPreferredField() {
  await nextTick();
  if (!form.tenantCode) {
    tenantInputRef.value?.focus();
    return;
  }
  if (!form.username) {
    usernameInputRef.value?.focus();
    return;
  }
  passwordInputRef.value?.focus();
}

function applyLoginProfileSelection(profileKey: string) {
  const profile = findLoginProfile(loginProfiles.value, profileKey);
  if (!profile) {
    return;
  }

  form.tenantCode = profile.tenantCode;
  form.username = profile.username;
  form.password = profile.password || '';
  rememberPassword.value = Boolean(profile.password);
  void focusPreferredField();
}

function updateCapsLockState(event: KeyboardEvent) {
  capsLockOn.value = event.getModifierState('CapsLock');
}

function handlePasswordBlur() {
  capsLockOn.value = false;
  stopTyping();
}

function removeLoginProfileItem(profileKey: string) {
  const isCurrentProfile = selectedHistoryKey.value === profileKey;
  deleteLoginProfile(profileKey);
  refreshLoginProfiles(isCurrentProfile ? '' : selectedHistoryKey.value);
  if (isCurrentProfile) {
    rememberPassword.value = false;
  }
  ElMessage.success(t('login.currentRecordDeleted'));
}

async function clearAllLoginProfiles() {
  try {
    await ElMessageBox.confirm(
      t('login.clearHistoryMessage'),
      t('login.clearHistoryTitle'),
      {
        type: 'warning',
        confirmButtonText: t('login.clearHistory'),
        cancelButtonText: t('common.cancel'),
      },
    );
  } catch {
    return;
  }
  clearLoginProfiles();
  refreshLoginProfiles();
  rememberPassword.value = false;
  ElMessage.success(t('login.historyCleared'));
}

async function submitLogin() {
  if (!form.tenantCode || !form.username || !form.password) {
    ElMessage.warning(t('login.required'));
    return;
  }

  loading.value = true;
  try {
    await authStore.login({
      tenantCode: form.tenantCode,
      username: form.username,
      password: form.password,
    });
    const profile = saveLoginProfile({
      tenantCode: form.tenantCode,
      username: form.username,
      password: rememberPassword.value ? form.password : undefined,
    });
    refreshLoginProfiles(profile.key);
    await authorizationStore.loadPermissionSnapshot();
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : appFrameworkConfig.routes.homePath;
    await router.push(redirect);
  } catch (error) {
    if (!isAxiosError(error)) {
      ElMessage.error(t('login.failed'));
    }
    await focusPreferredField();
  } finally {
    loading.value = false;
  }
}
</script>
