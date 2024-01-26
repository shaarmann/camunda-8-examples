package com.camunda.consulting.quarkus;

import com.camunda.consulting.workers.InvokePaymentWorker;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.quarkiverse.zeebe.test.InjectZeebeClient;
import io.quarkiverse.zeebe.test.ZeebeTestEmbeddedResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(ZeebeTestEmbeddedResource.class)
public class OrderProcessTest {
  @InjectZeebeClient
  ZeebeClient zbClient;
  ZeebeTestEngine engine;

  @BeforeEach
  public void setUpEngine() {
    engine = ZeebeTestEmbeddedResource.ZEEBE_ENGINE;
  }

  @BeforeAll
  public static void setup() {
    var mock = Mockito.mock(InvokePaymentWorker.class);
    QuarkusMock.installMockForType(mock, InvokePaymentWorker.class);
  }
  @Test
  public void testOrderProcess() throws InterruptedException, TimeoutException {
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
    zbClient.newPublishMessageCommand()
        .messageName("PaymentCompletedMsg")
        .correlationKey("order1")
        .send()
        .join();
    var assertPi = BpmnAssert.assertThat(pi);
    await().atMost(Duration.ofSeconds(7)).untilAsserted(assertPi::isCompleted);
  }

  private void completeServiceTask(final String jobType)
      throws InterruptedException, TimeoutException {
    completeServiceTasks(jobType, 1);
  }

  private void completeServiceTasks(final String jobType, final int count)
      throws InterruptedException, TimeoutException {

    final var activateJobsResponse =
        zbClient.newActivateJobsCommand().jobType(jobType).maxJobsToActivate(count).send().join();

    final int activatedJobCount = activateJobsResponse.getJobs().size();
    if (activatedJobCount < count) {
      Assertions.fail(
          "Unable to activate %d jobs, because only %d were activated."
              .formatted(count, activatedJobCount));
    }

    for (int i = 0; i < count; i++) {
      final var job = activateJobsResponse.getJobs().get(i);

      zbClient.newCompleteCommand(job.getKey()).send().join();
    }

    engine.waitForIdleState(Duration.ofSeconds(1));
  }
}
