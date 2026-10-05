package com.stefank3.quality.tests;

import com.stefank3.quality.config.TestConfig;
import com.stefank3.quality.driver.DriverFactory;
import com.stefank3.quality.driver.DriverProvider;
import java.util.Optional;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/** Owns a fresh browser per invocation and releases it even when setup or assertions fail. */
public abstract class BaseTest implements DriverProvider {
  protected WebDriver driver;
  protected TestConfig config;

  /** Validates safety gates before creating resources; no live navigation happens here. */
  @BeforeMethod(alwaysRun = true)
  public void setUp(ITestContext context) throws Exception {
    config =
        TestConfig.from(
            System.getProperties(), context.getSuite().getXmlSuite().getParameter("lane"));
    driver = createDriver(config);
  }

  /** Provides a narrow lifecycle test seam; normal execution always creates Chrome. */
  protected WebDriver createDriver(TestConfig configuration) {
    return DriverFactory.create(configuration);
  }

  /** Preserves an existing throwable; a new cleanup failure remains a failing configuration. */
  @AfterMethod(alwaysRun = true)
  public void tearDown(ITestResult result) {
    RuntimeException cleanupFailure = null;
    try {
      if (driver != null) {
        driver.quit();
      }
    } catch (RuntimeException failure) {
      cleanupFailure = failure;
    } finally {
      driver = null;
    }
    if (cleanupFailure != null) {
      if (result.getThrowable() != null) {
        result.getThrowable().addSuppressed(cleanupFailure);
      } else {
        throw cleanupFailure;
      }
    }
  }

  /** Exposes the current driver without giving reporting responsibility for its lifecycle. */
  @Override
  public Optional<WebDriver> currentDriver() {
    return Optional.ofNullable(driver);
  }
}
