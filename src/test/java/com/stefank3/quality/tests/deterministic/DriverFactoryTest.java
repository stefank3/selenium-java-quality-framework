package com.stefank3.quality.tests.deterministic;

import com.stefank3.quality.config.TestConfig;
import com.stefank3.quality.driver.DriverFactory;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.openqa.selenium.PageLoadStrategy;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Checks Chrome option construction without starting Chrome. */
public final class DriverFactoryTest {
  /** Verifies headless selection, conservative traffic options and enabled browser security. */
  @Test
  public void buildsConservativeOptions() {
    Properties properties = new Properties();
    var options = DriverFactory.options(TestConfig.from(properties, "deterministic"));
    Assert.assertEquals(options.getBrowserName(), "chrome");
    Assert.assertEquals(options.getCapability("pageLoadStrategy"), PageLoadStrategy.EAGER);
    var chrome = (Map<?, ?>) options.getCapability("goog:chromeOptions");
    var arguments = (List<?>) chrome.get("args");
    Assert.assertTrue(arguments.contains("--headless=new"));
    Assert.assertTrue(arguments.contains("--no-first-run"));
    Assert.assertTrue(arguments.contains("--disable-background-networking"));
    Assert.assertFalse(arguments.contains("--no-sandbox"));
    Assert.assertNotEquals(options.getCapability("acceptInsecureCerts"), Boolean.TRUE);
    properties.setProperty("headless", "false");
    var headed =
        (Map<?, ?>)
            DriverFactory.options(TestConfig.from(properties, "deterministic"))
                .getCapability("goog:chromeOptions");
    Assert.assertFalse(((List<?>) headed.get("args")).contains("--headless=new"));
  }
}
