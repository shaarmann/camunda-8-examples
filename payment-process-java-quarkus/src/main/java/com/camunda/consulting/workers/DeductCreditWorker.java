package com.camunda.consulting.workers;

import com.camunda.consulting.services.CustomerService;
import io.quarkiverse.zeebe.JobWorker;
import io.quarkiverse.zeebe.Variable;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class DeductCreditWorker {

  @Inject
  CustomerService service;

  @JobWorker(name = "com.camunda.consulting.workers.DeductCreditWorker", type = "deductCredit")
  public Map<String, Object> deductCredit(@Variable String customerId, @Variable Double orderTotal) {
    Log.info("Deducting credit.");
    double customerCredit = service.getCustomerCredit(customerId);
    double openAmount = service.deductCredit(orderTotal, customerCredit);
    return Map.of("customerCredit", customerCredit, "openAmount", openAmount);
  }
}
