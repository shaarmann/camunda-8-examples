package com.camunda.consulting.rest;

import com.camunda.consulting.services.CustomerService;
import io.camunda.zeebe.client.ZeebeClient;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/order")
public class OrderResource {
  @Inject
  ZeebeClient zbClient;

  @Inject
  Template orderConfirmation;

  @Inject
  CustomerService service;

  @POST
  @Transactional
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance add(Order order) {
    assert order.getOrderId() != null;
    var response = zbClient.newCreateInstanceCommand()
        .bpmnProcessId("Process_Order")
        .latestVersion()
        .variables(order)
        .send()
        .join();
    String processInstanceId = String.valueOf(response.getProcessInstanceKey());
    service.addOrder(order);
    return orderConfirmation.data("orderId", processInstanceId);
  }
}