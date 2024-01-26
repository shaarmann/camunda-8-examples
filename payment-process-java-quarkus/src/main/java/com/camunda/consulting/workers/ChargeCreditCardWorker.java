package com.camunda.consulting.workers;

import com.camunda.consulting.services.CreditCardService;
import io.quarkiverse.zeebe.JobWorker;
import io.quarkiverse.zeebe.Variable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChargeCreditCardWorker {

  @Inject
  CreditCardService creditCardService;

  @JobWorker(name = "com.camunda.consulting.workers.ChargeCreditCardWorker", type = "chargeCreditCard")
  public void chargeCreditCard(@Variable String cardNumber, @Variable String cvc, @Variable String expiryDate, @Variable Double openAmount) {
    creditCardService.chargeCreditCard(cardNumber,cvc,expiryDate,openAmount);
  }
}
