export interface NavItem {
  label: string;
  path: string;
  icon: 'patients' | 'templates' | 'more';
}

/** Destinations available today; more items arrive with their stories (H1.2, H1.4, H1.5, É2). */
export const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Pacientes', path: '/pacientes', icon: 'patients' },
  { label: 'Plantillas', path: '/plantillas', icon: 'templates' },
];

/** Mobile tab bar: the main destinations plus "Más" (account menu; on desktop it lives in the side nav). */
export const TAB_ITEMS: readonly NavItem[] = [...NAV_ITEMS, { label: 'Más', path: '/mas', icon: 'more' }];
