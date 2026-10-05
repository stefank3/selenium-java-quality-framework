package com.stefank3.quality.pages;

import com.stefank3.quality.config.TestConfig;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Exposes consumed product details and a single reversible cart action, with no checkout API. */
public final class ProductDetailsPage {
  private static final By INFORMATION = By.cssSelector(".product-information");
  private final WebDriver driver;
  private final TestConfig config;
  private final WebDriverWait wait;

  /** Receives the test's existing session rather than creating a browser. */
  public ProductDetailsPage(WebDriver driver, TestConfig config) {
    this.driver = driver;
    this.config = config;
    this.wait = new WebDriverWait(driver, config.waitTimeout());
  }

  /** Waits for consumed content and rejects an unexpected navigation origin. */
  public ProductDetailsPage awaitLoaded() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(INFORMATION));
    config.requireApprovedPage(driver.getCurrentUrl());
    return this;
  }

  /** Returns the hidden product identity used by the site's add-to-cart action. */
  public String id() {
    return driver.findElement(By.id("product_id")).getDomAttribute("value");
  }

  /** Returns the displayed product name. */
  public String name() {
    return driver.findElement(INFORMATION).findElement(By.tagName("h2")).getText().strip();
  }

  /** Returns the category text without assuming a position among detail paragraphs. */
  public String category() {
    return field("Category:");
  }

  /** Returns the displayed availability contract. */
  public String availability() {
    return field("Availability:");
  }

  /** Reads the displayed rupee amount without floating-point arithmetic. */
  public BigDecimal price() {
    String value =
        driver.findElement(By.cssSelector(".product-information > span > span")).getText().strip();
    if (!value.matches("Rs\\.\\s*\\d+(?:\\.\\d{1,2})?")) {
      throw new IllegalStateException("Unsupported detail price format");
    }
    return new BigDecimal(value.replaceFirst("Rs\\.\\s*", ""));
  }

  /** Adds quantity one, waits for confirmation, and follows only the cart link. */
  public CartPage addOneToCart() {
    config.requireApprovedPage(driver.getCurrentUrl());
    var quantity = driver.findElement(By.id("quantity"));
    quantity.clear();
    quantity.sendKeys("1");
    wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector(".product-information button.cart")))
        .click();
    var viewCart =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector("#cartModal a[href='/view_cart']")));
    // Validate even a missing href before the final navigation; error text never includes the URL.
    config.requireApprovedPage(viewCart.getAttribute("href"));
    viewCart.click();
    return new CartPage(driver, config).awaitLoaded();
  }

  private String field(String label) {
    return driver.findElement(INFORMATION).findElements(By.tagName("p")).stream()
        .map(element -> element.getText().strip())
        .filter(text -> text.startsWith(label))
        .map(text -> text.substring(label.length()).strip())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Required product field is absent: " + label));
  }
}
