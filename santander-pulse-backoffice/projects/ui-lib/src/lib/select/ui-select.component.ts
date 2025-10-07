import { Component, Input, Output, EventEmitter, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormControl, ReactiveFormsModule } from '@angular/forms';

export interface SelectOption {
  value: any;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'ui-select',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ui-select-container">
      <label *ngIf="label" [for]="id" class="ui-label">{{ label }}</label>
      <select
        [id]="id"
        [disabled]="disabled"
        [class]="'ui-select' + (hasError ? ' ui-select-error' : '')"
        [value]="value"
        (change)="onChange($event)"
        (blur)="onBlur()"
      >
        <option *ngIf="placeholder" value="" disabled>{{ placeholder }}</option>
        <option 
          *ngFor="let option of options"
          [value]="option.value"
          [disabled]="option.disabled"
        >
          {{ option.label }}
        </option>
      </select>
      <div *ngIf="hasError && errorMessage" class="ui-error-message">
        {{ errorMessage }}
      </div>
    </div>
  `,
  styles: [`
    .ui-select-container {
      margin-bottom: 16px;
    }
    
    .ui-label {
      display: block;
      margin-bottom: 4px;
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }
    
    .ui-select {
      width: 100%;
      padding: 8px 12px;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 14px;
      font-family: Arial, sans-serif;
      background-color: white;
      transition: border-color 0.2s ease;
      box-sizing: border-box;
    }
    
    .ui-select:focus {
      outline: none;
      border-color: #007bff;
      box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
    }
    
    .ui-select:disabled {
      background-color: #f8f9fa;
      color: #6c757d;
      cursor: not-allowed;
    }
    
    .ui-select-error {
      border-color: #dc3545;
    }
    
    .ui-select-error:focus {
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
      useExisting: forwardRef(() => UiSelectComponent),
      multi: true
    }
  ]
})
export class UiSelectComponent implements ControlValueAccessor {
  @Input() control?: FormControl;
  @Input() id: string = '';
  @Input() label: string = '';
  @Input() placeholder: string = '';
  @Input() disabled: boolean = false;
  @Input() hasError: boolean = false;
  @Input() errorMessage: string = '';
  @Input() options: SelectOption[] = [];
  
  @Output() selectionChange = new EventEmitter<any>();

  value: any = '';
  
  private onChangeCallback = (value: any) => {};
  private onTouchedCallback = () => {};

  get showError(): boolean {
    if (this.control) {
      return this.control.invalid && this.control.touched;
    }
    return this.hasError;
  }

  getErrorMessage(): string {
    if (this.control && this.control.errors) {
      const errors = this.control.errors;
      if (errors['required']) return 'Campo obrigatorio';
      return this.errorMessage || 'Campo invalido';
    }
    return this.errorMessage;
  }

  onChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.value = target.value;
    this.onChangeCallback(this.value);
    this.selectionChange.emit(this.value);
  }

  onBlur(): void {
    this.onTouchedCallback();
  }

  writeValue(value: any): void {
    this.value = value || '';
  }

  registerOnChange(fn: (value: any) => void): void {
    this.onChangeCallback = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouchedCallback = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}