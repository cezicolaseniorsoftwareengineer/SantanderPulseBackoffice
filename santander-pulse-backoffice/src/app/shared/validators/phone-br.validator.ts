import { AbstractControl, ValidatorFn, ValidationErrors } from '@angular/forms';

export const phoneBrValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value || '';
  
  // Permite vazio (required vai validar obrigatoriedade)
  if (!value) {
    return null;
  }
  
  // Regex para telefone brasileiro: (DD) 4|5 dígitos-4 dígitos
  const phoneRegex = /^\(\d{2}\)\s?\d{4,5}-\d{4}$/;
  
  return phoneRegex.test(value) ? null : { phoneBR: true };
};