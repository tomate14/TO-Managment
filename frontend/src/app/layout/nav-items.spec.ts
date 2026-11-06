import { NAV_ITEMS, SUPERADMIN_NAV_ITEMS, SUPERADMIN_TAB_ITEMS } from './nav-items';

describe('NAV_ITEMS', () => {
  it('adds Agenda between Pacientes and Plantillas', () => {
    expect(NAV_ITEMS.map((i) => i.label)).toEqual(['Pacientes', 'Agenda', 'Plantillas']);
  });
});

describe('SUPERADMIN_NAV_ITEMS', () => {
  it('only offers Establecimientos, plus Más in the tab bar', () => {
    expect(SUPERADMIN_NAV_ITEMS.map((i) => i.label)).toEqual(['Establecimientos']);
    expect(SUPERADMIN_NAV_ITEMS[0].icon).toBe('establishment');
    expect(SUPERADMIN_TAB_ITEMS.map((i) => i.label)).toEqual(['Establecimientos', 'Más']);
  });
});
