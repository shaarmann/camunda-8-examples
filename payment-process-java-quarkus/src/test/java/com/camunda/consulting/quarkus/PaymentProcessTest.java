package com.camunda.consulting.quarkus;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.quarkiverse.zeebe.test.InjectZeebeClient;
import io.quarkiverse.zeebe.test.ZeebeTestEmbeddedResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(ZeebeTestEmbeddedResource.class)
public class PaymentProcessTest {
  @InjectZeebeClient
  ZeebeClient zbClient;

  @Test
  public void testHappyPath() throws InterruptedException, TimeoutException {
    Map<String, Object> variables = Map.of(
        "customerId", "customer99",
        "orderId", "order1",
        "orderTotal", 90,
        "cardNumber", "123457890",
        "cvc", "123",
        "expiryDate", "12/99"
    );
    var publishMessageResponse = zbClient.newPublishMessageCommand()
        .messageName("InvokePaymentMsg")
        .correlationKey("")
        .variables(variables)
        .send()
        .join();
    var assertThatMsg = BpmnAssert.assertThat(publishMessageResponse);
    await().atMost(Duration.ofSeconds(90)).untilAsserted(assertThatMsg::hasCreatedProcessInstance);
    var piAssert = BpmnAssert.assertThat(publishMessageResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance();
   await().untilAsserted(piAssert::isCompleted);
    piAssert
        .hasPassedElement("Activity_0mx6apu")
        .hasNotPassedElement("Activity_1ryrfq4")
        .isCompleted();
  }
}
