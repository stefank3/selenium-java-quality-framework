package com.stefank3.quality.tests.deterministic;

import com.stefank3.quality.components.ProductCard;
import com.stefank3.quality.config.TestConfig;
import com.stefank3.quality.pages.ProductDetailsPage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Exercises configuration boundaries without opening a browser or contacting an application. */
public final class TestConfigTest {
  /** Confirms defaults and explicit property precedence. */
  @Test
  public void defaultsAndOverrides() {
    Properties properties = new Properties();
    TestConfig defaults = TestConfig.from(properties, "deterministic");
    Assert.assertTrue(defaults.headless());
    Assert.assertEquals(defaults.waitTimeout().toSeconds(), 10L);
    properties.setProperty("headless", "false");
    properties.setProperty("wait.seconds", "3");
    TestConfig overrides = TestConfig.from(properties, "deterministic");
    Assert.assertFalse(overrides.headless());
    Assert.assertEquals(overrides.waitTimeout().toSeconds(), 3L);
  }

  /** Supplies invalid values without using the real process configuration. */
  @DataProvider
  public Object[][] invalidConfiguration() {
    return new Object[][] {
      {"browser", "firefox"},
      {"headless", "yes"},
      {"live.enabled", "true"},
      {"live.enabled", "TRUE"},
      {"live.profile.active", "true"},
      {"base.url", "https://automationexercise.com"},
      {"wait.seconds", "0"},
      {"wait.seconds", "31"},
      {"wait.seconds", "invalid"},
      {"test.lane", "live"}
    };
  }

  /** Rejects unsafe configuration instead of silently substituting defaults. */
  @Test(dataProvider = "invalidConfiguration")
  public void rejectsInvalidConfiguration(String key, String value) {
    Properties properties = new Properties();
    properties.setProperty(key, value);
    Assert.expectThrows(
        IllegalArgumentException.class, () -> TestConfig.from(properties, "deterministic"));
  }

  /** Includes origin lookalikes and URL components forbidden by the live contract. */
  @DataProvider
  public Object[][] rejectedOrigins() {
    return new Object[][] {
      {""},
      {"http://automationexercise.com"},
      {"https://automationexercise.com:443"},
      {"https://automationexercise.com:8443"},
      {"https://www.automationexercise.com"},
      {"https://automationexercise.com.evil.invalid"},
      {"https://user:secret@automationexercise.com"},
      {"https://automationexercise.com?token=secret"},
      {"https://automationexercise.com/#fragment"},
      {"https://automationexercise.com/products"},
      {"https://automationexercise.com."},
      {"https://automationexercise.com\\@evil.invalid"}
    };
  }

  /** Rejects whole URL inputs without leaking their content into the exception. */
  @Test(dataProvider = "rejectedOrigins")
  public void rejectsUnapprovedOrigin(String origin) {
    Properties properties = liveProperties();
    properties.setProperty("base.url", origin);
    var failure =
        Assert.expectThrows(
            IllegalArgumentException.class, () -> TestConfig.from(properties, "live"));
    Assert.assertFalse(failure.getMessage().contains("secret"));
  }

  /** Requires both explicit switches even when the approved origin is present. */
  @Test
  public void requiresBothLiveGates() {
    for (String key : new String[] {"live.enabled", "live.profile.active"}) {
      Properties properties = liveProperties();
      properties.remove(key);
      Assert.expectThrows(
          IllegalArgumentException.class, () -> TestConfig.from(properties, "live"));
    }
    Assert.assertEquals(
        TestConfig.from(liveProperties(), "live").sanitizedBaseUrl(),
        "https://automationexercise.com");
    Assert.expectThrows(
        IllegalArgumentException.class, () -> TestConfig.from(new Properties(), "unknown"));
  }

  /** Allows site-owned search paths while rejecting a change of scheme or authority. */
  @Test
  public void validatesNavigationOrigin() {
    TestConfig live = TestConfig.from(liveProperties(), "live");
    live.requireApprovedPage("https://automationexercise.com/products?search=Blue%20Top");
    for (String url :
        new String[] {
          "http://automationexercise.com/products",
          "https://automationexercise.com.invalid/products",
          "https://automationexercise.com:443/products"
        }) {
      Assert.expectThrows(IllegalStateException.class, () -> live.requireApprovedPage(url));
    }
  }

  /** Synthetic secrets exercise parsing and origin rejection without making network requests. */
  @DataProvider
  public Object[][] unsafeNavigationUrls() {
    return new Object[][] {
      {"https://automationexercise.com/%broken?token=query-secret#fragment-secret"},
      {
        "https://user:credential-secret@automationexercise.com/products?token=query-secret#fragment-secret"
      },
      {"https://automationexercise.com.invalid/?token=query-secret#fragment-secret"},
      {"https://automationexercise.com:443/?token=query-secret#fragment-secret"},
      {"http://automationexercise.com/?token=query-secret#fragment-secret"},
      {"https://automationexercise.com/[bad path]?token=query-secret#fragment-secret"}
    };
  }

  /** Neither the message nor its complete exception chain may disclose rejected input. */
  @Test(dataProvider = "unsafeNavigationUrls")
  public void rejectsNavigationWithoutDisclosure(String input) {
    var failure =
        Assert.expectThrows(
            IllegalStateException.class,
            () -> TestConfig.from(liveProperties(), "live").requireApprovedPage(input));
    assertSafeNavigationFailure(failure);
    StringWriter trace = new StringWriter();
    failure.printStackTrace(new PrintWriter(trace));
    Assert.assertFalse(trace.toString().contains(input));
    for (String secret : new String[] {"credential-secret", "query-secret", "fragment-secret"}) {
      Assert.assertFalse(trace.toString().contains(secret));
    }
  }

  /** Keeps accepted origins, ordinary application paths and search queries compatible. */
  @Test
  public void retainsApprovedUrls() {
    for (String origin :
        new String[] {"https://automationexercise.com", "https://automationexercise.com/"}) {
      Properties properties = liveProperties();
      properties.setProperty("base.url", origin);
      TestConfig config = TestConfig.from(properties, "live");
      for (String suffix :
          new String[] {
            "",
            "/",
            "/products?search=Blue%20Top",
            "/product_details/1",
            "/view_cart",
            "/products#catalogue"
          }) {
        config.requireApprovedPage("https://automationexercise.com" + suffix);
      }
    }
  }

  /** Missing link attributes must fail deliberately before a navigation click. */
  @DataProvider
  public Object[][] missingHrefs() {
    return new Object[][] {{null}, {""}, {" \t "}};
  }

  /** Exercises both real page/component href call paths using local DOM doubles only. */
  @Test(dataProvider = "missingHrefs")
  public void rejectsMissingHrefsSafely(String href) {
    AtomicInteger clicks = new AtomicInteger();
    WebElement element =
        (WebElement)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {WebElement.class},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "findElement" -> proxy;
                      case "getAttribute" -> href;
                      case "isDisplayed", "isEnabled" -> true;
                      case "clear", "sendKeys" -> null;
                      case "click" -> {
                        clicks.incrementAndGet();
                        yield null;
                      }
                      default ->
                          throw new UnsupportedOperationException("Unexpected DOM operation");
                    });
    WebDriver driver =
        (WebDriver)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {WebDriver.class},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "getCurrentUrl" -> "https://automationexercise.com/products";
                      case "findElement" -> element;
                      default ->
                          throw new UnsupportedOperationException("Unexpected browser operation");
                    });
    TestConfig config = TestConfig.from(liveProperties(), "live");
    assertSafeNavigationFailure(
        Assert.expectThrows(
            IllegalStateException.class,
            () -> new ProductCard(driver, element, config).openDetails()));
    Assert.assertEquals(clicks.get(), 0, "Missing details href must prevent navigation");
    assertSafeNavigationFailure(
        Assert.expectThrows(
            IllegalStateException.class,
            () -> new ProductDetailsPage(driver, config).addOneToCart()));
    Assert.assertEquals(
        clicks.get(), 1, "Only the simulated add button may be clicked, never the cart link");
  }

  private static void assertSafeNavigationFailure(IllegalStateException failure) {
    Assert.assertEquals(failure.getMessage(), "Navigation URL must use the approved live origin");
    Assert.assertNull(failure.getCause(), "Parsing causes can disclose raw URL input");
    Assert.assertEquals(failure.getSuppressed().length, 0);
  }

  private static Properties liveProperties() {
    Properties properties = new Properties();
    properties.setProperty("test.lane", "live");
    properties.setProperty("live.enabled", "true");
    properties.setProperty("live.profile.active", "true");
    properties.setProperty("base.url", "https://automationexercise.com");
    return properties;
  }
}
