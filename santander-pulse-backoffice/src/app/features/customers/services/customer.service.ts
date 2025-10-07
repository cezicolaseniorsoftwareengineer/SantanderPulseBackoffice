import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Customer, CustomerCreate, PageResponse } from '../models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private baseUrl = environment.apiUrl + '/customers';

  constructor(private http: HttpClient) {}

  list(params: any = {}): Observable<PageResponse<Customer>> {
    let httpParams = new HttpParams();
    Object.keys(params).forEach(key => {
      if (params[key] !== null && params[key] !== undefined) {
        httpParams = httpParams.set(key, params[key].toString());
      }
    });
    return this.http.get<PageResponse<Customer>>(this.baseUrl, { params: httpParams });
  }

  create(customer: CustomerCreate): Observable<Customer> {
    // Mapear os dados para o formato esperado pelo backend
    const customerData = {
      nome: customer.nome,
      cpf: customer.cpf,
      email: customer.email,
      telefone: customer.telefone,
      status: customer.status
    };
    
    console.log('Enviando dados para API:', customerData);
    console.log('URL:', this.baseUrl);
    
    return this.http.post<Customer>(this.baseUrl, customerData);
  }

  update(id: number | string, customer: CustomerCreate): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, customer);
  }

  delete(id: number | string): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }

  findById(id: number | string): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }
}