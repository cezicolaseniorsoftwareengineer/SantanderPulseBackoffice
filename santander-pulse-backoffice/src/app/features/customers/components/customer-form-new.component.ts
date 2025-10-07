import { Component, Input, Output, E        <!-- Header seguindo padrã          <fieldset class="form-section">
            <legend class="section-title">
              <span class="section-icon" aria-hidden="true"></span>
              Informações Pessoais
            </legend>composição -->
        <div class="form-header">
          <h2 class="form-title">
            <span class="form-icon" aria-hidden="true"></span>
            {{ getFormTitle() }}
          </h2>
          <button 
            type="button" 
            class="close-btn" 
            (click)="handleCancelAction()"
            aria-label="Fechar formulário"
          >
            <span aria-hidden="true">&times;</span>
          </button>
        </div>Init } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { cpfValidator } from '../../../shared/validators/cpf.validator';
import { phoneBrValidator } from '../../../shared/validators/phone-br.validator';
import { CpfMaskDirective } from '../../../shared/directives/cpf-mask.directive';
import { PhoneMaskDirective } from '../../../shared/directives/phone-mask.directive';
import { Customer, CustomerCreate } from '../models/customer.model';
import { CustomerService } from '../services/customer.service';
import { NotificationService } from '../../../core/services/notification.service';

/**
 * CustomerFormComponent
 * 
 * Responsabilidade única: Gerenciar o formulário de criação/edição de clientes
 * 
 * Princípios aplicados:
 * - Single Responsibility: Apenas manipula o formulário de cliente
 * - Open/Closed: Extensível através de validadores customizados
 * - Dependency Inversion: Depende de abstrações (services injetados)
 * - Interface Segregation: Eventos específicos para cada ação
 */
@Component({
  selector: 'app-customer-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CpfMaskDirective,
    PhoneMaskDirective
  ],
  template: `
    <!-- Modal overlay com padrão Strategy para fechamento -->
    <div class="santander-form-overlay" (click)="handleOverlayClick()">
      <div class="santander-form-container" (click)="$event.stopPropagation()">
        
        <!-- Header seguindo padrão de composição -->
        <div class="form-header">
          <h2 class="form-title">
            <span class="form-icon" aria-hidden="true">�</span>
            {{ getFormTitle() }}
          </h2>
          <button 
            type="button" 
            class="close-btn" 
            (click)="handleCancelAction()"
            aria-label="Fechar formulário"
          >
            <span aria-hidden="true">&times;</span>
          </button>
        </div>

        <!-- Form content com validação reativa -->
        <form [formGroup]="customerForm" (ngSubmit)="handleFormSubmission()" class="santander-form">
          
          <!-- Seção de dados pessoais - cohesão alta -->
          <fieldset class="form-section">
            <legend class="section-title">
              <span class="section-icon" aria-hidden="true">�</span>
              Informações Pessoais
            </legend>
            
            <div class="form-grid">
              <div class="form-group full-width">
                <label class="form-label required" for="customer-name">Nome Completo</label>
                <input 
                  id="customer-name"
                  type="text"
                  formControlName="nome"
                  class="form-input"
                  placeholder="Digite o nome completo do cliente"
                  autocomplete="name"
                  [attr.aria-invalid]="isFieldInvalid('nome')"
                  [attr.aria-describedby]="isFieldInvalid('nome') ? 'name-error' : null"
                />
                <div class="helper-text">Nome como consta no documento de identidade</div>
                <div 
                  *ngIf="isFieldInvalid('nome')" 
                  class="error-message"
                  id="name-error"
                  role="alert"
                >
                  Nome é obrigatório
                </div>
              </div>

              <div class="form-group">
                <label class="form-label required" for="customer-cpf">CPF</label>
                <input 
                  id="customer-cpf"
                  type="text"
                  formControlName="cpf"
                  class="form-input"
                  placeholder="000.000.000-00"
                  cpfMask
                  autocomplete="off"
                  [attr.aria-invalid]="isFieldInvalid('cpf')"
                  [attr.aria-describedby]="isFieldInvalid('cpf') ? 'cpf-error' : null"
                />
                <div class="helper-text">Formatação automática durante digitação</div>
                <div 
                  *ngIf="isFieldInvalid('cpf')" 
                  class="error-message"
                  id="cpf-error"
                  role="alert"
                >
                  <span *ngIf="getFieldError('cpf', 'required')">CPF é obrigatório</span>
                  <span *ngIf="getFieldError('cpf', 'cpf')">CPF inválido</span>
                </div>
              </div>
            </div>
          </fieldset>

          <!-- Seção de contato - responsabilidade isolada -->
          <fieldset class="form-section">
            <legend class="section-title">
              <span class="section-icon" aria-hidden="true"></span>
              Informações de Contato
            </legend>
            
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label required" for="customer-email">E-mail</label>
                <input 
                  id="customer-email"
                  type="email"
                  formControlName="email"
                  class="form-input"
                  placeholder="cliente@exemplo.com"
                  autocomplete="email"
                  [attr.aria-invalid]="isFieldInvalid('email')"
                  [attr.aria-describedby]="isFieldInvalid('email') ? 'email-error' : null"
                />
                <div class="helper-text">E-mail principal para comunicações</div>
                <div 
                  *ngIf="isFieldInvalid('email')" 
                  class="error-message"
                  id="email-error"
                  role="alert"
                >
                  <span *ngIf="getFieldError('email', 'required')">E-mail é obrigatório</span>
                  <span *ngIf="getFieldError('email', 'email')">E-mail inválido</span>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label required" for="customer-phone">Telefone</label>
                <input 
                  id="customer-phone"
                  type="tel"
                  formControlName="telefone"
                  class="form-input"
                  placeholder="(11) 98765-4321"
                  phoneMask
                  autocomplete="tel"
                  [attr.aria-invalid]="isFieldInvalid('telefone')"
                  [attr.aria-describedby]="isFieldInvalid('telefone') ? 'phone-error' : null"
                />
                <div class="helper-text">Celular ou telefone fixo</div>
                <div 
                  *ngIf="isFieldInvalid('telefone')" 
                  class="error-message"
                  id="phone-error"
                  role="alert"
                >
                  <span *ngIf="getFieldError('telefone', 'required')">Telefone é obrigatório</span>
                  <span *ngIf="getFieldError('telefone', 'phone')">Telefone inválido</span>
                </div>
              </div>
            </div>
          </fieldset>

          <!-- Seção de status - domain logic isolada -->
          <fieldset class="form-section">
            <legend class="section-title">
              <span class="section-icon" aria-hidden="true"></span>
              Status do Cliente
            </legend>
            
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label required" for="customer-status">Status</label>
                <select 
                  id="customer-status"
                  formControlName="status" 
                  class="form-select"
                  [attr.aria-invalid]="isFieldInvalid('status')"
                >
                  <option value="ATIVO">Ativo</option>
                  <option value="INATIVO">Inativo</option>
                </select>
                <div class="helper-text">Define se o cliente está ativo no sistema</div>
              </div>
            </div>
          </fieldset>

          <!-- Actions com Command pattern implícito -->
          <div class="form-actions">
            <button 
              type="button" 
              class="btn btn-secondary" 
              (click)="handleCancelAction()" 
              [disabled]="isSubmitting"
            >
              <span class="btn-icon" aria-hidden="true"></span>
              Cancelar
            </button>
            
            <button 
              type="submit" 
              class="btn btn-primary" 
              [disabled]="!canSubmitForm()"
            >
              <span class="btn-icon" *ngIf="!isSubmitting" aria-hidden="true"></span>
              <span class="btn-icon loading" *ngIf="isSubmitting" aria-hidden="true"></span>
              {{ getSubmitButtonText() }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    /* ====== OVERLAY & CONTAINER ====== */
    .santander-form-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.75);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      backdrop-filter: blur(3px);
    }

    .santander-form-container {
      background: white;
      border-radius: 16px;
      box-shadow: 0 24px 48px rgba(0, 0, 0, 0.2);
      max-width: 800px;
      width: 90vw;
      max-height: 90vh;
      overflow-y: auto;
      animation: modalEnter 0.3s ease-out;
    }

    @keyframes modalEnter {
      from {
        opacity: 0;
        transform: scale(0.9) translateY(-20px);
      }
      to {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
    }

    /* ====== HEADER ====== */
    .form-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 24px 32px;
      border-bottom: 2px solid #f1f3f4;
      background: linear-gradient(135deg, #ec0000 0%, #d40000 100%);
      color: white;
      border-radius: 16px 16px 0 0;
    }

    .form-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0;
      font-size: 24px;
      font-weight: 700;
    }

    .close-btn {
      background: rgba(255, 255, 255, 0.2);
      border: none;
      color: white;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 24px;
      font-weight: bold;
      transition: all 0.2s ease;
    }

    .close-btn:hover {
      background: rgba(255, 255, 255, 0.3);
      transform: scale(1.1);
    }

    /* ====== FORM CONTENT ====== */
    .santander-form {
      padding: 32px;
    }

    .form-section {
      margin-bottom: 32px;
    }

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
      color: #2d3748;
      margin: 0 0 20px 0;
      padding-bottom: 8px;
      border-bottom: 2px solid #e2e8f0;
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      align-items: start;
    }

    .form-group.full-width {
      grid-column: 1 / -1;
    }

    .form-group {
      display: flex;
      flex-direction: column;
    }

    .form-label {
      font-size: 14px;
      font-weight: 600;
      color: #2d3748;
      margin-bottom: 6px;
      display: flex;
      align-items: center;
    }

    .form-label.required::after {
      content: ' *';
      color: #e53e3e;
      font-weight: bold;
    }

    .form-input,
    .form-select {
      padding: 12px 16px;
      border: 2px solid #e2e8f0;
      border-radius: 8px;
      font-size: 16px;
      transition: all 0.2s ease;
      background: white;
    }

    .form-input:focus,
    .form-select:focus {
      outline: none;
      border-color: #ec0000;
      box-shadow: 0 0 0 3px rgba(236, 0, 0, 0.1);
    }

    .form-input.ng-invalid.ng-touched,
    .form-select.ng-invalid.ng-touched {
      border-color: #e53e3e;
      box-shadow: 0 0 0 3px rgba(229, 62, 62, 0.1);
    }

    .helper-text {
      font-size: 12px;
      color: #718096;
      margin-top: 4px;
    }

    .error-message {
      font-size: 12px;
      color: #e53e3e;
      margin-top: 4px;
      font-weight: 500;
    }

    /* ====== ACTIONS ====== */
    .form-actions {
      display: flex;
      gap: 16px;
      justify-content: flex-end;
      padding-top: 24px;
      border-top: 1px solid #e2e8f0;
      margin-top: 32px;
    }

    .btn {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 24px;
      border: none;
      border-radius: 8px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      min-width: 140px;
      justify-content: center;
    }

    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      transform: none !important;
    }

    .btn-secondary {
      background: #f7fafc;
      color: #4a5568;
      border: 2px solid #e2e8f0;
    }

    .btn-secondary:hover:not(:disabled) {
      background: #edf2f7;
      transform: translateY(-1px);
    }

    .btn-primary {
      background: linear-gradient(135deg, #ec0000 0%, #d40000 100%);
      color: white;
      box-shadow: 0 4px 8px rgba(236, 0, 0, 0.3);
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 12px rgba(236, 0, 0, 0.4);
    }

    /* ====== RESPONSIVE ====== */
    @media (max-width: 768px) {
      .santander-form-container {
        width: 95vw;
        margin: 20px;
      }

      .form-header,
      .santander-form {
        padding: 16px;
      }

      .form-grid {
        grid-template-columns: 1fr;
        gap: 16px;
      }

      .form-actions {
        flex-direction: column;
      }

      .btn {
        width: 100%;
      }
    }
  `]
})
export class CustomerFormComponent implements OnInit {
  // Interface segregation - inputs/outputs específicos
  @Input() customer: Customer | null = null;
  @Output() customerSaved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  // State management - cohesão alta
  customerForm: FormGroup;
  isSubmitting = false;
  isEditing = false;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly customerService: CustomerService,
    private readonly notificationService: NotificationService
  ) {
    this.customerForm = this.createCustomerForm();
  }

  ngOnInit(): void {
    this.initializeFormState();
  }

  // Template helper methods - presentation logic
  getFormTitle(): string {
    return this.isEditing ? 'Editar Cliente' : 'Novo Cliente';
  }

  getSubmitButtonText(): string {
    if (this.isSubmitting) return 'Salvando...';
    return this.isEditing ? 'Atualizar' : 'Criar Cliente';
  }

  canSubmitForm(): boolean {
    return this.customerForm.valid && !this.isSubmitting;
  }

  // Validation helper methods - single responsibility
  isFieldInvalid(fieldName: string): boolean {
    const field = this.customerForm.get(fieldName);
    return !!(field?.errors && field?.touched);
  }

  getFieldError(fieldName: string, errorType: string): boolean {
    const field = this.customerForm.get(fieldName);
    return !!(field?.errors?.[errorType] && field?.touched);
  }

  // Event handlers - command pattern
  handleOverlayClick(): void {
    this.executeCancel();
  }

  handleCancelAction(): void {
    this.executeCancel();
  }

  handleFormSubmission(): void {
    this.executeSubmit();
  }

  // Private implementation methods - encapsulation
  private createCustomerForm(): FormGroup {
    return this.formBuilder.group({
      nome: ['', Validators.required],
      cpf: ['', [Validators.required, cpfValidator]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.required, phoneBrValidator]],
      status: ['ATIVO', Validators.required]
    });
  }

  private initializeFormState(): void {
    if (this.customer) {
      this.isEditing = true;
      this.populateFormWithCustomerData();
    }
  }

  private populateFormWithCustomerData(): void {
    if (!this.customer) return;
    
    this.customerForm.patchValue({
      nome: this.customer.nome,
      cpf: this.customer.cpf,
      email: this.customer.email,
      telefone: this.customer.telefone,
      status: this.customer.status
    });
  }

  private executeCancel(): void {
    this.cancelled.emit();
  }

  private executeSubmit(): void {
    if (!this.validateFormBeforeSubmission()) return;

    this.isSubmitting = true;
    const customerData = this.extractCustomerDataFromForm();
    
    this.performCustomerOperation(customerData)
      .subscribe({
        next: () => this.handleSuccessfulSubmission(),
        error: (error) => this.handleSubmissionError(error)
      });
  }

  private validateFormBeforeSubmission(): boolean {
    if (this.customerForm.invalid) {
      this.markAllFieldsAsTouched();
      this.notificationService.error('Por favor, corrija os erros no formulário');
      return false;
    }
    return true;
  }

  private markAllFieldsAsTouched(): void {
    Object.keys(this.customerForm.controls).forEach(key => {
      this.customerForm.get(key)?.markAsTouched();
    });
  }

  private extractCustomerDataFromForm(): CustomerCreate {
    const formValues = this.customerForm.value;
    return {
      nome: formValues.nome,
      cpf: formValues.cpf,
      email: formValues.email,
      telefone: formValues.telefone,
      status: formValues.status
    };
  }

  private performCustomerOperation(customerData: CustomerCreate) {
    return this.customer 
      ? this.customerService.update(this.customer.id as number, customerData)
      : this.customerService.create(customerData);
  }

  private handleSuccessfulSubmission(): void {
    const successMessage = this.isEditing 
      ? 'Cliente atualizado com sucesso!' 
      : 'Cliente criado com sucesso!';
    
    this.notificationService.success(successMessage);
    this.customerSaved.emit();
    this.isSubmitting = false;
  }

  private handleSubmissionError(error: any): void {
    const errorMessage = this.determineErrorMessage(error);
    this.processValidationErrors(error);
    this.notificationService.error(errorMessage);
    this.isSubmitting = false;
  }

  private determineErrorMessage(error: any): string {
    if (error.status === 400 && error.error?.fields) {
      return 'Dados inválidos. Verifique os campos destacados.';
    }
    if (error.status === 409) {
      return 'CPF ou e-mail já cadastrado';
    }
    if (error.error?.message) {
      return error.error.message;
    }
    return 'Erro ao salvar cliente';
  }

  private processValidationErrors(error: any): void {
    if (error.status === 400 && error.error?.fields) {
      Object.keys(error.error.fields).forEach(field => {
        const control = this.customerForm.get(field);
        if (control) {
          control.setErrors({ serverError: error.error.fields[field] });
        }
      });
    }
  }
}