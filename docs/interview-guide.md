# Interview guide

Describe this repository as a current reference implementation built on established Java, Selenium, Maven and TestNG experience. Do not present it as proprietary production code or claim every feature was recently used commercially.

| Topic | Explanation grounded in this repository |
| --- | --- |
| Maven and wrapper | The wrapper fixes the runtime; POM pins dependencies/plugins; `verify` includes tests and formatting. Dependency download checks are distinct from browser application traffic. |
| TestNG lifecycle | BeforeMethod creates one session per method/DataProvider row; AfterMethod always attempts cleanup. No tests rely on priority/order or a previous cart. |
| DataProvider | Two safe search rows demonstrate parameterized execution without inflating live coverage. |
| DriverFactory | Separates Chrome option construction from ownership; returns a new session; handles initialization cleanup. |
| Constructor injection | Tests pass their driver to pages/components explicitly; dependencies are visible and no DI container is needed. |
| Page/component boundaries | A page models actions/state, a card scopes repeated DOM; tests retain business expectations. No assertion-heavy page methods or speculative BasePage. |
| Explicit waits | Wait for document replacement, visibility or cart confirmation; zero implicit wait avoids compounded timing. |
| Selenium Manager | Matches ChromeDriver to installed Chrome; no WebDriverManager or hardcoded driver executable. Provisioning is an external infrastructure dependency. |
| Allure | Native TestNG adapter captures result and fixture status; runtime steps and one bounded screenshot add context without AspectJ. |
| Original failure | Screenshot, attachment and metadata errors are secondary. Cleanup failure is suppressed onto an existing throwable; otherwise it fails the run. |
| CI split | Required local-content tests validate the framework; manual external tests show integration behavior without making public-site uptime a merge dependency. |
| Scope choices | One browser and sequential execution prioritize clarity, reliable ownership and low traffic. Parallelism would require redesign and evidence. |

Useful demonstrations: trace one cart test through its page objects; explain why the empty-query assertion differs from an unmatched search; show configuration rejection before driver creation; locate the Allure attachment within a failure step; explain why passing UI assertions do not excuse a teardown failure.

Be candid about limitations: this is a small catalogue contract, not comprehensive ecommerce coverage. Site content/ads, browser updates, infrastructure downloads and runner changes can fail a run. Default Chrome flags are not an outbound firewall. The Windows process-host socket issue required a local JVM setting. GitHub workflow correctness still needs a real CI run after merge. No security, performance, accessibility-certification or cross-browser claims are made.

See selenium-vs-playwright.md for a balanced comparison. Deferred work includes multiple browsers, parallelism, Grid, Docker, retries, API/database testing, visual regression, accounts/payments and advanced reporting. Add any of these only when an actual requirement and validation plan justify it.
