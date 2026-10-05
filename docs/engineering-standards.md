# Engineering standards

- Use the committed Maven Wrapper and Java 21. Do not use global Maven 3.3.9.
- Pin direct dependencies, build plugins, formatter and workflow actions. Verify updates through official releases and execution.
- `clean verify` is the canonical gate. Spotless checks Java with pinned google-java-format; `spotless:apply` is an explicit developer action.
- Keep driver ownership in BaseTest and constructor injection in pages/components. No static mutable drivers, ThreadLocal, generic utility collection or generic BasePage.
- Tests assert business expectations. Pages expose meaningful actions and state. Use `BigDecimal` for currency arithmetic.
- Prefer stable IDs, names, semantic CSS and scoped locators. Document a concrete reason before introducing XPath. Do not cache stale elements across navigation.
- Use explicit waits for observable conditions and zero implicit wait. Never use `Thread.sleep`, retries, swallowed assertions or JavaScript clicks to bypass interaction problems.
- Give every maintained Java class concise class-level Javadoc. Document reusable APIs, lifecycle details, configuration boundaries and failure behavior. Comments explain purpose rather than syntax.
- XML/YAML comments explain safety and lifecycle decisions. Keep generated wrapper license/comments intact.
- Never commit generated output or IntelliJ metadata. Do not inflate live tests to demonstrate framework mechanics.
