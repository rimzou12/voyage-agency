import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from './core/auth.service';
import { I18nService, Lang } from './core/i18n.service';
import { ChatWidget } from './chat-widget/chat-widget';

@Component({
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule, ChatWidget],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly currentYear = new Date().getFullYear();

  constructor() {
    effect(() => {
      if (this.auth.expired()) {
        this.auth.acknowledgeExpiry();
        this.snackBar.open(this.i18n.t('nav.sessionExpired'), undefined, { duration: 5000 });
        this.router.navigateByUrl('/');
      }
    });
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }

  protected setLang(lang: Lang): void {
    this.i18n.setLang(lang);
  }
}
