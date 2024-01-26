package com.camunda.consulting.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order {
  private String orderId;
  private String customerId;
  private String cardNumber;
  private String cvc;
  private String expiryDate;
  private Double orderTotal;
}
