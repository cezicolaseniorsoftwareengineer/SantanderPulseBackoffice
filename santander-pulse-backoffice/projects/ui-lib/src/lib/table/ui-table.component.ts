import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
  type?: 'text' | 'number' | 'date' | 'actions';
}

export interface TableAction {
  label: string;
  icon?: string;
  action: string;
  variant?: 'primary' | 'secondary' | 'danger';
}

@Component({
  selector: 'ui-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ui-table-container">
      <div *ngIf="loading" class="ui-table-loading">Carregando...</div>
      <table class="ui-table" *ngIf="!loading">
        <thead>
          <tr>
            <th *ngFor="let column of columns" 
                [class]="'ui-th' + (column.sortable ? ' ui-th-sortable' : '')"
                (click)="onSort(column)">
              {{ column.label }}
              <span *ngIf="column.sortable && sortColumn === column.key" class="ui-sort-indicator">
                {{ sortDirection === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of data; trackBy: trackByFn" class="ui-tr">
            <td *ngFor="let column of columns" class="ui-td">
              <ng-container [ngSwitch]="column.type">
                <ng-container *ngSwitchCase="'actions'">
                  <button 
                    *ngFor="let action of actions"
                    [class]="'ui-action-btn ui-action-btn-' + (action.variant || 'primary')"
                    (click)="onAction(action.action, row)">
                    {{ action.label }}
                  </button>
                </ng-container>
                <ng-container *ngSwitchDefault>
                  {{ getColumnValue(row, column.key) }}
                </ng-container>
              </ng-container>
            </td>
          </tr>
          <tr *ngIf="data.length === 0" class="ui-tr-empty">
            <td [attr.colspan]="columns.length" class="ui-td-empty">
              {{ emptyMessage }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .ui-table-container {
      overflow-x: auto;
    }
    
    .ui-table {
      width: 100%;
      border-collapse: collapse;
      font-family: Arial, sans-serif;
      font-size: 14px;
    }
    
    .ui-th {
      background-color: #f8f9fa;
      border: 1px solid #dee2e6;
      padding: 12px 8px;
      text-align: left;
      font-weight: 600;
      color: #495057;
    }
    
    .ui-th-sortable {
      cursor: pointer;
      user-select: none;
    }
    
    .ui-th-sortable:hover {
      background-color: #e9ecef;
    }
    
    .ui-sort-indicator {
      margin-left: 4px;
      font-size: 12px;
    }
    
    .ui-tr {
      border-bottom: 1px solid #dee2e6;
    }
    
    .ui-tr:hover {
      background-color: #f8f9fa;
    }
    
    .ui-tr-empty {
      background-color: transparent;
    }
    
    .ui-tr-empty:hover {
      background-color: transparent;
    }
    
    .ui-td {
      border: 1px solid #dee2e6;
      padding: 12px 8px;
      vertical-align: middle;
    }
    
    .ui-td-empty {
      text-align: center;
      color: #6c757d;
      font-style: italic;
      padding: 20px;
    }
    
    .ui-action-btn {
      padding: 6px 12px;
      margin-right: 8px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 18px;
      transition: all 0.2s ease;
      line-height: 1;
      
      &:last-child {
        margin-right: 0;
      }
    }
    
    .ui-action-btn-primary {
      background-color: #007bff;
      color: white;
    }
    
    .ui-action-btn-primary:hover {
      background-color: #0056b3;
      transform: scale(1.1);
    }
    
    .ui-action-btn-secondary {
      background-color: #6c757d;
      color: white;
    }
    
    .ui-action-btn-secondary:hover {
      background-color: #545b62;
      transform: scale(1.1);
    }
    
    .ui-action-btn-danger {
      background-color: #dc3545;
      color: white;
    }
    
    .ui-action-btn-danger:hover {
      background-color: #c82333;
      transform: scale(1.1);
    }
  `]
})
export class UiTableComponent {
  @Input() columns: TableColumn[] = [];
  @Input() data: any[] = [];
  @Input() actions: TableAction[] = [];
  @Input() loading: boolean = false;
  @Input() emptyMessage: string = 'Nenhum registro encontrado';
  @Input() sortColumn: string = '';
  @Input() sortDirection: 'asc' | 'desc' = 'asc';
  
  @Output() sortChanged = new EventEmitter<{field: string, direction: 'asc' | 'desc'}>();
  @Output() actionClicked = new EventEmitter<{action: string, row: any}>();

  onSort(column: TableColumn): void {
    if (!column.sortable) return;
    
    if (this.sortColumn === column.key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column.key;
      this.sortDirection = 'asc';
    }
    
    this.sortChanged.emit({
      field: this.sortColumn,
      direction: this.sortDirection
    });
  }

  onAction(action: string, row: any): void {
    this.actionClicked.emit({ action, row });
  }

  getColumnValue(row: any, key: string): any {
    return key.split('.').reduce((obj, prop) => obj?.[prop], row);
  }

  trackByFn(index: number, item: any): any {
    return item.id || index;
  }
}