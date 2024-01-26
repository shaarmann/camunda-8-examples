package com.camunda.consulting.processtests;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.filters.RecordStream;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@ZeebeProcessTest
public class TestPaymentProcess {
  private ZeebeTestEngine engine;
  private ZeebeClient zbClient;
  private RecordStream recordStream;

  @BeforeEach
  public void deployOrderProcess() {
    zbClient.newDeployResourceCommand()
        .addResourceFromClasspath("bpmn/payment_process.bpmn")
        .send()
        .join();
  }

  @Test
  public void testHappyPath() throws InterruptedException, TimeoutException {
    Map<String, Object> variables = Map.of(
        "openAmount", 0D);
    var response = sendMessage("InvokePaymentMsg", "", variables);
    completeServiceTask("deductCredit");
    completeServiceTask("completePayment");
    BpmnAssert.assertThat(response)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElement("Activity_0mx6apu")
        .hasNotPassedElement("Activity_1ryrfq4")
        .isCompleted();
  }

  @Test
  public void testInsufficientCredit() throws InterruptedException, TimeoutException {
    Map<String, Object> variables = Map.of(
        "openAmount", 1D);
    var response = sendMessage("InvokePaymentMsg", "", variables);
    completeServiceTask("deductCredit");
    completeServiceTask("chargeCreditCard");
    completeServiceTask("completePayment");
    BpmnAssert.assertThat(response)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElement("Activity_0mx6apu")
        .hasPassedElement("Activity_1ryrfq4")
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

  private void waitForBusyState(final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForBusyState(duration);
  }
}
