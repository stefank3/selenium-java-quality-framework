package com.stefank3.quality.reporting;

import com.stefank3.quality.config.TestConfig;
import com.stefank3.quality.driver.DriverProvider;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestResult;

/** Adds bounded evidence before teardown, without owning drivers or replacing test failures. */
public final class FailureEvidenceListener implements IInvokedMethodListener, ISuiteListener {
  private static final int MAX_SCREENSHOT_BYTES = 5 * 1024 * 1024;
  private static final String CAPTURED = "quality.evidence.attempted";
  private final BiConsumer<String, byte[]> attachment;

  /**
   * Uses the runtime attachment API; no annotation weaving or second screenshot store is needed.
   */
  public FailureEvidenceListener() {
    this(
        (name, bytes) ->
            Allure.attachment(
                name,
                "image/png",
                new ByteArrayInputStream(bytes),
                AttachmentOptions.withFileExtension("png")));
  }

  /**
   * Allows deterministic verification of attachment failures without modifying global Allure state.
   */
  public FailureEvidenceListener(BiConsumer<String, byte[]> attachment) {
    this.attachment = attachment;
  }

  /** Rejects mismatched suite/output lanes before TestNG invokes any test or setup. */
  @Override
  public void onStart(ISuite suite) {
    TestConfig config =
        TestConfig.from(System.getProperties(), suite.getXmlSuite().getParameter("lane"));
    Path expected = Path.of("target", "allure-results", config.lane()).toAbsolutePath().normalize();
    Path actual =
        Path.of(System.getProperty("allure.results.directory", "allure-results"))
            .toAbsolutePath()
            .normalize();
    if (!actual.equals(expected)) {
      throw new IllegalArgumentException("Allure results directory must match the execution lane");
    }
    try {
      Files.createDirectories(actual);
      // Only approved, non-sensitive values are recorded. Rejected URLs are never echoed.
      String metadata =
          "browser=chrome\nheadless="
              + config.headless()
              + "\njava="
              + safe(System.getProperty("java.version"))
              + "\nos="
              + safe(System.getProperty("os.name"))
              + "\nlane="
              + config.lane()
              + "\nbaseUrl="
              + config.sanitizedBaseUrl()
              + "\n";
      Files.writeString(actual.resolve("environment.properties"), metadata);
    } catch (Exception failure) {
      diagnostic("environment metadata", failure);
    }
  }

  /** Runs while the test/fixture scope is active and before BaseTest releases its session. */
  @Override
  public void afterInvocation(IInvokedMethod method, ITestResult result) {
    if (method.isTestMethod()) {
      try {
        Allure.label("lane", System.getProperty("test.lane", "deterministic"));
        Allure.label("browser", "chrome");
        Allure.label("headless", System.getProperty("headless", "true"));
        if (result.getInstance() instanceof DriverProvider provider) {
          provider
              .currentDriver()
              .ifPresent(
                  driver -> {
                    if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver remote) {
                      Allure.label("browserVersion", remote.getCapabilities().getBrowserVersion());
                    }
                  });
        }
      } catch (Exception failure) {
        diagnostic("test metadata", failure);
      }
    }
    if (result.getStatus() == ITestResult.FAILURE) {
      capture(result);
    }
  }

  /** Attempts one screenshot per failed invocation; a missing or broken driver is harmless. */
  public void capture(ITestResult result) {
    if (result.getAttribute(CAPTURED) != null) {
      return;
    }
    result.setAttribute(CAPTURED, true);
    try {
      if (!(result.getInstance() instanceof DriverProvider provider)) {
        return;
      }
      var driver = provider.currentDriver();
      if (driver.isEmpty() || !(driver.get() instanceof TakesScreenshot screenshot)) {
        return;
      }
      byte[] bytes = screenshot.getScreenshotAs(OutputType.BYTES);
      if (bytes == null || bytes.length == 0 || bytes.length > MAX_SCREENSHOT_BYTES) {
        diagnostic(
            "screenshot size limit", new IllegalStateException("Empty or oversized screenshot"));
        return;
      }
      String name = safe(result.getName()) + "-" + UUID.randomUUID();
      try {
        attachment.accept(name, bytes);
      } catch (Exception failure) {
        diagnostic("Allure attachment", failure);
        // A fallback exists only when Allure fails; successful attachments are never duplicated.
        Path fallback =
            Path.of(
                "target",
                "diagnostics",
                "live".equals(System.getProperty("test.lane")) ? "live" : "deterministic");
        Files.createDirectories(fallback);
        Path diagnostic = fallback.resolve(name + ".png");
        Files.write(diagnostic, bytes);
        result.setAttribute("quality.evidence.diagnostic", diagnostic);
      }
    } catch (Exception failure) {
      diagnostic("failure evidence", failure);
    }
  }

  private static String safe(String input) {
    String value = input == null ? "unknown" : input.replaceAll("[^A-Za-z0-9._-]", "_");
    return value.substring(0, Math.min(value.length(), 80));
  }

  private static void diagnostic(String operation, Exception failure) {
    System.err.println(
        "Evidence warning: " + operation + " (" + failure.getClass().getSimpleName() + ")");
  }
}
