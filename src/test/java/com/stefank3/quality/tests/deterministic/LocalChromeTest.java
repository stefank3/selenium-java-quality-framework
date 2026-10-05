package com.stefank3.quality.tests.deterministic;

import com.stefank3.quality.support.LocalFixture;
import com.stefank3.quality.tests.BaseTest;
import io.qameta.allure.Allure;
import java.util.HashSet;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Exercises one controlled UI contract twice to prove clean sessions against the same origin. */
public final class LocalChromeTest extends BaseTest {
  private LocalFixture sharedOrigin;
  private final Set<String> sessions = new HashSet<>();

  /** A stable origin ensures storage isolation is caused by fresh sessions, not different ports. */
  @BeforeClass
  public void startOrigin() throws Exception {
    sharedOrigin = new LocalFixture();
  }

  /** Releases the class fixture independently of browser setup outcomes. */
  @AfterClass(alwaysRun = true)
  public void stopOrigin() {
    if (sharedOrigin != null) {
      sharedOrigin.close();
    }
  }

  /** Two sequential invocations are the minimum needed to demonstrate isolation. */
  @DataProvider(parallel = false)
  public Object[][] invocations() {
    return new Object[][] {{"first"}, {"second"}};
  }

  /**
   * Verifies UI state and clean storage, then leaves a marker that must not reach the next session.
   */
  @Test(dataProvider = "invocations")
  public void controlledUiAndFreshSession(String marker) {
    String session = ((RemoteWebDriver) driver).getSessionId().toString();
    Assert.assertTrue(sessions.add(session), "Each invocation must own a distinct session");
    Allure.step(
        "Open the controlled loopback fixture", () -> driver.get(sharedOrigin.origin().toString()));
    JavascriptExecutor script = (JavascriptExecutor) driver;
    Assert.assertNull(script.executeScript("return localStorage.getItem('session-marker')"));
    Assert.assertTrue(driver.manage().getCookies().isEmpty());
    Allure.step(
        "Change and verify local UI state",
        () -> {
          driver.findElement(By.id("update")).click();
          new WebDriverWait(driver, config.waitTimeout())
              .until(ExpectedConditions.textToBe(By.id("state"), "Updated"));
          Assert.assertEquals(driver.findElement(By.id("state")).getText(), "Updated");
        });
    script.executeScript("localStorage.setItem('session-marker', arguments[0])", marker);
  }
}
