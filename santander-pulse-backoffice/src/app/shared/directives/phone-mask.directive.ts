import { Directive, HostListener, ElementRef } from '@angular/core';
import { NgControl } from '@angular/forms';

@Directive({
  selector: '[phoneMask]',
  standalone: true
})
export class PhoneMaskDirective {
  constructor(
    private el: ElementRef,
    private control: NgControl
  ) {}

  @HostListener('input', ['$event'])
  onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    
    if (value.length > 11) {
      value = value.substring(0, 11);
    }
    
    if (value.length > 10) {
      value = value.replace(/(\d{2})(\d{5})(\d{0,4})/, '($1) $2-$3');
    } else if (value.length > 6) {
      value = value.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
    } else if (value.length > 2) {
      value = value.replace(/(\d{2})(\d{0,5})/, '($1) $2');
    } else if (value.length > 0) {
      value = value.replace(/(\d{0,2})/, '($1');
    }
    
    this.control.control?.setValue(value, { emitEvent: false });
    input.value = value;
  }

  @HostListener('blur')
  onBlur(): void {
    const value = this.control.control?.value;
    if (value) {
      const numbers = value.replace(/\D/g, '');
      if (numbers.length === 11) {
        const formatted = numbers.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
        this.control.control?.setValue(formatted);
      } else if (numbers.length === 10) {
        const formatted = numbers.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
        this.control.control?.setValue(formatted);
      }
    }
  }
}
