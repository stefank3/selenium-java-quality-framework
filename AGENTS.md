# Repository instructions

This is a Java 21 / Selenium / TestNG portfolio reference implementation. Read README.md and docs/running-tests.md before changing execution behavior.

- Use the Maven Wrapper: `.\mvnw.cmd -B -ntp clean verify` on Windows, `./mvnw -B -ntp clean verify` on Linux.
- Format Java with the pinned Spotless plugin. Do not add Checkstyle alongside it.
- Default validation uses only controlled loopback application content. Do not imply that browser startup or dependency provisioning is network-isolated.
- Live execution requires explicit user authorization, `-Plive`, `-Dlive.enabled=true` and the exact approved base origin. Never expand into login, account creation, checkout, payments or persistent writes.
- Keep explicit suite allowlists, Chrome-only sequential execution, fresh sessions and zero retries.
- Preserve original failures when diagnostics or cleanup fail. Reporting may access the driver only through DriverProvider.
- Keep docs implementation-backed; no invented commercial-experience or validation claims.
- Keep `.idea/`, target output, browser profiles and temporary probes out of version control.
- Do not stage, commit, push, dispatch workflows, alter repository rules or publish without user authorization.

See docs/engineering-standards.md for JavaDoc and design rules. Do not add administrative milestone documents.
