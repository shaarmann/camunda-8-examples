package com.camunda.consulting.services;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreditCardService {

  public void chargeCreditCard(String cardNumber, String cvc, String expiryDate, Double openAmount) {
    Log.info("Charging " + openAmount + "€ from credit card " + cardNumber);
  }
}
