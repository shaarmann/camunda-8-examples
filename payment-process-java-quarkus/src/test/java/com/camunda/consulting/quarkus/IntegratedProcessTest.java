package com.camunda.consulting.quarkus;

import com.camunda.consulting.services.CreditCardService;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.quarkiverse.zeebe.test.InjectZeebeClient;
import io.quarkiverse.zeebe.test.ZeebeTestEmbeddedResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(ZeebeTestEmbeddedResource.class)
public class IntegratedProcessTest {
  @InjectZeebeClient
  ZeebeClient zbClient;
  ZeebeTestEngine engine;
  CreditCardService creditCardService;

  @BeforeEach
  public void setUpEngine() {
    engine = ZeebeTestEmbeddedResource.ZEEBE_ENGINE;
  }

  @BeforeEach
  public void setUpSpies() {
    creditCardService = Mockito.spy(CreditCardService.class);
    QuarkusMock.installMockForType(creditCardService, CreditCardService.class);
  }
  @Test
  public void testSufficientCustomerCredit() throws InterruptedException, TimeoutException {
    Map<String, Object> variables = Map.of(
        "customerId", "customer99",
        "orderId", "order1",
        "orderTotal", 90,
        "cardNumber", "123457890",
        "cvc", "123",
        "expiryDate", "12/99"
    );
    zbClient.newDeployResourceCommand()
        .addResourceFromClasspath("bpmn/order_process.bpmn")
        .send()
        .join();
    var pi = zbClient.newCreateInstanceCommand()
        .bpmnProcessId("Process_Order")
        .latestVersion()
        .variables(variables)
        .send()
        .join();
    var assertPi = BpmnAssert.assertThat(pi);
    await().atMost(Duration.ofSeconds(7)).untilAsserted(assertPi::isCompleted);
    Mockito.verify(creditCardService, times(0)).chargeCreditCard(anyString(), anyString(), anyString(), anyDouble());
  }
  @Test
  public void testInsufficientCustomerCredit() throws InterruptedException, TimeoutException {
    Map<String, Object> variables = Map.of(
        "customerId", "customer10",
        "orderId", "order1",
        "orderTotal", 90,
        "cardNumber", "123457890",
        "cvc", "123",
        "expiryDate", "12/99"
    );
    zbClient.newDeployResourceCommand()
        .addResourceFromClasspath("bpmn/order_process.bpmn")
        .send()
        .join();
    var pi = zbClient.newCreateInstanceCommand()
        .bpmnProcessId("Process_Order")
        .latestVersion()
        .variables(variables)
        .send()
        .join();
    var assertPi = BpmnAssert.assertThat(pi);
    await().atMost(Duration.ofSeconds(7)).untilAsserted(assertPi::isCompleted);
    Mockito.verify(creditCardService, times(1)).chargeCreditCard(anyString(), anyString(), anyString(), anyDouble());
  }
}
