package com.camunda.consulting.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.quarkiverse.zeebe.test.ZeebeTestEmbeddedResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(ZeebeTestEmbeddedResource.class)
public class OrderResourceTest {

  @Test
  public void testPlaceOrder() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    Order order = new Order(
        "order1",
        "customer90",
        "123 456 7890",
        "123",
        "01/29",
        50.0d
    );

    given()
        .when()
        .contentType(ContentType.JSON)
        .body(mapper.writeValueAsString(order))
        .post("/order")
        .then()
        .statusCode(200);
  }
}
