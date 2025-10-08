import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CustomerService } from '../services/customer.service';
import { Customer, PageResponse } from '../models/customer.model';
import { UiTableComponent, TableColumn, TableAction } from 'ui-lib';
import { UiInputComponent } from 'ui-lib';
import { UiButtonComponent } from 'ui-lib';
import { CustomerFormComponent } from '../components/customer-form-clean.component';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-customer-list-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    UiTableComponent,
    UiInputComponent,
    UiButtonComponent,
    CustomerFormComponent
  ],
  template: `
    <div class="santander-page">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-content">
          <div class="header-title">
            <h1>Gestão de Clientes</h1>
            <p class="subtitle">Gerencie todos os clientes do Santander Pulse</p>
          </div>
          <ui-button variant="success" (click)="openNewCustomerForm()">
            <span class="btn-icon">+</span>
            Novo Cliente
          </ui-button>
        </div>
      </div>

      <!-- Statistics Cards - VERSÃO PROFISSIONAL SIMPLES -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">Total de Clientes</div>
          <div class="stat-value">{{ pageResponse?.totalElements || 0 }}</div>
        </div>
        
        <div class="stat-card">
          <div class="stat-label">Clientes Ativos</div>
          <div class="stat-value">{{ getActiveCount() }}</div>
        </div>
        
        <div class="stat-card">
          <div class="stat-label">Clientes Inativos</div>
          <div class="stat-value">{{ getInactiveCount() }}</div>
        </div>
        
        <div class="stat-card">
          <div class="stat-label">Novos (30 dias)</div>
          <div class="stat-value">{{ getNewCount() }}</div>
        </div>
      </div>

      <!-- Search & Filter Section -->
      <div class="search-section">
        <div class="search-row">
          <ui-input
            [control]="searchControl"
            placeholder="Buscar cliente..."
            label="Buscar"
          ></ui-input>
          
          <div class="filter-group">
            <label class="filter-label">Filtrar por Status:</label>
            <select class="status-filter" [(ngModel)]="statusFilter" (change)="onStatusFilterChange()">
              <option value="">Todos os clientes</option>
              <option value="ATIVO">Apenas Ativos</option>
              <option value="INATIVO">Apenas Inativos</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Customers Table -->
      <div class="table-card">
        <div class="table-header">
          <h2>Lista de Clientes</h2>
          <span class="table-count">{{ pageResponse?.totalElements || 0 }} registro(s)</span>
        </div>

        <ui-table
          [columns]="columns"
          [data]="customers"
          [actions]="actions"
          [loading]="loading"
          (actionClicked)="handleAction($event)"
          (sortChanged)="handleSort($event)"
        ></ui-table>

        <div class="table-pagination" *ngIf="pageResponse">
          <div class="pagination-info">
            <span>
              Mostrando {{ (pageResponse.currentPage * pageResponse.pageSize) + 1 }} - 
              {{ Math.min((pageResponse.currentPage + 1) * pageResponse.pageSize, pageResponse.totalElements) }}
              de {{ pageResponse.totalElements }} registros
            </span>
          </div>
          
          <div class="pagination-controls">
            <button
              class="btn-page"
              [disabled]="pageResponse.currentPage === 0"
              (click)="goToPage(0)"
              title="Primeira página"
            >
              &laquo;
            </button>
            
            <button
              class="btn-page"
              [disabled]="pageResponse.currentPage === 0"
              (click)="goToPage(pageResponse.currentPage - 1)"
            >
              ← Anterior
            </button>
            
            <div class="page-numbers">
              <span class="current-page">Página {{ pageResponse.currentPage + 1 }}</span>
              <span class="page-separator">de</span>
              <span class="total-pages">{{ pageResponse.totalPages }}</span>
            </div>
            
            <button
              class="btn-page"
              [disabled]="pageResponse.currentPage >= pageResponse.totalPages - 1"
              (click)="goToPage(pageResponse.currentPage + 1)"
            >
              Próxima →
            </button>
            
            <button
              class="btn-page"
              [disabled]="pageResponse.currentPage >= pageResponse.totalPages - 1"
              (click)="goToPage(pageResponse.totalPages - 1)"
              title="Última página"
            >
              &raquo;
            </button>
          </div>
        </div>
      </div>

      <!-- Modal Form -->
      <app-customer-form
        *ngIf="showForm"
        [customer]="editingCustomer"
        (customerSaved)="onCustomerSaved()"
        (cancelled)="closeForm()"
      ></app-customer-form>

      <!-- Modal de Confirmação de Exclusão -->
      <div class="santander-modal" *ngIf="showDeleteConfirm" (click)="cancelDelete()">
        <div class="modal-dialog modal-confirm" (click)="$event.stopPropagation()">
          <div class="modal-header modal-header-danger">
            <div class="modal-title">
              <span class="modal-icon">!</span>
              <h2>Confirmar Exclusão</h2>
            </div>
            <button class="btn-close" (click)="cancelDelete()">&times;</button>
          </div>
          
          <div class="modal-body">
            <div class="confirm-message">
              <p class="confirm-text">
                Tem certeza que deseja <strong>excluir</strong> o cliente?
              </p>
              <div class="customer-info" *ngIf="customerToDelete">
                <div class="info-row">
                  <span class="info-label">Nome:</span>
                  <span class="info-value">{{ customerToDelete.nome }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">CPF:</span>
                  <span class="info-value">{{ customerToDelete.cpf }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">E-mail:</span>
                  <span class="info-value">{{ customerToDelete.email }}</span>
                </div>
              </div>
              <p class="warning-text">
                Esta ação não pode ser desfeita!
              </p>
            </div>
          </div>

          <div class="modal-footer">
            <ui-button variant="secondary" (click)="cancelDelete()">
              Cancelar
            </ui-button>
            <ui-button variant="danger" (click)="confirmDelete()" [disabled]="deleting">
              {{ deleting ? 'Excluindo...' : 'Sim, Excluir' }}
            </ui-button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @import '../../../../assets/styles/variables';

    .santander-page {
      animation: fadeIn 0.3s ease-in;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* ============================================
       PAGE HEADER
       ============================================ */
    .page-header {
      background: $white;
      border-radius: $border-radius-lg;
      padding: $spacing-xl;
      margin-bottom: $spacing-lg;
      box-shadow: $shadow-md;
    }

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: $spacing-lg;
    }

    .header-title h1 {
      margin: 0 0 $spacing-xs 0;
      color: $gray-900;
      font-size: $font-size-3xl;
      font-weight: $font-weight-bold;
    }

    .subtitle {
      color: $gray-600;
      font-size: $font-size-base;
      margin: 0;
    }

    .btn-icon {
      margin-right: $spacing-xs;
    }

    /* ============================================
       STATISTICS CARDS - PROFISSIONAL SIMPLES
       ============================================ */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: $spacing-md;
      margin-bottom: $spacing-xl;
    }

    .stat-card {
      background: $white;
      border: 1px solid $gray-300;
      border-radius: $border-radius-md;
      padding: $spacing-lg;
      text-align: center;
      
      &:hover {
        border-color: $santander-red;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }
    }

    .stat-label {
      display: block;
      color: $gray-600;
      font-size: $font-size-sm;
      font-weight: $font-weight-semibold;
      margin-bottom: $spacing-sm;
      text-transform: uppercase;
    }

    .stat-value {
      display: block;
      color: $santander-red;
      font-size: $font-size-3xl;
      font-weight: $font-weight-bold;
    }

    /* ============================================
       SEARCH SECTION
       ============================================ */
    .search-section {
      margin-bottom: $spacing-lg;
    }

    .search-row {
      display: flex;
      gap: $spacing-lg;
      align-items: end;
      flex-wrap: wrap;
    }

    .search-bar {
      position: relative;
      max-width: 500px;
      flex: 1;
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: $spacing-xs;
      min-width: 200px;
    }

    .filter-label {
      font-size: $font-size-sm;
      font-weight: $font-weight-medium;
      color: $gray-700;
    }

    .status-filter {
      padding: $spacing-sm $spacing-md;
      border: 2px solid $gray-300;
      border-radius: $border-radius-md;
      font-size: $font-size-base;
      background: $white;
      color: $gray-700;
      cursor: pointer;
      transition: all $transition-fast;

      &:focus {
        outline: none;
        border-color: $santander-red;
        box-shadow: 0 0 0 3px rgba(236, 0, 50, 0.1);
      }

      &:hover {
        border-color: $gray-400;
      }
    }

    .search-icon {
      position: absolute;
      left: $spacing-md;
      top: 50%;
      transform: translateY(-50%);
      font-size: $font-size-lg;
      color: $gray-500;
      z-index: 1;
    }

    /* ============================================
       TABLE CARD
       ============================================ */
    .table-card {
      background: $white;
      border-radius: $border-radius-lg;
      box-shadow: $shadow-md;
      overflow: hidden;
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: $spacing-lg $spacing-xl;
      border-bottom: 2px solid $gray-200;
      background: linear-gradient(to right, $white, $gray-50);
    }

    .table-header h2 {
      margin: 0;
      color: $gray-900;
      font-size: $font-size-xl;
      font-weight: $font-weight-semibold;
    }

    .table-count {
      color: $gray-600;
      font-size: $font-size-sm;
      background: $gray-200;
      padding: $spacing-xs $spacing-md;
      border-radius: $border-radius-pill;
      font-weight: $font-weight-medium;
    }

    /* ============================================
       PAGINATION
       ============================================ */
    .table-pagination {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: $spacing-lg $spacing-xl;
      border-top: 1px solid $gray-200;
      background: $gray-50;
    }

    .pagination-info {
      color: $gray-600;
      font-size: $font-size-sm;
    }

    .pagination-controls {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
    }

    .btn-page {
      padding: $spacing-sm $spacing-md;
      background: $white;
      color: $gray-700;
      border: 1px solid $gray-300;
      border-radius: $border-radius-md;
      cursor: pointer;
      font-weight: $font-weight-medium;
      font-size: $font-size-sm;
      transition: all $transition-fast;

      &:hover:not(:disabled) {
        background: $santander-red;
        color: $white;
        border-color: $santander-red;
        transform: translateY(-2px);
        box-shadow: $shadow-sm;
      }

      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    }

    .page-numbers {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      padding: 0 $spacing-md;
      color: $gray-700;
      font-weight: $font-weight-medium;
    }

    .current-page {
      color: $santander-red;
      font-weight: $font-weight-bold;
    }

    .page-separator {
      color: $gray-500;
    }

    /* ============================================
       MODAL
       ============================================ */
    .santander-modal {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: $z-modal;
      animation: modalFadeIn 0.2s ease-out;
    }

    @keyframes modalFadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .modal-dialog {
      background: $white;
      border-radius: $border-radius-lg;
      width: 90%;
      max-width: 700px;
      max-height: 90vh;
      overflow: hidden;
      box-shadow: $shadow-xl;
      animation: modalSlideIn 0.3s ease-out;
    }

    @keyframes modalSlideIn {
      from { transform: translateY(-50px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: $spacing-xl;
      border-bottom: 2px solid $gray-200;
      background: $gradient-red;
    }

    .modal-title {
      display: flex;
      align-items: center;
      gap: $spacing-md;
    }

    .modal-icon {
      font-size: $font-size-2xl;
    }

    .modal-header h2 {
      margin: 0;
      color: $white;
      font-size: $font-size-2xl;
      font-weight: $font-weight-semibold;
    }

    .btn-close {
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: $font-size-2xl;
      color: $white;
      cursor: pointer;
      transition: all $transition-fast;

      &:hover {
        background: rgba(255, 255, 255, 0.3);
        transform: rotate(90deg);
      }
    }

    .modal-body {
      padding: $spacing-xl;
      max-height: calc(90vh - 100px);
      overflow-y: auto;
    }

    /* ============================================
       MODAL DE CONFIRMAÇÃO
       ============================================ */
    .modal-confirm {
      max-width: 500px;
    }

    .modal-header-danger {
      background: linear-gradient(135deg, #DC143C 0%, #B71C1C 100%);
    }

    .confirm-message {
      text-align: center;
    }

    .confirm-text {
      font-size: $font-size-lg;
      color: $gray-800;
      margin-bottom: $spacing-lg;
      line-height: 1.6;

      strong {
        color: $santander-red;
        font-weight: $font-weight-bold;
      }
    }

    .customer-info {
      background: $gray-50;
      border: 1px solid $gray-300;
      border-radius: $border-radius-md;
      padding: $spacing-lg;
      margin: $spacing-lg 0;
      text-align: left;
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      padding: $spacing-sm 0;
      border-bottom: 1px solid $gray-200;

      &:last-child {
        border-bottom: none;
      }
    }

    .info-label {
      font-weight: $font-weight-semibold;
      color: $gray-600;
      font-size: $font-size-sm;
    }

    .info-value {
      color: $gray-900;
      font-weight: $font-weight-medium;
      font-size: $font-size-sm;
    }

    .warning-text {
      color: $santander-red;
      font-weight: $font-weight-bold;
      font-size: $font-size-base;
      margin: $spacing-lg 0 0 0;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: $spacing-md;
      padding: $spacing-xl;
      border-top: 1px solid $gray-200;
      background: $gray-50;
    }

    /* ============================================
       RESPONSIVE
       ============================================ */
    @media (max-width: 768px) {
      .stats-grid {
        grid-template-columns: 1fr;
      }

      .header-content {
        flex-direction: column;
        align-items: flex-start;
      }

      .pagination-controls {
        flex-wrap: wrap;
      }
    }
  `]
})
export class CustomerListPage implements OnInit {
  customers: Customer[] = [];
  pageResponse: PageResponse<Customer> | null = null;
  loading = false;
  showForm = false;
  editingCustomer: Customer | null = null;
  searchControl = new FormControl('');

  // Filtro de status para resolver o problema do cliente "desaparecido"
  statusFilter: string = '';

  // Modal de confirmação de exclusão
  showDeleteConfirm = false;
  customerToDelete: Customer | null = null;
  deleting = false;

  currentPage = 0;
  pageSize = 10;
  sortField = 'createdAt';
  sortDirection = 'desc';

  // Expose Math to template
  Math = Math;

  columns: TableColumn[] = [
    { key: 'nome', label: 'Nome', sortable: true },
    { key: 'cpf', label: 'CPF', sortable: true },
    { key: 'email', label: 'Email', sortable: true },
    { key: 'telefone', label: 'Telefone', sortable: false },
    { key: 'status', label: 'Status', sortable: true },
    { key: 'actions', label: 'Ações', type: 'actions' }
  ];

  actions: TableAction[] = [
    { label: 'Editar', action: 'edit', variant: 'primary' },
    { label: 'Excluir', action: 'delete', variant: 'danger' }
  ];

  constructor(
    private customerService: CustomerService,
    private notification: NotificationService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadCustomers();

    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.currentPage = 0;
        this.loadCustomers();
      });
  }

  loadCustomers(): void {
    this.loading = true;
    const params: any = {
      page: this.currentPage,
      size: this.pageSize,
      sort: `${this.sortField},${this.sortDirection}`
    };

    const searchValue = this.searchControl.value?.trim();
    if (searchValue) {
      params.q = searchValue;
    }

    // 🎯 SOLUÇÃO: Aplicar filtro de status quando selecionado
    if (this.statusFilter) {
      params.status = this.statusFilter;
      console.log(`🔍 [STATUS FILTER] Carregando apenas clientes: ${this.statusFilter}`);
    } else {
      console.log('🔍 [STATUS FILTER] Carregando TODOS os clientes (sem filtro)');
    }

    console.log('📡 [API] Parâmetros da requisição:', params);

    this.customerService.list(params).subscribe({
      next: (response) => {
        this.pageResponse = response;
        this.customers = response.customers;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar clientes:', error);
        this.loading = false;
        this.notification.error('Erro ao carregar clientes');
      }
    });
  }

  // Método para lidar com mudança do filtro de status
  onStatusFilterChange(): void {
    console.log('🔍 [STATUS FILTER] Filtro alterado para:', this.statusFilter);
    this.currentPage = 0; // Reset para primeira página
    this.loadCustomers();
  }

  openNewCustomerForm(): void {
    console.log('openNewCustomerForm() chamado!');
    console.log('showForm antes:', this.showForm);
    this.editingCustomer = null;
    this.showForm = true;
    console.log('showForm depois:', this.showForm);
  }

  testButton(): void {
    console.log('Botão de teste clicado!');
    alert('Botão funcionando! showForm = ' + this.showForm);
    this.showForm = true;
    console.log('Forçando showForm = true');
  }

  testCreateCustomerDirect(): void {
    console.log('Testando criação direta de cliente...');

    // Primeiro fazer login
    const authService = this.notification; // Inject via component se necessário

    // Dados de teste para o cliente
    const testCustomer = {
      nome: 'Cliente Teste Direto',
      cpf: '12345678901',
      email: 'teste.direto@exemplo.com',
      telefone: '(11) 99999-9999',
      status: 'ATIVO' as const
    };

    // Fazer o POST direto
    this.customerService.create(testCustomer).subscribe({
      next: (response) => {
        console.log('Cliente criado com sucesso:', response);
        this.notification.success('Cliente criado com sucesso via API!');
        this.loadCustomers(); // Recarregar a lista
      },
      error: (error) => {
        console.error('Erro ao criar cliente:', error);
        if (error.status === 401) {
          this.notification.error('Erro de autenticação. Fazendo login...');
          this.doLoginAndRetry(testCustomer);
        } else {
          this.notification.error('Erro ao criar cliente: ' + error.message);
        }
      }
    });
  }

  doLoginAndRetry(customerData: any): void {
    // Usar AuthService para fazer login
    const authRequest = {
      cpf: '11122233344',
      password: 'admin123'
    };

    // Este seria o login, mas vou simular
    console.log('Simulando login...', authRequest);
    this.notification.error('Funcionalidade de login precisa ser implementada aqui');
  }

  closeForm(): void {
    this.showForm = false;
    this.editingCustomer = null;
  }

  onCustomerSaved(): void {
    this.closeForm();
    this.loadCustomers();
    this.notification.success('Cliente salvo com sucesso');
  }

  handleAction(event: { action: string; row: Customer }): void {
    console.log('handleAction CHAMADO');
    console.log('  Event completo:', event);
    console.log('  Ação:', event.action);
    console.log('  Cliente:', event.row);

    if (event.action === 'edit') {
      console.log('Ação EDIT detectada - abrindo formulário');
      this.editingCustomer = event.row;
      this.showForm = true;
    } else if (event.action === 'delete') {
      console.log('Ação DELETE detectada - abrindo modal de confirmação');
      this.openDeleteConfirm(event.row);
    } else {
      console.warn('Ação desconhecida:', event.action);
    }
  }

  openDeleteConfirm(customer: Customer): void {
    console.log('openDeleteConfirm chamado com:', customer);
    this.customerToDelete = customer;
    this.showDeleteConfirm = true;
    console.log('Modal deve estar visível agora. showDeleteConfirm =', this.showDeleteConfirm);
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.customerToDelete = null;
  }

  confirmDelete(): void {
    if (!this.customerToDelete || this.deleting) {
      return;
    }

    this.deleting = true;
    const customerId = typeof this.customerToDelete.id === 'string'
      ? parseInt(this.customerToDelete.id)
      : this.customerToDelete.id;

    this.customerService.delete(customerId).subscribe({
      next: (deletionResponse) => {
        // Processamento baseado na resposta estruturada do backend
        if (deletionResponse.shouldRemoveFromList) {
          // Remove imediatamente da lista local para feedback visual instantâneo
          this.customers = this.customers.filter(c => c.id !== customerId);

          // Atualiza contadores sem fazer nova requisição
          if (this.pageResponse) {
            this.pageResponse.totalElements = Math.max(0, this.pageResponse.totalElements - 1);
          }
        }

        this.notification.success(deletionResponse.message);
        this.resetDeleteState();

        // Recarregamento completo para garantir consistência
        this.loadCustomers();
      },
      error: (error) => {
        console.error('Erro na exclusão:', error);
        this.notification.error('Erro ao excluir cliente: ' + (error.error?.message || error.message));
        this.deleting = false;
      }
    });
  }

  private resetDeleteState(): void {
    this.showDeleteConfirm = false;
    this.customerToDelete = null;
    this.deleting = false;
  }

  deleteCustomer(id: number): void {
    this.customerService.delete(id).subscribe({
      next: () => {
        this.notification.success('Cliente excluido com sucesso');
        this.loadCustomers();
      }
    });
  }

  handleSort(event: { field: string; direction: 'asc' | 'desc' }): void {
    this.sortField = event.field;
    this.sortDirection = event.direction;
    this.currentPage = 0;
    this.loadCustomers();
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadCustomers();
  }

  // Statistics Helper Methods
  getActiveCount(): number {
    return this.customers.filter(c => c.status === 'ATIVO').length;
  }

  getInactiveCount(): number {
    return this.customers.filter(c => c.status === 'INATIVO').length;
  }

  getNewCount(): number {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    return this.customers.filter(c => {
      const createdAt = c.createdAt ? new Date(c.createdAt) : null;
      return createdAt && createdAt >= thirtyDaysAgo;
    }).length;
  }


}
