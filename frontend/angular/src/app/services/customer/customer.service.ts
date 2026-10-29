import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, tap } from 'rxjs';
import { CustomerDTO } from '../../models/customer-dto';
import { environment } from '../../../environments/environment';
import { CustomerRegistrationRequest } from '../../models/customer-registration-request';
import { CustomerUpdateRequest } from '../../models/customer-update-request';
import { AuthenticationResponse } from '../../models/authentication-response';
import { PageResponse } from '../../models/page-response';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {

  private readonly customerUrl = `${environment.api.baseUrl}/${environment.api.customerUrl}`;

  constructor(
    private http: HttpClient
  ) { }

  // The API returns the list in pages. This client has no paging UI, so it shows
  // the first 100 customers, the largest page the API allows.
  findAll(): Observable<CustomerDTO[]> {
    return this.http.get<PageResponse<CustomerDTO>>(this.customerUrl, { params: { size: 100 } })
    .pipe(
      map(page => page.content)
    );
  }

  registerCustomer(customer: CustomerRegistrationRequest): Observable<void> {
    return this.http.post<void>(this.customerUrl, customer);
  }

  deleteCustomer(id: number | undefined): Observable<void> {
    return this.http.delete<void>(`${this.customerUrl}/${id}`);
  }

  updateCustomer(id: number | undefined, customer: CustomerUpdateRequest): Observable<void> {
    return this.http.put<void>(`${this.customerUrl}/${id}`, customer, { observe: 'response' })
    .pipe(
      tap(response => this.storeRefreshedToken(response.headers.get('Authorization'), customer.email)),
      map(() => undefined)
    );
  }

  // Users who change their own email get a new token, because the old one names
  // the old email. Admins editing someone else get no token and keep theirs.
  private storeRefreshedToken(token: string | null, email: string | undefined) {
    const storedUser = localStorage.getItem('user');
    if (!token || !storedUser) {
      return;
    }
    const authResponse: AuthenticationResponse = JSON.parse(storedUser);
    authResponse.token = token;
    if (email) {
      authResponse.customerDTO.email = email;
      authResponse.customerDTO.username = email;
    }
    localStorage.setItem('user', JSON.stringify(authResponse));
  }
}
