package com.camunda.consulting.services;

import com.camunda.consulting.model.Customer;
import com.camunda.consulting.model.OrderDetails;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;

@ApplicationScoped
public class CustomerService {

  @Inject
  EntityManager em;

  @Transactional
  public void addOrder(com.camunda.consulting.rest.Order order) {
    Customer customer = em.find(Customer.class, order.getCustomerId());
    if (null == customer) {
      customer = new Customer(
          order.getCustomerId(),
          order.getCardNumber(),
          order.getCvc(),
          order.getExpiryDate(),
          new ArrayList<>()
      );
    }
    OrderDetails jpaOrder = new OrderDetails(
        order.getOrderId(),
        order.getOrderTotal()
    );
    customer.getOrderDetailsList().add(jpaOrder);
    em.persist(customer);
  }

  public double getCustomerCredit(String customerId) {
    return Double.parseDouble(customerId.substring(customerId.length()-2));
  }

  public double deductCredit(double orderTotal, double customerCredit) {
    return Math.max(0, orderTotal - customerCredit);
  }
}
