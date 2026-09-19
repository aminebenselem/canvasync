
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth-service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink
  ],
  templateUrl: './register.html'
})
export class Register {

  private authService = inject(AuthService);
  private router = inject(Router);

  username = '';
  email = '';
  password = '';

  error = '';
  loading = false;

  register(): void {

    if (this.loading) {
      return;
    }

    this.error = '';
    this.loading = true;

    this.authService.register({
      username: this.username,
      email: this.email,
      password: this.password
    }).subscribe({

      next: () => {
        this.loading = false;

        this.router.navigate(['/login']);
      },

      error: (err) => {
        this.loading = false;

        if (err.status === 409) {
          this.error = 'Username or email already exists.';
        } else if (err.status === 400) {
          this.error = 'Please check your information.';
        } else {
          this.error = 'Something went wrong. Please try again.';
        }
      }
    });
  }
}

