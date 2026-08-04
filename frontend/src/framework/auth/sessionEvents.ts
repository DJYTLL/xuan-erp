import type { LoginResponse } from '@/types/auth';

export const AUTH_SESSION_REFRESHED_EVENT = 'xuan-auth-session-refreshed';

export function emitAuthSessionRefreshed(session: LoginResponse) {
  window.dispatchEvent(new CustomEvent<LoginResponse>(AUTH_SESSION_REFRESHED_EVENT, {
    detail: session,
  }));
}

export function listenAuthSessionRefreshed(handler: (session: LoginResponse) => void | Promise<void>) {
  const listener = (event: Event) => {
    const session = (event as CustomEvent<LoginResponse>).detail;
    if (session) {
      void handler(session);
    }
  };
  window.addEventListener(AUTH_SESSION_REFRESHED_EVENT, listener);
  return () => window.removeEventListener(AUTH_SESSION_REFRESHED_EVENT, listener);
}
