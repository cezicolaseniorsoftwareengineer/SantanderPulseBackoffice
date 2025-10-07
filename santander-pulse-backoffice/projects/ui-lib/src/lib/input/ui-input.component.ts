import { Component, Input, Output, EventEmitter, forwardRef, Optional, Self } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormControl, ReactiveFormsModule, NgControl } from '@angular/forms';

@Component({
  selector: 'ui-input',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="ui-input-container">
      <label *ngIf="label" [for]="id" class="ui-label">{{ label }}</label>
      <input
        *ngIf="!control; else withControl"
        [id]="id"
        [type]="type"
        [placeholder]="placeholder"
        [disabled]="disabled"
  [attr.autocomplete]="autocomplete"
        [class]="'ui-input' + (hasError ? ' ui-input-error' : '')"
        [value]="value"
        (input)="onInput($event)"
        (blur)="onBlur()"
        (focus)="onFocus()"
      />
      <ng-template #withControl>
        <input
          [id]="id"
          [type]="type"
          [placeholder]="placeholder"
          [formControl]="control!"
          [attr.autocomplete]="autocomplete"
          [class]="'ui-input' + (control!.invalid && control!.touched ? ' ui-input-error' : '')"
          (blur)="onBlur()"
          (focus)="onFocus()"
          (input)="onInput($event)"
        />
      </ng-template>
      <div *ngIf="showError" class="ui-error-message">
        {{ getErrorMessage() }}
      </div>
    </div>
  `,
  styles: [`
    .ui-input-container {
      margin-bottom: 16px;
    }
    
    .ui-label {
      display: block;
      margin-bottom: 4px;
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }
    
    .ui-input {
      width: 100%;
      padding: 8px 12px;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 14px;
      font-family: Arial, sans-serif;
      transition: border-color 0.2s ease;
      box-sizing: border-box;
    }
    
    .ui-input:focus {
      outline: none;
      border-color: #007bff;
      box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
    }
    
    .ui-input:disabled {
      background-color: #f8f9fa;
      color: #6c757d;
      cursor: not-allowed;
    }
    
    .ui-input-error {
      border-color: #dc3545;
    }
    
    .ui-input-error:focus {
      border-color: #dc3545;
      box-shadow: 0 0 0 2px rgba(220, 53, 69, 0.25);
    }
    
    .ui-error-message {
      color: #dc3545;
      font-size: 12px;
      margin-top: 4px;
    }
  `],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiInputComponent),
      multi: true
    }
  ]
})
export class UiInputComponent implements ControlValueAccessor {
  @Input() control?: FormControl;
  @Input() id: string = '';
  @Input() type: string = 'text';
  @Input() label: string = '';
  @Input() placeholder: string = '';
  @Input() disabled: boolean = false;
  @Input() hasError: boolean = false;
  @Input() errorMessage: string = '';
  @Input() mask?: 'cpf' | 'phone';
  @Input() autocomplete: string = 'off';
  
  @Output() focus = new EventEmitter<void>();
  @Output() blur = new EventEmitter<void>();

  value: string = '';
  
  private onChange = (value: string) => {};
  private onTouched = () => {};

  constructor(@Optional() @Self() private ngControl?: NgControl) {
    if (this.ngControl) {
      this.ngControl.valueAccessor = this;
    }
  }

  get showError(): boolean {
    const ctrl = this.resolveControl();
    if (ctrl) {
      return ctrl.invalid && (ctrl.touched || ctrl.dirty);
    }
    return this.hasError;
  }

  getErrorMessage(): string {
    const ctrl = this.resolveControl();
    if (ctrl && ctrl.errors) {
      const errors = ctrl.errors;
      if (errors['required']) return 'Campo obrigatorio';
      if (errors['email']) return 'Email invalido';
      if (errors['cpf']) return 'CPF invalido';
      if (errors['phone'] || errors['phoneBR']) return 'Telefone invalido';
      return this.errorMessage || 'Campo invalido';
    }
    return this.errorMessage;
  }

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    const formatted = this.applyMask(target.value);
    this.updateValue(formatted);
    target.value = formatted;
  }

  onFocus(): void {
    this.focus.emit();
  }

  onBlur(): void {
    this.onTouched();
    this.blur.emit();
  }

  writeValue(value: string): void {
    this.value = this.applyMask(value || '');
    const ctrl = this.resolveControl();
    if (ctrl && ctrl.value !== this.value) {
      ctrl.setValue(this.value, { emitEvent: false });
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  private resolveControl(): FormControl | null {
    if (this.control) {
      return this.control;
    }
    return (this.ngControl?.control as FormControl) ?? null;
  }

  private applyMask(value: string): string {
    if (!this.mask) {
      return value;
    }

    const digits = (value || '').replace(/\D/g, '');

    if (this.mask === 'cpf') {
      const trimmed = digits.substring(0, 11);
      if (trimmed.length > 9) {
        return trimmed.replace(/(\d{3})(\d{3})(\d{3})(\d{0,2})/, '$1.$2.$3-$4');
      }
      if (trimmed.length > 6) {
        return trimmed.replace(/(\d{3})(\d{3})(\d{0,3})/, '$1.$2.$3');
      }
      if (trimmed.length > 3) {
        return trimmed.replace(/(\d{3})(\d{0,3})/, '$1.$2');
      }
      return trimmed;
    }

    if (this.mask === 'phone') {
      const trimmed = digits.substring(0, 11);
      if (trimmed.length > 10) {
        return trimmed.replace(/(\d{2})(\d{5})(\d{0,4})/, '($1) $2-$3');
      }
      if (trimmed.length > 6) {
        return trimmed.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
      }
      if (trimmed.length > 2) {
        return trimmed.replace(/(\d{2})(\d{0,5})/, '($1) $2');
      }
      if (trimmed.length > 0) {
        return trimmed.replace(/(\d{0,2})/, '($1');
      }
      return trimmed;
    }

    return value;
  }

  private updateValue(value: string): void {
    const ctrl = this.resolveControl();
    if (ctrl) {
      if (ctrl.value !== value) {
        ctrl.setValue(value, { emitEvent: true });
      }
    } else {
      this.value = value;
      this.onChange(this.value);
    }
  }
}