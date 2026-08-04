<template>
  <div ref="rootRef" class="recent-account-search" :class="{ 'is-open': panelOpen, 'is-disabled': disabled }">
    <div
      class="recent-account-search-box"
      role="combobox"
      :aria-expanded="panelOpen"
      aria-haspopup="listbox"
      @click="focusInput"
    >
      <input
        ref="inputRef"
        :value="searchText"
        :placeholder="placeholder"
        :disabled="disabled"
        autocomplete="off"
        @focus="openPanel"
        @input="handleSearchInput"
        @keydown.escape.stop="closePanel"
      />
    </div>

    <div v-if="panelOpen && !disabled" class="recent-account-panel">
      <section class="recent-account-suggestions" aria-label="最近账号">
        <div v-if="filteredProfiles.length" class="recent-account-list" role="listbox">
          <div
            v-for="profile in filteredProfiles"
            :key="profile.key"
            class="recent-account-option"
            :class="{ active: profile.key === modelValue }"
            role="option"
            :aria-selected="profile.key === modelValue"
            @mousedown.prevent="selectProfile(profile)"
          >
            <span class="recent-account-option-label">{{ formatLoginProfile(profile) }}</span>
            <button
              class="recent-account-option-delete"
              type="button"
              :aria-label="removeLabel"
              :title="removeLabel"
              @mousedown.stop.prevent="removeProfile(profile.key)"
            >
              <X :size="15" />
            </button>
          </div>
        </div>
        <p v-else class="recent-account-empty">{{ emptyLabel }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { X } from 'lucide-vue-next';
import type { LoginProfile } from '@/utils/loginProfiles';

const props = withDefaults(defineProps<{
  modelValue: string;
  profiles: LoginProfile[];
  placeholder?: string;
  disabled?: boolean;
  removeLabel?: string;
  emptyLabel?: string;
}>(), {
  placeholder: '',
  disabled: false,
  removeLabel: '删除当前记录',
  emptyLabel: '未找到匹配账号',
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'select': [profileKey: string];
  'remove': [profileKey: string];
}>();

const rootRef = ref<HTMLElement | null>(null);
const inputRef = ref<HTMLInputElement | null>(null);
const panelOpen = ref(false);
const searchText = ref('');
const searchActive = ref(false);

const selectedProfile = computed(() => props.profiles.find((profile) => profile.key === props.modelValue));
const normalizedSearch = computed(() => searchText.value.trim().toLowerCase());
const filteredProfiles = computed(() => {
  if (!searchActive.value || !normalizedSearch.value) {
    return props.profiles;
  }

  return props.profiles.filter((profile) => formatLoginProfile(profile).toLowerCase().includes(normalizedSearch.value));
});

watch(
  () => [props.modelValue, props.profiles] as const,
  () => {
    searchText.value = selectedProfile.value ? formatLoginProfile(selectedProfile.value) : '';
  },
  { immediate: true },
);

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentMouseDown);
});

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentMouseDown);
});

function focusInput() {
  if (props.disabled) {
    return;
  }
  inputRef.value?.focus();
  openPanel();
}

function openPanel() {
  if (!props.disabled) {
    panelOpen.value = true;
  }
}

function closePanel() {
  panelOpen.value = false;
  searchActive.value = false;
  searchText.value = selectedProfile.value ? formatLoginProfile(selectedProfile.value) : '';
}

function handleSearchInput(event: Event) {
  const target = event.target as HTMLInputElement;
  searchText.value = target.value;
  searchActive.value = true;
  panelOpen.value = true;
  if (props.modelValue) {
    emit('update:modelValue', '');
  }
}

function selectProfile(profile: LoginProfile) {
  searchText.value = formatLoginProfile(profile);
  searchActive.value = false;
  emit('update:modelValue', profile.key);
  emit('select', profile.key);
  panelOpen.value = false;
}

function removeProfile(profileKey: string) {
  emit('remove', profileKey);
  if (props.modelValue === profileKey) {
    searchText.value = '';
    emit('update:modelValue', '');
  }
  openPanel();
}

function formatLoginProfile(profile: LoginProfile) {
  return `${profile.tenantCode} / ${profile.username}`;
}

function handleDocumentMouseDown(event: MouseEvent) {
  if (!rootRef.value?.contains(event.target as Node)) {
    closePanel();
  }
}
</script>

<style scoped>
.recent-account-search {
  position: relative;
  width: 100%;
}

.recent-account-search-box {
  min-height: 54px;
  border: 1px solid rgba(209, 213, 219, 0.9);
  border-radius: 999px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: center;
  padding: 0 18px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
  transition: border-color 120ms ease, box-shadow 120ms ease;
}

.recent-account-search.is-open .recent-account-search-box,
.recent-account-search-box:focus-within {
  border-color: rgba(209, 213, 219, 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.1);
}

.recent-account-search.is-open .recent-account-search-box {
  border-bottom-color: rgba(229, 231, 235, 0.9);
  border-radius: 24px 24px 0 0;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.1);
}

.recent-account-search.is-disabled {
  opacity: 0.72;
}

.recent-account-search-box input {
  width: 100%;
  height: 50px;
  min-width: 0;
  border: 0 !important;
  border-radius: 0;
  padding: 0 !important;
  color: var(--xuan-text);
  background: transparent;
  font-size: 15px;
  font-weight: 600;
  outline: none;
  box-shadow: none !important;
}

.recent-account-search-box input:focus {
  border: 0 !important;
  outline: none;
  box-shadow: none !important;
}

.recent-account-search-box input::placeholder {
  color: var(--xuan-muted);
  font-weight: 500;
}

.recent-account-panel {
  position: absolute;
  z-index: 20;
  top: calc(100% - 1px);
  left: 0;
  right: 0;
  min-height: 120px;
  border: 1px solid rgba(209, 213, 219, 0.96);
  border-top: 0;
  border-radius: 0 0 24px 24px;
  padding: 10px 0 12px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.12);
}

.recent-account-list {
  display: grid;
  gap: 4px;
  padding: 0 10px;
}

.recent-account-option {
  min-width: 0;
  border: 0;
  border-radius: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  color: var(--xuan-text);
  background: transparent;
  font-size: 14px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition: background 180ms ease, color 180ms ease;
}

.recent-account-option:hover,
.recent-account-option.active {
  background: rgba(243, 244, 246, 0.95);
}

.recent-account-option-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-account-option-delete {
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 999px;
  display: grid;
  place-items: center;
  padding: 0;
  color: var(--xuan-muted);
  background: transparent;
  cursor: pointer;
  transition: background 180ms ease, color 180ms ease;
}

.recent-account-option-delete:hover {
  color: var(--xuan-text);
  background: rgba(229, 231, 235, 0.95);
}

.recent-account-empty {
  margin: 10px 16px;
  color: var(--xuan-muted);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 640px) {
  .recent-account-panel {
    border-radius: 0 0 20px 20px;
  }
}
</style>
