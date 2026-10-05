package com.stefank3.quality.config;

import java.net.URI;
import java.time.Duration;
import java.util.Properties;

/** Immutable execution boundary; invalid configuration fails before any browser is created. */
public record TestConfig(String lane, boolean headless, URI baseUrl, Duration waitTimeout) {
  /**
   * Validates explicit suite identity and JVM properties without consulting environment variables.
   */
  public static TestConfig from(Properties properties, String suiteLane) {
    if (!"deterministic".equals(suiteLane) && !"live".equals(suiteLane)) {
      throw new IllegalArgumentException("Unknown suite lane");
    }
    if (!suiteLane.equals(properties.getProperty("test.lane", "deterministic"))) {
      throw new IllegalArgumentException("Suite and Maven lane disagree");
    }
    if (!"chrome".equals(properties.getProperty("browser", "chrome"))) {
      throw new IllegalArgumentException("Only browser=chrome is supported");
    }
    boolean headless = strictBoolean(properties, "headless", "true");
    boolean enabled = strictBoolean(properties, "live.enabled", "false");
    boolean profile = strictBoolean(properties, "live.profile.active", "false");
    String rawUrl = properties.getProperty("base.url", "");
    URI origin = null;
    if ("live".equals(suiteLane)) {
      if (!enabled || !profile) {
        throw new IllegalArgumentException("Live execution requires -Plive -Dlive.enabled=true");
      }
      // Compare the complete allowlist value: do not echo rejected input into reports.
      if (!"https://automationexercise.com".equals(rawUrl)
          && !"https://automationexercise.com/".equals(rawUrl)) {
        throw new IllegalArgumentException("base.url must be the exact approved HTTPS origin");
      }
      origin = URI.create("https://automationexercise.com");
    } else if (enabled || profile || !rawUrl.isEmpty()) {
      throw new IllegalArgumentException("Deterministic execution rejects live configuration");
    }
    int seconds;
    try {
      seconds = Integer.parseInt(properties.getProperty("wait.seconds", "10"));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("wait.seconds must be an integer", exception);
    }
    if (seconds < 1 || seconds > 30) {
      throw new IllegalArgumentException("wait.seconds must be between 1 and 30");
    }
    return new TestConfig(suiteLane, headless, origin, Duration.ofSeconds(seconds));
  }

  private static boolean strictBoolean(Properties properties, String key, String fallback) {
    String value = properties.getProperty(key, fallback);
    if (!"true".equals(value) && !"false".equals(value)) {
      throw new IllegalArgumentException(key + " must be true or false");
    }
    return Boolean.parseBoolean(value);
  }

  /** Returns an approved origin label without user input, paths, or query strings. */
  public String sanitizedBaseUrl() {
    return "live".equals(lane) ? "https://automationexercise.com" : "http://127.0.0.1";
  }

  /** Stops page actions after an unexpected origin change without recording the rejected URL. */
  public void requireApprovedPage(String currentUrl) {
    String safeError = "Navigation URL must use the approved live origin";
    if (currentUrl == null || currentUrl.isBlank()) {
      throw new IllegalStateException(safeError);
    }
    URI current;
    try {
      current = URI.create(currentUrl);
    } catch (IllegalArgumentException rejected) {
      // URI parsing exceptions include raw input; do not retain them as a cause or suppressed
      // error.
      throw new IllegalStateException(safeError);
    }
    if (!"live".equals(lane)
        || !"https".equals(current.getScheme())
        || !"automationexercise.com".equals(current.getRawAuthority())) {
      throw new IllegalStateException(safeError);
    }
  }
}
