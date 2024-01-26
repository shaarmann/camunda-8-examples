package com.camunda.consulting.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CustomerServiceTest {

  @Test
  public void testGetCustomerCredit() {
    CustomerService service = new CustomerService();
    assertEquals(10, service.getCustomerCredit("customer10"));
  }

  @Test
  public void getCustomerCreditFailsWithInvalidID() {
    CustomerService service = new CustomerService();
    assertThrows(NumberFormatException.class ,() -> service.getCustomerCredit("customer"));
  }

}
