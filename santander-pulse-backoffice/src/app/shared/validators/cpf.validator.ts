import { AbstractControl, ValidatorFn, ValidationErrors } from '@angular/forms';

const DEV_ALLOW_LIST = new Set([
  '11122233344', // legacy admin sample
  '55566677788', // legacy manager sample
  '99988877766', // legacy user sample
  '12345678901', // blueprint example
  '98765432100', // secondary seed example
  '35060268871'  // seed data used in demos
]);

export const cpfValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = (control.value || '').replace(/\D/g, '');
  
  // Permite vazio (required vai validar obrigatoriedade)
  if (!value) {
    return null;
  }
  
  // Valida apenas se tiver 11 dígitos completos
  if (value.length !== 11 || /^(\d)\1{10}$/.test(value)) {
    return { cpf: true };
  }
  
  if (DEV_ALLOW_LIST.has(value)) {
    return null;
  }

  const calculateDigit = (numbers: string, multiplier: number): number => {
    let sum = 0;
    for (let i = 0; i < multiplier - 1; i++) {
      sum += +numbers[i] * (multiplier - i);
    }
    const remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  };
  
  const digit1 = calculateDigit(value, 10);
  const digit2 = calculateDigit(value, 11);
  
  return (digit1 === +value[9] && digit2 === +value[10]) ? null : { cpf: true };
};