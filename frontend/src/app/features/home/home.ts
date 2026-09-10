import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  response: any;
  constructor(private http: HttpClient ,private router: Router) {

  }

ngOnInit() {
this.checkHealth();
}
  checkHealth() {
    this.http.get('http://localhost:8080/api/health').subscribe(
      (response) => {
        this.response = response;
        console.log('Health check successful:', response);
      },
      (error) => {
        console.error('Health check failed:', error);
      }
    );
}
goToWhiteboard(){
      this.router.navigate(['/whiteboard']);
}
}