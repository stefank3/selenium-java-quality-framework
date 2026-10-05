package com.stefank3.quality.tests.deterministic;

import com.stefank3.quality.config.TestConfig;
import com.stefank3.quality.tests.BaseTest;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.Test;

/** Proves ownership and teardown behavior with tiny driver doubles and real lifecycle methods. */
public final class DriverLifecycleTest {
  /** Confirms independent driver objects and exactly one quit for each invocation. */
  @Test
  public void createsAndClosesEachInvocation(ITestContext context) throws Exception {
    AtomicInteger quits = new AtomicInteger();
    LifecycleOwner owner = new LifecycleOwner(quits, false, false);
    owner.setUp(context);
    WebDriver first = owner.currentDriver().orElseThrow();
    owner.tearDown(result(null));
    Assert.assertTrue(owner.currentDriver().isEmpty());
    owner.setUp(context);
    Assert.assertNotSame(owner.currentDriver().orElseThrow(), first);
    owner.tearDown(result(null));
    Assert.assertEquals(quits.get(), 2);
  }

  /** Keeps assertion identity when quit fails and surfaces quit failure after a passing test. */
  @Test
  public void preservesPrimaryFailure(ITestContext context) throws Exception {
    LifecycleOwner owner = new LifecycleOwner(new AtomicInteger(), true, false);
    owner.setUp(context);
    AssertionError original = new AssertionError("original assertion");
    ITestResult failed = result(original);
    owner.tearDown(failed);
    Assert.assertSame(failed.getThrowable(), original);
    Assert.assertEquals(original.getSuppressed().length, 1);
    Assert.assertTrue(owner.currentDriver().isEmpty());
    owner.setUp(context);
    Assert.expectThrows(IllegalStateException.class, () -> owner.tearDown(result(null)));
    Assert.assertTrue(owner.currentDriver().isEmpty());
  }

  /** Cleanup remains safe after browser creation fails. */
  @Test
  public void handlesMissingDriverSetup(ITestContext context) {
    LifecycleOwner owner = new LifecycleOwner(new AtomicInteger(), false, true);
    var failure = Assert.expectThrows(IllegalStateException.class, () -> owner.setUp(context));
    owner.tearDown(result(failure));
    Assert.assertTrue(owner.currentDriver().isEmpty());
  }

  private static ITestResult result(Throwable failure) {
    return (ITestResult)
        Proxy.newProxyInstance(
            DriverLifecycleTest.class.getClassLoader(),
            new Class<?>[] {ITestResult.class},
            (proxy, method, args) -> {
              if ("getThrowable".equals(method.getName())) {
                return failure;
              }
              throw new UnsupportedOperationException("Unexpected lifecycle result operation");
            });
  }

  /** Test-only owner supplies disposable driver doubles without Selenium or shared state. */
  private static final class LifecycleOwner extends BaseTest {
    private final AtomicInteger quits;
    private final boolean failQuit;
    private final boolean failSetup;

    private LifecycleOwner(AtomicInteger quits, boolean failQuit, boolean failSetup) {
      this.quits = quits;
      this.failQuit = failQuit;
      this.failSetup = failSetup;
    }

    @Override
    protected WebDriver createDriver(TestConfig configuration) {
      if (failSetup) {
        throw new IllegalStateException("controlled creation failure");
      }
      return (WebDriver)
          Proxy.newProxyInstance(
              getClass().getClassLoader(),
              new Class<?>[] {WebDriver.class},
              (proxy, method, args) -> {
                if ("quit".equals(method.getName())) {
                  quits.incrementAndGet();
                  if (failQuit) {
                    throw new IllegalStateException("controlled quit failure");
                  }
                }
                return null;
              });
    }
  }
}
