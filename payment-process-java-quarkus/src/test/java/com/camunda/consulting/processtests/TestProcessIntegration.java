package com.camunda.consulting.processtests;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.quarkiverse.zeebe.test.ZeebeTestEmbeddedResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;

@ZeebeProcessTest
public class TestProcessIntegration {

  private ZeebeTestEngine engine;
  private ZeebeClient zbClient;
  private RecordStream recordStream;

  @BeforeEach
  public void deployOrderProcess() {
    zbClient.newDeployResourceCommand()
        .addResourceFromClasspath("bpmn/payment_process.bpmn")
        .addResourceFromClasspath("bpmn/order_process.bpmn")
        .send()
        .join();
  }

  @Test
  public void testHappyPath() throws InterruptedException, TimeoutException {
    Map<String, Object> variables = Map.of(
        "customerId", "Cust99",
        "orderId", "order1",
        "orderTotal", 90,
        "cardNumber", "1234567890",
        "cvc", "123",
        "expiryDate", "05/24");
    zbClient.newCreateInstanceCommand()
        .bpmnProcessId("Process_Order")
        .latestVersion()
        .variables(variables)
        .withResult()
        .send();
    waitForIdleState(Duration.ofSeconds(1L));
    var activatedJobResponse = zbClient.newActivateJobsCommand()
        .jobType("invokePayment")
        .maxJobsToActivate(1)
        .send()
        .join();
    var publishInvokePaymentMsgResponse = zbClient.newPublishMessageCommand()
        .messageName("InvokePaymentMsg")
        .correlationKey("")
        .variables(variables)
        .send()
        .join();
    zbClient.newCompleteCommand(activatedJobResponse.getJobs().get(0))
        .send()
        .join();
    completeServiceTask("deductCredit");
    completeServiceTask("chargeCreditCard");
    activatedJobResponse = zbClient.newActivateJobsCommand()
        .jobType("completePayment")
        .maxJobsToActivate(1)
        .send()
        .join();
    var publishCompletePaymentMsgResponse = zbClient.newPublishMessageCommand()
        .messageName("PaymentCompletedMsg")
        .correlationKey("order1")
        .variables(variables)
        .send()
        .join();
    zbClient.newCompleteCommand(activatedJobResponse.getJobs().get(0))
        .send()
        .join();
    waitForIdleState(Duration.ofSeconds(1L));
    BpmnAssert.assertThat(publishInvokePaymentMsgResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .isCompleted();
    BpmnAssert.assertThat(publishCompletePaymentMsgResponse)
        .hasBeenCorrelated()
        .extractingProcessInstance()
        .isCompleted();
  }


  private PublishMessageResponse sendMessage(
      final String messageName, final String correlationKey, final Map<String, Object> variables)
      throws InterruptedException, TimeoutException {
    final PublishMessageResponse response =
        zbClient
            .newPublishMessageCommand()
            .messageName(messageName)
            .correlationKey(correlationKey)
            .variables(variables)
            .send()
            .join();
    waitForIdleState(Duration.ofSeconds(1));
    return response;
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

    waitForIdleState(Duration.ofSeconds(1));
  }

  private void completeUserTask(final String elementId)
      throws InterruptedException, TimeoutException {
    // user tasks can be controlled similarly to service tasks
    // all user tasks share a common job type
    final var activateJobsResponse =
        zbClient
            .newActivateJobsCommand()
            .jobType("io.camunda.zeebe:userTask")
            .maxJobsToActivate(100)
            .send()
            .join();

    boolean userTaskWasCompleted = false;

    for (final ActivatedJob userTask : activateJobsResponse.getJobs()) {
      if (userTask.getElementId().equals(elementId)) {
        // complete the user task we care about
        zbClient.newCompleteCommand(userTask).send().join();
        userTaskWasCompleted = true;
      } else {
        // fail all other user tasks that were activated
        // failing a task with a retry value >0 means the task can be reactivated in the future
        zbClient.newFailCommand(userTask).retries(Math.max(userTask.getRetries(), 1)).send().join();
      }
    }

    waitForIdleState(Duration.ofSeconds(1));

    if (!userTaskWasCompleted) {
      Assertions.fail("Tried to complete task `%s`, but it was not found".formatted(elementId));
    }
  }

  private void waitForIdleState(final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForIdleState(duration);
  }
}
