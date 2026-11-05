import { NAV_ITEMS } from './nav-items';

describe('NAV_ITEMS', () => {
  it('adds Agenda between Pacientes and Plantillas', () => {
    expect(NAV_ITEMS.map((i) => i.label)).toEqual(['Pacientes', 'Agenda', 'Plantillas']);
  });
});
