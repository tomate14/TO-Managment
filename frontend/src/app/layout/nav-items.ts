export interface NavItem {
  label: string;
  path: string;
  icon: 'patients' | 'templates';
}

/** Destinations available today; more items arrive with their stories (H1.2, H1.4, H1.5, É2). */
export const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Pacientes', path: '/pacientes', icon: 'patients' },
  { label: 'Plantillas', path: '/plantillas', icon: 'templates' },
];
