import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CustomerDTO } from '../../models/customer-dto';
import { CustomerService } from '../../services/customer/customer.service';
import { CustomerRegistrationRequest } from '../../models/customer-registration-request';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-customer',
  templateUrl: './customer.component.html',
  styleUrls: ['./customer.component.scss']
})
export class CustomerComponent implements OnInit {

  display = false;
  operation: 'create' | 'update' = 'create';
  customers: Array<CustomerDTO> = [];
  customer: CustomerRegistrationRequest = {};

  constructor(
    private customerService: CustomerService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.findAllCustomers();
  }


  private findAllCustomers() {
    this.customerService.findAll()
    .subscribe({
      next: (data) => {
        this.customers = data;
        console.log(data);
      },
      error: (err: HttpErrorResponse) => this.messageService.add({
        severity: 'error',
        summary: 'Could not load customers',
        detail: err.error?.message ?? 'Something went wrong. Please try again.'
      })
    })
  }

  save(customer: CustomerRegistrationRequest) {
    if (customer) {
      if (this.operation === 'create') {
        this.customerService.registerCustomer(customer)
        .subscribe({
          next: () => {
            this.findAllCustomers();
            this.display = false;
            this.customer = {};
            this.messageService.add(
              {severity:'success',
                summary: 'Customer saved',
                detail: `Customer ${customer.name} was successfully saved`
              }
            );
          },
          error: (err: HttpErrorResponse) => this.showError(err)
        });
      } else if (this.operation === 'update') {
        this.customerService.updateCustomer(customer.id, customer)
        .subscribe({
          next: () => {
            this.findAllCustomers();
            this.display = false;
            this.customer = {};
            this.messageService.add(
              {
                severity:'success',
                summary: 'Customer updated',
                detail: `Customer ${customer.name} was successfully updated`
              }
            );
          },
          error: (err: HttpErrorResponse) => this.showError(err)
        });
      }
    }
  }

  deleteCustomer(customer: CustomerDTO) {
    this.confirmationService.confirm({
      header: 'Delete customer',
      message: `Are you sure you want to delete ${customer.name}? You can\'t undo this action afterwords`,
      accept: () => {
        this.customerService.deleteCustomer(customer.id)
        .subscribe({
          next: () => {
            this.findAllCustomers();
            this.messageService.add(
              {
                severity:'success',
                summary: 'Customer deleted',
                detail: `Customer ${customer.name} was successfully deleted`
              }
            );
          },
          error: (err: HttpErrorResponse) => this.showError(err)
        });
      }
    });
  }

  // A 403 on a change means the user is neither an admin nor that customer,
  // which the API's generic "Access denied" does not explain.
  private showError(err: HttpErrorResponse) {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: err.status === 403
        ? 'You can only change your own account.'
        : err.error?.message ?? 'Something went wrong. Please try again.'
    });
  }

  updateCustomer(customerDTO: CustomerDTO) {
    this.display = true;
    this.customer = customerDTO;
    this.operation = 'update';
  }

  createCustomer() {
    this.display = true;
    this.customer = {};
    this.operation = 'create';
  }

  cancel() {
    this.display = false;
    this.customer = {};
    this.operation = 'create';
  }
}
