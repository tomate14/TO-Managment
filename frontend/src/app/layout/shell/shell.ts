import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SideNav } from '../side-nav/side-nav';
import { TabBar } from '../tab-bar/tab-bar';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, SideNav, TabBar],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell implements OnInit {
  private readonly auth = inject(AuthService);

  ngOnInit(): void {
    this.auth.refreshCurrentUser();
  }
}
