package com.stefank3.quality.driver;

import java.util.Optional;
import org.openqa.selenium.WebDriver;

/** Narrow read-only access to the current session for failure evidence. */
public interface DriverProvider {
  /**
   * Returns no driver before creation or after cleanup; callers must tolerate an unusable session.
   */
  Optional<WebDriver> currentDriver();
}
