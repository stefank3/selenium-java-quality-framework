package com.stefank3.quality.driver;

import com.stefank3.quality.config.TestConfig;
import java.time.Duration;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.http.ClientConfig;

/** Creates isolated Chrome sessions; Selenium Manager resolves the matching driver. */
public final class DriverFactory {
  private DriverFactory() {}

  /** Builds conservative options without launching Chrome or disabling browser security. */
  public static ChromeOptions options(TestConfig config) {
    ChromeOptions options = new ChromeOptions();
    // Wait for DOM readiness; page objects wait for the application state they actually consume.
    options.setPageLoadStrategy(PageLoadStrategy.EAGER);
    options.addArguments(
        "--window-size=1440,1000", "--no-first-run", "--disable-background-networking");
    if (config.headless()) {
      options.addArguments("--headless=new");
    }
    return options;
  }

  /** Returns a new driver, cleaning up if timeout initialization fails. */
  public static ChromeDriver create(TestConfig config) {
    ChromeDriver driver =
        new ChromeDriver(
            options(config),
            ClientConfig.defaultConfig()
                .connectionTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(45)));
    try {
      driver.manage().timeouts().implicitlyWait(Duration.ZERO);
      driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
      driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
      return driver;
    } catch (RuntimeException failure) {
      try {
        driver.quit();
      } catch (RuntimeException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }
}
