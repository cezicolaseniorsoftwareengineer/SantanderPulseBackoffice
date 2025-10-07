export interface Customer {
  id: string | number;
  nome: string;
  cpf: string;
  email: string;
  telefone: string;
  status: 'ATIVO' | 'INATIVO';
  createdAt?: string;
}

export interface CustomerCreate {
  nome: string;
  cpf: string;
  email: string;
  telefone: string;
  status: 'ATIVO' | 'INATIVO';
}

export interface PageResponse<T> {
  customers: T[];
  totalElements: number;
  totalPages: number;
  pageSize: number;
  currentPage: number;
}
