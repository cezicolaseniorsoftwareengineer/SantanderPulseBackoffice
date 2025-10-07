import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ui-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button 
      [type]="type"
      [disabled]="disabled"
      [class]="'ui-btn ui-btn-' + variant + (disabled ? ' ui-btn-disabled' : '')"
      (click)="handleClick($event)">
      <ng-content></ng-content>
    </button>
  `,
  styles: [`
    .ui-btn {
      padding: 12px 24px;
      border-radius: 8px;
      border: 2px solid transparent;
      cursor: pointer;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
      font-size: 15px;
      font-weight: 600;
      transition: all 0.2s ease;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    
    .ui-btn:hover:not(.ui-btn-disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
    }

    .ui-btn:active:not(.ui-btn-disabled) {
      transform: translateY(0);
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    }
    
    .ui-btn-primary {
      background: linear-gradient(135deg, #EC0000 0%, #C40000 100%);
      border-color: #EC0000;
      color: white;
    }
    
    .ui-btn-primary:hover:not(.ui-btn-disabled) {
      background: linear-gradient(135deg, #D10000 0%, #B00000 100%);
      border-color: #D10000;
      box-shadow: 0 4px 12px rgba(236, 0, 0, 0.3);
    }
    
    .ui-btn-secondary {
      background: white;
      border-color: #E0E0E0;
      color: #424242;
    }
    
    .ui-btn-secondary:hover:not(.ui-btn-disabled) {
      background: #F5F5F5;
      border-color: #BDBDBD;
    }
    
    .ui-btn-danger {
      background: linear-gradient(135deg, #DC3545 0%, #C82333 100%);
      border-color: #DC3545;
      color: white;
    }
    
    .ui-btn-danger:hover:not(.ui-btn-disabled) {
      background: linear-gradient(135deg, #C82333 0%, #BD2130 100%);
      border-color: #C82333;
      box-shadow: 0 4px 12px rgba(220, 53, 69, 0.3);
    }
    
    .ui-btn-success {
      background: linear-gradient(135deg, #28A745 0%, #218838 100%);
      border-color: #28A745;
      color: white;
    }
    
    .ui-btn-success:hover:not(.ui-btn-disabled) {
      background: linear-gradient(135deg, #218838 0%, #1E7E34 100%);
      border-color: #218838;
      box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
    }
    
    .ui-btn-disabled {
      opacity: 0.6;
      cursor: default !important;
      transform: none !important;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
      pointer-events: none;
    }
  `]
})
export class UiButtonComponent {
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() variant: 'primary' | 'secondary' | 'danger' | 'success' = 'primary';
  @Input() disabled: boolean = false;
  @Output() click = new EventEmitter<Event>();
  @Output() clicked = new EventEmitter<Event>();

  handleClick(event: Event): void {
    if (!this.disabled) {
      this.click.emit(event);
      this.clicked.emit(event);
    }
  }
}