export const AUTH_SESSION_REFRESHED_EVENT = 'xuan-auth-session-refreshed';

export type FrameworkSessionPayload = object;

export function emitAuthSessionRefreshed<TSession extends FrameworkSessionPayload = FrameworkSessionPayload>(session: TSession) {
  window.dispatchEvent(new CustomEvent<TSession>(AUTH_SESSION_REFRESHED_EVENT, {
    detail: session,
  }));
}

export function listenAuthSessionRefreshed<TSession extends FrameworkSessionPayload = FrameworkSessionPayload>(
  handler: (session: TSession) => void | Promise<void>,
) {
  const listener = (event: Event) => {
    const session = (event as CustomEvent<TSession>).detail;
    if (session) {
      void handler(session);
    }
  };
  window.addEventListener(AUTH_SESSION_REFRESHED_EVENT, listener);
  return () => window.removeEventListener(AUTH_SESSION_REFRESHED_EVENT, listener);
}
