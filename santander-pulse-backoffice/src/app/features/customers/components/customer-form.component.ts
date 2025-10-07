import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UiButtonComponent, UiInputComponent, UiSelectComponent, SelectOption } from 'ui-lib';
import { cpfValidator } from '../../../shared/validators/cpf.validator';
import { phoneBrValidator } from '../../../shared/validators/phone-br.validator';
import { CpfMaskDirective } from '../../../shared/directives/cpf-mask.directive';
import { PhoneMaskDirective } from '../../../shared/directives/phone-mask.directive';
import { Customer, CustomerCreate } from '../models/customer.model';
import { CustomerService } from '../services/customer.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-customer-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    UiButtonComponent,
    UiInputComponent,
    UiSelectComponent,
    CpfMaskDirective,
    PhoneMaskDirective
  ],
  template: `
    <form [formGroup]="customerForm" (ngSubmit)="onSubmit()" class="santander-form">
      
      <!-- Seção: Informações Pessoais -->
      <div class="form-section">
        <h3 class="section-title">
          Informações Pessoais
        </h3>
        
        <div class="form-grid">
          <div class="form-group full-width">
            <ui-input
              formControlName="nome"
              label="Nome Completo"
              placeholder="Digite o nome completo do cliente"
              autocomplete="name"
            ></ui-input>
            <span class="helper-text">Nome como consta no documento de identidade</span>
          </div>

          <div class="form-group">
            <ui-input
              formControlName="cpf"
              label="CPF"
              placeholder="000.000.000-00"
              mask="cpf"
              autocomplete="off"
            ></ui-input>
          </div>

          <div class="form-group">
            <ui-select
              [control]="statusControl"
              label="Status"
              placeholder="Selecione o status"
              [options]="statusOptions"
            ></ui-select>
            <span class="helper-text">Status atual do cliente no sistema</span>
          </div>
        </div>
      </div>

      <!-- Seção: Contato -->
      <div class="form-section">
        <h3 class="section-title">
          Informações de Contato
        </h3>
        
        <div class="form-grid">
          <div class="form-group">
            <ui-input
              formControlName="telefone"
              label="Telefone"
              placeholder="(11) 99999-9999"
              mask="phone"
              autocomplete="tel"
            ></ui-input>
          </div>

          <div class="form-group">
            <ui-input
              formControlName="email"
              type="email"
              label="E-mail"
              placeholder="email@exemplo.com"
              autocomplete="email"
            ></ui-input>
          </div>
        </div>
      </div>

      <!-- Footer com Ações -->
      <div class="form-footer">
        <p class="required-info">
          <span class="required">*</span> Campos obrigatórios
        </p>
        <div class="form-actions">
          <ui-button type="button" variant="secondary" (click)="onCancel()">
            Cancelar
          </ui-button>
          <ui-button type="submit" variant="success" [disabled]="isSubmitDisabled()">
            {{ loading ? 'Salvando...' : (customer ? 'Atualizar Cliente' : 'Criar Cliente') }}
          </ui-button>
        </div>
      </div>
    </form>
  `,
  styles: [`
    .santander-form {
      animation: slideIn 0.3s ease-out;
      max-height: 70vh;
      overflow-y: auto;
      padding: 0 4px;
    }

    @keyframes slideIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* Seções do formulário */
    .form-section {
      background: #F9FAFB;
      border-radius: 12px;
      padding: 24px;
      margin-bottom: 20px;
      border: 1px solid #E5E7EB;
    }

    .section-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0 0 20px 0;
      color: #111827;
      font-size: 18px;
      font-weight: 600;
      padding-bottom: 16px;
      border-bottom: 2px solid #D1D5DB;
    }

    .section-icon {
      font-size: 24px;
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 20px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 8px;

      &.full-width {
        grid-column: 1 / -1;
      }
    }

    .form-label {
      font-size: 14px;
      font-weight: 600;
      color: #374151;
      margin-bottom: 4px;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .required {
      color: #DC2626;
      font-weight: 700;
    }

    .form-group ui-input,
    .form-group ui-select {
      width: 100%;
      display: block;
    }

    .helper-text {
      font-size: 13px;
      color: #6B7280;
      margin-top: 4px;
      font-style: italic;
    }

    .form-footer {
      border-top: 2px solid #E5E7EB;
      padding-top: 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 20px;
      background: #FFFFFF;
      position: sticky;
      bottom: 0;
      padding-bottom: 16px;
    }

    .required-info {
      color: #4B5563;
      font-size: 14px;
      margin: 0;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .form-actions {
      display: flex;
      gap: 16px;
    }

    /* Scrollbar customizada */
    .santander-form::-webkit-scrollbar {
      width: 8px;
    }

    .santander-form::-webkit-scrollbar-track {
      background: #F3F4F6;
      border-radius: 4px;
    }

    .santander-form::-webkit-scrollbar-thumb {
      background: #9CA3AF;
      border-radius: 4px;

      &:hover {
        background: #6B7280;
      }
    }

    /* Responsive */
    @media (max-width: 768px) {
      .form-grid {
        grid-template-columns: 1fr;
      }

      .form-section {
        padding: 16px;
      }

      .form-footer {
        flex-direction: column;
        gap: 16px;
        align-items: stretch;
      }

      .form-actions {
        width: 100%;
      }
    }
  `]
})
export class CustomerFormComponent implements OnInit {
  @Input() customer: Customer | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  customerForm: FormGroup;
  loading = false;
  adding = false;
  statusOptions: SelectOption[] = [
    { value: 'ATIVO', label: 'Ativo' },
    { value: 'INATIVO', label: 'Inativo' }
  ];

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private notification: NotificationService
  ) {
    this.customerForm = this.fb.group({
      nome: ['', Validators.required],
      cpf: ['', [Validators.required, cpfValidator]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.required, phoneBrValidator]],
      status: ['ATIVO', Validators.required]
    });
  }

  get statusControl(): FormControl {
    return this.customerForm.get('status') as FormControl;
  }

  ngOnInit(): void {
    if (this.customer) {
      this.customerForm.patchValue(this.customer);
    }
    // Garantir que o usuário está autenticado
    this.ensureAuthenticated();
  }

  onSubmit(): void {
    if (this.customerForm.invalid) {
      // Marcar todos os campos como touched para mostrar erros
      Object.keys(this.customerForm.controls).forEach(key => {
        this.customerForm.get(key)?.markAsTouched();
      });
      this.notification.error('Por favor, corrija os erros no formulário');
      return;
    }

    this.isSubmitting = true;
    const customerData: CustomerCreate = {
      nome: this.customerForm.value.nome,
      cpf: this.customerForm.value.cpf,
      email: this.customerForm.value.email,
      telefone: this.customerForm.value.telefone,
      status: this.customerForm.value.status
    };

    console.log('Enviando dados do cliente:', customerData);

    const operation = this.customer 
      ? this.customerService.update(this.customer.id, customerData)
      : this.customerService.create(customerData);

    operation.subscribe({
      next: (result) => {
        console.log('Cliente salvo com sucesso:', result);
        this.notification.success(
          this.customer ? 'Cliente atualizado com sucesso!' : 'Cliente criado com sucesso!'
        );
        this.customerSaved.emit();
        this.resetForm();
        this.isSubmitting = false;
      },
      error: (error) => {
        console.error('Erro ao salvar cliente:', error);
        this.notification.error('Erro ao salvar cliente: ' + (error.error?.message || error.message));
        this.isSubmitting = false;
      }
    });
  }

    const operation = this.customer
      ? this.customerService.update(this.customer.id as number, customerData)
      : this.customerService.create(customerData);

    operation.subscribe({
      next: (response) => {
        this.notification.success(`Cliente ${action === 'create' ? 'criado' : 'atualizado'} com sucesso!`);
        this.saved.emit();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erro na operação:', error);
        
        let errorMessage = `Erro ao ${action === 'create' ? 'criar' : 'atualizar'} cliente`;
        
        if (error.status === 400 && error.error?.fields) {
          // Erro de validação - mapear erros para o formulário
          Object.keys(error.error.fields).forEach(field => {
            const control = this.customerForm.get(field);
            if (control) {
              control.setErrors({ serverError: error.error.fields[field] });
            }
          });
          errorMessage = 'Dados inválidos. Verifique os campos destacados.';
        } else if (error.status === 409) {
          errorMessage = 'CPF ou e-mail já cadastrado';
        } else if (error.status === 401) {
          errorMessage = 'Sessão expirada. Faça login novamente.';
        }
        
        this.notification.error(errorMessage);
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }

  isSubmitDisabled(): boolean {
    return this.loading || this.customerForm.invalid;
  }

}

