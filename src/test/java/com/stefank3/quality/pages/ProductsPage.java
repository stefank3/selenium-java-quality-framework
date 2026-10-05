package com.stefank3.quality.pages;

import com.stefank3.quality.components.ProductCard;
import com.stefank3.quality.config.TestConfig;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Catalogue navigation and search state; business expectations belong to the tests. */
public final class ProductsPage {
  private static final By HEADING = By.cssSelector(".features_items h2.title");
  private static final By CARDS = By.cssSelector(".features_items .product-image-wrapper");
  private final WebDriver driver;
  private final TestConfig config;
  private final WebDriverWait wait;

  /** Uses a constructor-injected driver and validated time budget. */
  public ProductsPage(WebDriver driver, TestConfig config) {
    this.driver = driver;
    this.config = config;
    this.wait = new WebDriverWait(driver, config.waitTimeout());
  }

  /** Opens only the approved catalogue route, then checks the resulting origin. */
  public ProductsPage open() {
    if (!"live".equals(config.lane())) {
      throw new IllegalStateException("Live pages require a live configuration");
    }
    driver.get(config.baseUrl().resolve("/products").toString());
    wait.until(ExpectedConditions.visibilityOfElementLocated(HEADING));
    config.requireApprovedPage(driver.getCurrentUrl());
    return this;
  }

  /** Submits through the UI and waits for the replacement server-rendered document. */
  public ProductsPage search(String query) {
    config.requireApprovedPage(driver.getCurrentUrl());
    WebElement previousHeading = driver.findElement(HEADING);
    WebElement input =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("search_product")));
    input.clear();
    if (!query.isEmpty()) {
      input.sendKeys(query);
    }
    wait.until(ExpectedConditions.elementToBeClickable(By.id("submit_search"))).click();
    wait.until(ExpectedConditions.stalenessOf(previousHeading));
    wait.until(ExpectedConditions.visibilityOfElementLocated(HEADING));
    config.requireApprovedPage(driver.getCurrentUrl());
    return this;
  }

  /** Returns the rendered heading so empty results are distinguishable from a broken page. */
  public String heading() {
    return driver.findElement(HEADING).getText().strip();
  }

  /** Returns displayed catalogue cards only, excluding recommendation and overlay duplicates. */
  public List<ProductCard> cards() {
    return driver.findElements(CARDS).stream()
        .filter(WebElement::isDisplayed)
        .map(root -> new ProductCard(driver, root, config))
        .toList();
  }

  /** Selects one exact known name; failure is explicit if the catalogue contract changes. */
  public ProductCard productNamed(String name) {
    return cards().stream()
        .filter(card -> name.equals(card.name()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Expected catalogue product is absent"));
  }
}
