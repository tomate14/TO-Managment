import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { CatalogItem } from '../../catalogs/catalogs.models';
import { TemplateSummary } from '../templates.models';
import { TemplatesService } from '../templates.service';

const LOAD_ERROR = 'No pudimos cargar las plantillas. Intentá de nuevo en unos minutos.';

/** H3.1 list of templates with a segment filter. */
@Component({
  selector: 'app-templates-page',
  imports: [RouterLink],
  templateUrl: './templates-page.html',
  styleUrl: './templates-page.scss',
})
export class TemplatesPage implements OnInit {
  private readonly templatesService = inject(TemplatesService);
  private readonly catalogsService = inject(CatalogsService);

  protected readonly templates = signal<TemplateSummary[] | null>(null);
  protected readonly segments = signal<CatalogItem[]>([]);
  /** null = "Todos". */
  protected readonly segmentId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly visible = computed(() => {
    const list = this.templates() ?? [];
    const segmentId = this.segmentId();
    return segmentId === null ? list : list.filter((template) => template.segment.id === segmentId);
  });

  ngOnInit(): void {
    this.templatesService.list().subscribe({
      next: (templates) => this.templates.set(templates),
      error: () => this.error.set(LOAD_ERROR),
    });
    this.catalogsService.list('body-segments', true).subscribe({
      next: (segments) => this.segments.set(segments),
      error: () => this.segments.set([]),
    });
  }

  protected filter(segmentId: number | null): void {
    this.segmentId.set(segmentId);
  }
}
