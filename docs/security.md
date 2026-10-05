# Safety and evidence boundaries

The deterministic suite makes no intentional external application requests. Its maintained HTML comes from one JDK HTTP server bound to `127.0.0.1`, with an ephemeral port and no external resources. A restrictive fixture Content-Security-Policy permits its inline script and forbids outbound connections.

This is not an outbound network firewall. Maven and Selenium Manager can download approved tooling/dependencies, and Chrome startup can make background requests. Chrome receives `--no-first-run` and `--disable-background-networking` to reduce that traffic; these flags do not prove complete isolation. Browser security remains enabled. Selenium Manager telemetry is disabled and browser downloads are disabled because Chrome is a prerequisite.

The live profile requires explicit opt-in and exact base-origin validation before browser creation. Unsupported browsers and malformed booleans fail closed. Pages check their resulting origin and follow only catalogue, details and cart routes. Cross-origin page resources may load naturally; origin validation cannot prevent every resource request or a redirect already performed by the browser.

Live execution uses five sequential invocations, fresh anonymous browser sessions and no retries. Only synthetic search values and one cart item are used. No accounts, login, personal data, payments, checkout, security probing, access-control bypass or persistent content creation are supported. Stop when an access challenge or unexpected destination appears.

Successful screenshots exist only as Allure PNG attachments. Capture is limited to one attempt per failed invocation, a fixed browser window and a 5 MiB accepted payload. A failed attachment may produce one bounded fallback PNG; no separate screenshot tree is maintained for successful attachments. Driver command timeouts bound capture attempts. The size check occurs after Selenium returns bytes, so it is an artifact limit, not a pre-allocation memory limit.

Do not retain page source, cookies, profiles, credentials or authorization headers. Metadata is allowlisted: browser/version, headless, Java, OS, lane and a sanitized origin. Reject messages never echo invalid base URLs. Framework tests use deliberately synthetic invalid URL examples; their DataProvider values are not real credentials.

GitHub workflows use read-only repository permissions, immutable action commits, no application secrets and seven-day evidence retention. Uploaded artifacts are limited to lane-specific Allure/Surefire results and genuine fallback diagnostics. No hosted reporting, notifications or publishing. Configure the required deterministic check only after the workflow is merged and proven; repository rules are not changed by this implementation.
