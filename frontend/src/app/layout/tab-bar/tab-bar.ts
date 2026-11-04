import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TAB_ITEMS } from '../nav-items';

@Component({
  selector: 'app-tab-bar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './tab-bar.html',
  styleUrl: './tab-bar.scss',
})
export class TabBar {
  protected readonly items = TAB_ITEMS;
}
