# Test strategy

The deterministic lane checks framework behavior independently of a public site's availability. Explicit TestNG class allowlists separate it from the manually authorized live lane. Execution uses one fork, sequential tests, fresh sessions and no retries.

## Deterministic coverage

Configuration tests cover strict options, both live gates, exact base-origin rejection and accepted page URLs. URL regressions inspect exception messages and complete traces for disclosure, and exercise null/blank href handling through both product-card and cart-link call paths using local doubles. No application navigation occurs in these tests.

Driver and evidence tests verify Chrome options, per-invocation ownership, missing-driver setup, cleanup and original-failure preservation. A cleanup failure after a passing test must fail the run; a cleanup failure after an existing failure is suppressed onto that original throwable. Reporting doubles cover screenshot, attachment and metadata failures.

The local UI check runs twice against the same loopback fixture origin. Each fresh session must have a distinct session ID, empty local storage and no cookies before changing visible state and leaving a storage marker. The fixture contains no external resources. This is not a network-denial boundary for tooling or Chrome startup.

## Live coverage

Four scenario methods produce five invocations:

| Scenario | Contract |
| --- | --- |
| Known product search | Search `Blue Top`; nonempty results; every displayed primary card name contains the query |
| Product details | Navigate from its card; identity/name/price agree; category `Women > Tops`; availability `In Stock` |
| Cart | Add one Blue Top in a fresh session; exactly one row; matching identity/name/unit price; quantity 1; exact decimal total |
| Search boundary DataProvider | Synthetic unmatched term gives no cards under Searched Products; empty input gives All Products and the same catalogue IDs |

Live execution requires explicit authorization, the live profile, opt-in and the exact approved HTTPS base origin. No accounts, login, checkout, payment or persistent content creation is implemented. Chrome uses EAGER document readiness plus bounded explicit waits for consumed application state; implicit waits remain zero.

## Evidence and limitations

Each lane produces native Allure results, sanitized metadata and Surefire reports. Failure evidence is one bounded screenshot attachment when a driver is usable; a separate fallback PNG is produced only if attachment fails. Original TestNG failures remain available when diagnostics fail. Intentionally failing diagnostic probes do not belong in the maintained suites.

The search assertion covers a known-name query, not a universal name-only search contract. Empty-query comparison assumes the catalogue does not change between two immediate navigations. Category, stock status, site resources and browser versions may change. EAGER does not establish that all asynchronous activity has finished or identify a delayed resource. Multiple browsers, parallel execution and broader ecommerce flows are outside scope.

Local test evidence and GitHub Actions evidence are distinct. The workflows have not yet executed; CI Chrome compatibility remains unproven. Make deterministic-validation a required check only after the workflow is merged and proven. Live execution stays manual and is not a required pull-request gate.
