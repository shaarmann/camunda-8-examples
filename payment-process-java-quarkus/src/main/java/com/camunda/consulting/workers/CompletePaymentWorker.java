package com.camunda.consulting.workers;

import io.camunda.zeebe.client.ZeebeClient;
import io.quarkiverse.zeebe.JobWorker;
import io.quarkiverse.zeebe.Variable;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CompletePaymentWorker {

  @Inject
  ZeebeClient zeebe;
  @JobWorker(name = "com.camunda.consulting.workers.CompletePaymentWorker", type = "completePayment")
  public void completePayment(@Variable String orderId) {
    Log.info("Complete Payment");
    zeebe.newPublishMessageCommand()
        .messageName("PaymentCompletedMsg")
        .correlationKey(orderId)
        .send()
        .join();
  }
}
