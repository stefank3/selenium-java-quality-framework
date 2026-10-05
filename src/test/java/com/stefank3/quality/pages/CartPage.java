package com.stefank3.quality.pages;

import com.stefank3.quality.config.TestConfig;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Reads cart state only; intentionally exposes no checkout or payment action. */
public final class CartPage {
  private final WebDriver driver;
  private final TestConfig config;

  /** Shares the test-owned driver so product identity remains in the same session. */
  public CartPage(WebDriver driver, TestConfig config) {
    this.driver = driver;
    this.config = config;
  }

  /** Waits for the populated cart table and checks its origin. */
  public CartPage awaitLoaded() {
    new WebDriverWait(driver, config.waitTimeout())
        .until(ExpectedConditions.visibilityOfElementLocated(By.id("cart_info_table")));
    config.requireApprovedPage(driver.getCurrentUrl());
    return this;
  }

  /** Counts product rows rather than the table header. */
  public int itemCount() {
    return driver.findElements(By.cssSelector("#cart_info_table tbody tr[id^='product-']")).size();
  }

  /** Returns the selected row's displayed product name. */
  public String name(String productId) {
    return row(productId).findElement(By.cssSelector(".cart_description h4 a")).getText().strip();
  }

  /** Reads the identity encoded in the cart row's native details link. */
  public String productId(String expectedId) {
    String href =
        row(expectedId)
            .findElement(By.cssSelector(".cart_description h4 a"))
            .getDomAttribute("href");
    return href.substring(href.lastIndexOf('/') + 1);
  }

  /** Returns the row's unit price as an exact decimal. */
  public BigDecimal unitPrice(String productId) {
    return amount(row(productId).findElement(By.cssSelector(".cart_price p")).getText());
  }

  /** Returns the visible quantity, rejecting nonnumeric content. */
  public int quantity(String productId) {
    return Integer.parseInt(
        row(productId).findElement(By.cssSelector(".cart_quantity button")).getText().strip());
  }

  /** Returns the site's calculated line total for comparison in the test. */
  public BigDecimal total(String productId) {
    return amount(row(productId).findElement(By.cssSelector(".cart_total_price")).getText());
  }

  private WebElement row(String productId) {
    if (!productId.matches("\\d+")) {
      throw new IllegalArgumentException("Product id must contain digits only");
    }
    return driver.findElement(By.id("product-" + productId));
  }

  private BigDecimal amount(String raw) {
    String value = raw.strip();
    if (!value.matches("Rs\\.\\s*\\d+(?:\\.\\d{1,2})?")) {
      throw new IllegalStateException("Unsupported cart price format");
    }
    return new BigDecimal(value.replaceFirst("Rs\\.\\s*", ""));
  }
}
