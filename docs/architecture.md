# Architecture

All Java code lives under `src/test/java/com/stefank3/quality`; there is no production application or reusable framework artifact to publish.

| Boundary | Responsibility |
| --- | --- |
| `config/TestConfig` | Immutable configuration, strict parsing, exact origin and live gate validation |
| `driver/DriverFactory` | Conservative Chrome options, new sessions and timeout initialization |
| `driver/DriverProvider` | Optional current driver access for diagnostics only |
| `tests/BaseTest` | Before/after-method ownership and original-failure-preserving cleanup |
| `pages` | Business actions and observable page state; no assertions |
| `components/ProductCard` | Locators scoped to one product card |
| `reporting/FailureEvidenceListener` | Lane validation, approved metadata and bounded failure evidence |
| `support/LocalFixture` | Ephemeral loopback HTTP server with one synthetic document |
| `tests/deterministic` | Framework contracts and controlled Chrome validation |
| `tests/live` | Exactly four live business scenarios |

The test owns a driver, passes it into page constructors, and owns business expectations. Pages use Selenium explicit waits. No generic BasePage, static mutable driver, ThreadLocal, DriverManager or dependency injection container is needed.

The Allure adapter registers through TestNG's service loader. The custom listener is registered once in each suite. It depends on `DriverProvider`, never concrete `BaseTest`. Evidence runs after an invocation while the driver still exists; teardown follows. Allure runtime steps avoid AspectJ.

The two XML suites list exact classes. The default POM selects deterministic tests; `live` replaces the suite and requires opt-in. Suite identity, runtime lane, origin and Allure directory must agree. Deliberate edits to the build are outside this accident-prevention boundary; it is not a security sandbox.

One Maven fork, sequential TestNG execution and a nonparallel DataProvider make a driver field appropriate. Parallelism would require a separate ownership design, not adding ThreadLocal by habit.
