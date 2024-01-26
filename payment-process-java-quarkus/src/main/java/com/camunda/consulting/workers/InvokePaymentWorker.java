package com.camunda.consulting.workers;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.quarkiverse.zeebe.JobWorker;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InvokePaymentWorker {
  @Inject
  ZeebeClient zeebe;
  @JobWorker(name = "com.camunda.consulting.workers.InvokePaymentWorker", type = "invokePayment")
  public void invokePayment(ActivatedJob job) {
    Log.info("Invoke Payment");
    var variables = job.getVariablesAsMap();
    zeebe.newPublishMessageCommand()
        .messageName("InvokePaymentMsg")
        .correlationKey("")
        .variables(variables)
        .send()
        .join();
  }
}
