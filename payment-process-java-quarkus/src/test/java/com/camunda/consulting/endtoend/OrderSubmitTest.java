package com.camunda.consulting.endtoend;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.time.Duration;

@QuarkusTest
public class OrderSubmitTest {

  @Test
  public void testOrderSubmit() {
    WebDriver driver = new ChromeDriver();
    driver.manage().window().maximize();
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    driver.get("http://localhost:8081/orderform");
    driver.findElement(By.id("customerId")).sendKeys("customer100");
    driver.findElement(By.id("orderId")).sendKeys("order1");
    driver.findElement(By.id("cardNumber")).sendKeys("1234567890");
    driver.findElement(By.id("cvc")).sendKeys("123");
    driver.findElement(By.id("expiryDate")).sendKeys("12/24");
    driver.findElement(By.id("orderTotal")).sendKeys("90");
    driver.findElement(By.id("submitBtn")).click();
    driver.findElement(By.id("orderConfirmation"));
    driver.quit();
  }
}
