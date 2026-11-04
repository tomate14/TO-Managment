import { DestroyRef, Signal, inject, signal } from '@angular/core';

/** Same breakpoint as the SCSS (`min-width: 1024px`), where the side nav replaces the tab bar. */
export const DESKTOP_QUERY = '(min-width: 1024px)';

/**
 * Signal that follows the desktop breakpoint, for screens whose behaviour (not only layout) changes on desktop.
 * Must be called in an injection context. Without matchMedia (tests, old browsers) it stays false (mobile).
 */
export function injectIsDesktop(): Signal<boolean> {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return signal(false).asReadonly();
  }
  const media = window.matchMedia(DESKTOP_QUERY);
  const isDesktop = signal(media.matches);
  const listener = (event: MediaQueryListEvent) => isDesktop.set(event.matches);
  media.addEventListener('change', listener);
  inject(DestroyRef).onDestroy(() => media.removeEventListener('change', listener));
  return isDesktop.asReadonly();
}
