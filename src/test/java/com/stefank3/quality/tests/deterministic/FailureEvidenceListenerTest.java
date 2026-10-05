package com.stefank3.quality.tests.deterministic;

import com.stefank3.quality.driver.DriverProvider;
import com.stefank3.quality.reporting.FailureEvidenceListener;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.IInvokedMethod;
import org.testng.ITestResult;
import org.testng.annotations.Test;

/** Tests evidence failure isolation without global drivers or intentional suite failures. */
public final class FailureEvidenceListenerTest {
  /** Only one capture is attempted even if multiple callbacks see the same failure. */
  @Test
  public void attachesOnceAndPreservesThrowable() {
    AtomicInteger attachments = new AtomicInteger();
    FailureEvidenceListener listener =
        new FailureEvidenceListener(
            (name, bytes) -> {
              Assert.assertFalse(name.contains("/"));
              attachments.incrementAndGet();
            });
    AssertionError original = new AssertionError("evidence-original");
    ITestResult result = result(() -> Optional.of(driver(false)), original);
    listener.capture(result);
    listener.capture(result);
    Assert.assertEquals(attachments.get(), 1);
    Assert.assertSame(result.getThrowable(), original);
  }

  /** Absent drivers, broken screenshots and attachment sinks never replace the primary failure. */
  @Test
  public void toleratesEvidenceFailures() throws Exception {
    FailureEvidenceListener listener =
        new FailureEvidenceListener(
            (name, bytes) -> {
              throw new IllegalStateException("controlled attachment failure");
            });
    DriverProvider[] providers = {
      Optional::empty,
      () -> Optional.of(driver(true)),
      () -> Optional.of(driver(false)),
      () -> {
        throw new IllegalStateException("controlled provider failure");
      }
    };
    for (DriverProvider provider : providers) {
      AssertionError original = new AssertionError("retained assertion");
      ITestResult result = result(provider, original);
      listener.capture(result);
      Assert.assertSame(result.getThrowable(), original);
      Assert.assertEquals(original.getMessage(), "retained assertion");
      Path fallback = (Path) result.getAttribute("quality.evidence.diagnostic");
      if (fallback != null) {
        try {
          Assert.assertTrue(
              Files.size(fallback) > 0, "Attachment failure must leave bounded evidence");
        } finally {
          Files.delete(fallback);
        }
      }
    }
  }

  /** A metadata-access failure cannot replace a failed invocation's original throwable. */
  @Test
  public void toleratesMetadataFailure() {
    DriverProvider broken =
        () -> {
          throw new IllegalStateException("controlled metadata failure");
        };
    AssertionError original = new AssertionError("metadata-original");
    ITestResult result = result(broken, original);
    IInvokedMethod method =
        (IInvokedMethod)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {IInvokedMethod.class},
                (proxy, called, args) -> "isTestMethod".equals(called.getName()));
    new FailureEvidenceListener().afterInvocation(method, result);
    Assert.assertSame(result.getThrowable(), original);
  }

  private static WebDriver driver(boolean failScreenshot) {
    return (WebDriver)
        Proxy.newProxyInstance(
            FailureEvidenceListenerTest.class.getClassLoader(),
            new Class<?>[] {WebDriver.class, TakesScreenshot.class},
            (proxy, method, args) -> {
              if ("getScreenshotAs".equals(method.getName())) {
                if (failScreenshot) {
                  throw new IllegalStateException("controlled screenshot failure");
                }
                return Base64.getDecoder()
                    .decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3foAAAAASUVORK5CYII=");
              }
              return null;
            });
  }

  private static ITestResult result(DriverProvider provider, Throwable original) {
    Map<String, Object> attributes = new HashMap<>();
    return (ITestResult)
        Proxy.newProxyInstance(
            FailureEvidenceListenerTest.class.getClassLoader(),
            new Class<?>[] {ITestResult.class},
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "getInstance" -> provider;
                  case "getThrowable" -> original;
                  case "getName" -> "unsafe/name";
                  case "getStatus" -> ITestResult.FAILURE;
                  case "getAttribute" -> attributes.get(args[0]);
                  case "setAttribute" -> attributes.put((String) args[0], args[1]);
                  default -> null;
                });
  }
}
