// auth.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of, tap } from 'rxjs';
export interface RegisterRequest

 { username: string; 
  email: string; 
  password: string;
 }
interface LoginRequest {
  email: string;
  password: string;
}

interface LoginResponse {
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);

  private readonly apiUrl = '/api';



  register(request: RegisterRequest): Observable<void> {

    return this.http.post<void>
      (`${this.apiUrl}/register`, request);
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {
          localStorage.setItem('access_token', response.token);
        })
      );
  }

 logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('userId');
  }
   isAuthenticated(): Observable<boolean> {
    const token = localStorage.getItem('access_token');

    if (!token) {
      return of(false);
    }

    return this.http.get('/api/me').pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  getToken(): string | null {
    return localStorage.getItem('access_token');
  }
}