export type CatalogSlug = 'body-segments' | 'surgery-types' | 'scar-types';

export interface CatalogItem {
  id: number;
  name: string;
  active: boolean;
}

export interface CatalogDefinition {
  slug: CatalogSlug;
  /** Value of the `?tipo=` query param. */
  key: string;
  tabLabel: string;
  addLabel: string;
}

/** Tab order follows the mockup. */
export const CATALOGS: readonly CatalogDefinition[] = [
  { slug: 'body-segments', key: 'segmentos', tabLabel: 'Segmentos', addLabel: 'Nuevo segmento' },
  { slug: 'surgery-types', key: 'cirugias', tabLabel: 'Cirugías', addLabel: 'Nuevo tipo de cirugía' },
  { slug: 'scar-types', key: 'cicatrices', tabLabel: 'Cicatrices', addLabel: 'Nuevo tipo de cicatriz' },
];

export const CATALOG_ERROR_CODES = {
  duplicate: 'CATALOG_ITEM_DUPLICATE',
} as const;
