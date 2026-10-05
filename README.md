# Selenium Java Quality Framework

A compact portfolio/reference implementation demonstrating maintainable Java UI automation: explicit configuration boundaries, per-test browser ownership, constructor-injected page objects, controlled validation, and failure reporting that preserves the original exception.

**Stack:** Temurin Java 21, Maven 3.10.0 Wrapper, Selenium 4, Selenium Manager, TestNG, Chrome, Allure TestNG, Spotless and GitHub Actions.

**Two execution lanes:** the default deterministic suite tests the framework and a loopback-only HTML fixture. The live suite is manually authorized and exercises a small catalogue/cart contract on Automation Exercise. Live tests never run in pull-request CI.

## Run

Install Temurin Java 21 and Chrome, then use the wrapper rather than a global Maven installation:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd -B -ntp clean verify
```

On Linux/macOS use `./mvnw`. Initial execution may download Maven, dependencies and a matching ChromeDriver. Chrome startup and provisioning are not a network-denial boundary; maintained deterministic test content uses only loopback.

After deterministic validation passes, run the live suite only when authorized:

```powershell
.\mvnw.cmd -B -ntp -Plive '-Dlive.enabled=true' '-Dbase.url=https://automationexercise.com' clean verify
```

There are four live scenario methods (five invocations): known-product search, details, one-product cart, and two data-driven search cases. No registration, login, checkout or payment is implemented.

## Evidence and quality gate

`verify` runs tests and checks Java formatting. Use `.\mvnw.cmd -B -ntp spotless:apply` to format intentional edits. Tests are sequential with no retries; every UI invocation receives a fresh Chrome session.

Raw Allure results, including failure screenshot attachments, are under `target/allure-results/<lane>/`; Surefire reports are under `target/surefire-reports/<lane>/`. A screenshot fallback is written under `target/diagnostics/<lane>/` only when Allure attachment fails. `clean` deletes previous output from both lanes. Reports are not hosted or published.

The deterministic workflow runs on pull requests and pushes to `main`. After it is merged and proven, configure `deterministic-validation` as a required check. Workflow creation alone does not enforce merges; this implementation does not change repository rulesets. The live workflow is dispatch-only, requires affirmative authorization, and first runs the deterministic prerequisite.

## Study the implementation

- [Running tests](docs/running-tests.md): configuration, commands, output and troubleshooting.
- [Architecture](docs/architecture.md) and [test strategy](docs/test-strategy.md): responsibilities and coverage.
- [Code walkthrough](docs/code-walkthrough.md) and [interview guide](docs/interview-guide.md): explain the engineering choices.
- [Engineering standards](docs/engineering-standards.md), [security](docs/security.md), and [Selenium vs Playwright](docs/selenium-vs-playwright.md).

This is current portfolio work based on established Java/Selenium experience. It does not reproduce proprietary systems or imply that every feature was recently used commercially. Chrome-only, sequential execution keeps the implementation explainable; broad platform coverage and advanced infrastructure are deferred.

MIT licensed; see [LICENSE](LICENSE).
