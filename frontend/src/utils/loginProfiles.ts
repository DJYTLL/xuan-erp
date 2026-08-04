export interface LoginProfile {
  key: string;
  tenantCode: string;
  username: string;
  password?: string;
  updatedAt: number;
}

const LOGIN_PROFILES_STORAGE_KEY = 'xuan-login-profiles';
const LEGACY_REMEMBER_TENANT_STORAGE_KEY = 'xuan-remember-tenant';
const LEGACY_TENANT_ID_STORAGE_KEY = 'xuan-login-tenant-id';
const LEGACY_USERNAME_STORAGE_KEY = 'xuan-login-username';
const MAX_LOGIN_PROFILES = 8;

interface SaveLoginProfileInput {
  tenantCode: string;
  username: string;
  password?: string;
}

export function loadLoginProfiles(): LoginProfile[] {
  const storage = getStorage();
  if (!storage) {
    return [];
  }

  const profiles = readProfiles(storage);
  if (profiles.length > 0) {
    return profiles;
  }

  const migratedProfile = readLegacyProfile(storage);
  if (!migratedProfile) {
    return [];
  }

  writeProfiles(storage, [migratedProfile]);
  clearLegacyProfile(storage);
  return [migratedProfile];
}

export function saveLoginProfile(input: SaveLoginProfileInput): LoginProfile {
  const storage = getStorage();
  const profile = normalizeProfile({
    tenantCode: input.tenantCode,
    username: input.username,
    password: input.password,
    updatedAt: Date.now(),
  });

  if (!storage) {
    return profile;
  }

  const nextProfiles = [
    profile,
    ...readProfiles(storage).filter((item) => item.key !== profile.key),
  ].slice(0, MAX_LOGIN_PROFILES);

  writeProfiles(storage, nextProfiles);
  clearLegacyProfile(storage);
  return profile;
}

export function findLoginProfile(profiles: LoginProfile[], key: string): LoginProfile | undefined {
  return profiles.find((profile) => profile.key === key);
}

export function deleteLoginProfile(key: string): LoginProfile[] {
  const storage = getStorage();
  if (!storage) {
    return [];
  }

  const nextProfiles = readProfiles(storage).filter((profile) => profile.key !== key);
  writeProfiles(storage, nextProfiles);
  return nextProfiles;
}

export function clearLoginProfiles() {
  const storage = getStorage();
  if (!storage) {
    return;
  }

  storage.removeItem(LOGIN_PROFILES_STORAGE_KEY);
  clearLegacyProfile(storage);
}

function readProfiles(storage: Storage): LoginProfile[] {
  const raw = storage.getItem(LOGIN_PROFILES_STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }

    return parsed
      .map((item) => normalizeProfile(item))
      .filter((item, index, source) => source.findIndex((candidate) => candidate.key === item.key) === index)
      .sort((left, right) => right.updatedAt - left.updatedAt)
      .slice(0, MAX_LOGIN_PROFILES);
  } catch {
    return [];
  }
}

function writeProfiles(storage: Storage, profiles: LoginProfile[]) {
  storage.setItem(LOGIN_PROFILES_STORAGE_KEY, JSON.stringify(profiles));
}

function readLegacyProfile(storage: Storage): LoginProfile | null {
  if (storage.getItem(LEGACY_REMEMBER_TENANT_STORAGE_KEY) !== 'true') {
    return null;
  }

  const tenantCode = normalizeText(storage.getItem(LEGACY_TENANT_ID_STORAGE_KEY));
  const username = normalizeText(storage.getItem(LEGACY_USERNAME_STORAGE_KEY));
  if (!tenantCode || !username) {
    return null;
  }

  return normalizeProfile({
    tenantCode,
    username,
    updatedAt: Date.now(),
  });
}

function clearLegacyProfile(storage: Storage) {
  storage.removeItem(LEGACY_REMEMBER_TENANT_STORAGE_KEY);
  storage.removeItem(LEGACY_TENANT_ID_STORAGE_KEY);
  storage.removeItem(LEGACY_USERNAME_STORAGE_KEY);
}

function normalizeProfile(input: Partial<LoginProfile> & { tenantCode?: string; tenantId?: string; username: string }): LoginProfile {
  const tenantCode = normalizeText(input.tenantCode ?? input.tenantId);
  const username = normalizeText(input.username);

  return {
    key: buildLoginProfileKey(tenantCode, username),
    tenantCode,
    username,
    password: normalizeText(input.password) || undefined,
    updatedAt: typeof input.updatedAt === 'number' ? input.updatedAt : 0,
  };
}

function buildLoginProfileKey(tenantCode: string, username: string): string {
  return `${tenantCode}::${username}`;
}

function normalizeText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function getStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }

  return window.localStorage;
}
