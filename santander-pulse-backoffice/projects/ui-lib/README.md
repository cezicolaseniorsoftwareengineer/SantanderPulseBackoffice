# Santander Pulse UI Library

## Overview

Enterprise-grade Angular component library for the Santander Pulse banking platform. This library provides reusable, accessible, and banking-compliant UI components following Santander's design system standards.

## Components

### Form Components
- **PulseInputComponent**: Banking-specific input fields with validation
- **PulseButtonComponent**: Consistent button styling and interactions
- **PulseSelectComponent**: Dropdown selections with accessibility support

### Validation Components
- **CPF Validator**: Brazilian CPF document validation
- **Banking Form Validators**: Financial data validation suite

### Layout Components
- **PulseTableComponent**: Data tables with sorting and pagination
- **PulseCardComponent**: Content containers with consistent styling

## Installation

```bash
ng build ui-lib
```

## Usage

Import the UI library in your Angular module:

```typescript
import { PulseUILibModule } from 'ui-lib';

@NgModule({
  imports: [PulseUILibModule],
  // ...
})
export class YourModule { }
```

## Development

### Code Generation
```bash
ng generate component component-name --project ui-lib
ng generate directive|pipe|service|class|guard|interface|enum|module --project ui-lib
```

### Testing
```bash
ng test ui-lib
```

### Building
```bash
ng build ui-lib
```

## Design System Compliance

All components follow Santander's design system guidelines:
- Consistent spacing and typography
- Accessibility standards (WCAG 2.1 AA)
- Banking-specific color palette
- Responsive design patterns

## Banking Standards

Components implement banking industry requirements:
- Brazilian document validation (CPF/CNPJ)
- Financial data formatting
- Security-focused form handling
- Audit trail compatibility

---

**Santander Pulse Engineering Guild**  
*Frontend Architecture Team*
