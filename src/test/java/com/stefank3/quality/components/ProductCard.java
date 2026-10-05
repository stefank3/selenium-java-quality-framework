package com.stefank3.quality.components;

import com.stefank3.quality.config.TestConfig;
import com.stefank3.quality.pages.ProductDetailsPage;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Scopes catalogue identity and navigation to one product, excluding the duplicate hover overlay.
 */
public final class ProductCard {
  private final WebDriver driver;
  private final WebElement root;
  private final TestConfig config;

  /** Receives the owner's browser and a card root valid only for the current document. */
  public ProductCard(WebDriver driver, WebElement root, TestConfig config) {
    this.driver = driver;
    this.root = root;
    this.config = config;
  }

  /** Returns the product identifier used by the site's cart and detail links. */
  public String id() {
    return root.findElement(By.cssSelector(".productinfo .add-to-cart"))
        .getDomAttribute("data-product-id");
  }

  /** Returns the catalogue name from the primary card, not its hover duplicate. */
  public String name() {
    return root.findElement(By.cssSelector(".productinfo p")).getText().strip();
  }

  /** Reads the site's integer-rupee display as an exact decimal value. */
  public BigDecimal price() {
    String text = root.findElement(By.cssSelector(".productinfo h2")).getText().strip();
    if (!text.matches("Rs\\.\\s*\\d+(?:\\.\\d{1,2})?")) {
      throw new IllegalStateException("Unsupported catalogue price format");
    }
    return new BigDecimal(text.replaceFirst("Rs\\.\\s*", ""));
  }

  /** Follows the card's native details link after checking it stays within the approved origin. */
  public ProductDetailsPage openDetails() {
    config.requireApprovedPage(driver.getCurrentUrl());
    WebElement link = root.findElement(By.cssSelector("a[href^='/product_details/']"));
    // The shared boundary rejects missing, malformed and off-origin hrefs without echoing them.
    config.requireApprovedPage(link.getAttribute("href"));
    new WebDriverWait(driver, config.waitTimeout())
        .until(ExpectedConditions.elementToBeClickable(link))
        .click();
    return new ProductDetailsPage(driver, config).awaitLoaded();
  }
}
