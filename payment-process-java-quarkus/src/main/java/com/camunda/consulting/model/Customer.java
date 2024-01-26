package com.camunda.consulting.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Customer {

  @Id
  private String customerId;
  private String cardNumber;
  private String cvc;
  private String expiryDate;

  @OneToMany(cascade = CascadeType.PERSIST)
  private List<OrderDetails> orderDetailsList;
}
