# Running tests

Use Temurin Java 21 and an installed Chrome. No accounts or application credentials are needed. The wrapper pins Maven 3.10.0; an older global `mvn` is not supported.

```powershell
.\mvnw.cmd --version
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp spotless:apply
```

The first two commands establish runtime identity and execute the deterministic quality gate. The third formats intentional Java edits. Linux/macOS use `./mvnw` (make it executable after checkout if necessary).

For an explicitly authorized live run:

```powershell
.\mvnw.cmd -B -ntp -Plive '-Dlive.enabled=true' '-Dbase.url=https://automationexercise.com' clean verify
```

Run deterministic validation first. Do not schedule live execution or use live tests as a required PR gate. Four methods produce five invocations because the search DataProvider has two rows. No automatic retries are configured.

## Configuration

JVM `-D` properties override POM/code defaults. There is no application configuration from environment variables, credentials files or TestNG method parameters. Suite lane is fixed in XML and must agree with the Maven lane. The `live` profile supplies its marker; opt-in remains separate.

| Property | Default / allowed values |
| --- | --- |
| `browser` | `chrome` only; all other values fail |
| `headless` | `true`; only exact `true` or `false` |
| `wait.seconds` | `10`; integer between 1 and 30 |
| `base.url` | Empty for deterministic; exact approved HTTPS origin (optional trailing slash) for live |
| `live.enabled` | `false`; explicit `true` required with `-Plive` |

Do not override internal suite/lane/output settings. Runtime checks reject mismatched suite and Allure paths. Base-origin validation rejects credentials, ports (including explicit `:443`), queries, fragments, subdomains, lookalikes and non-root paths. Browser navigation may include the site's search query, but must retain the approved origin.

Timeouts: zero implicit wait, 10-second default explicit waits, 30-second page load, 10-second script timeout, 10-second driver connection and 45-second driver read timeout. Selenium Manager has a 60-second provisioning timeout; the test fork is bounded at 600 seconds. No test retries or sleeps.

Chrome uses `PageLoadStrategy.EAGER`: navigation waits for DOM readiness rather than full document/resource completion. Page objects still wait for the visible heading, replacement document, product information or cart confirmation they consume. This does not bypass failed navigation, wait for every asynchronous application operation, or block third-party resources. `NONE` is not used.

## Results

- `target/allure-results/deterministic/` or `target/allure-results/live/`: native results, fixture containers, approved environment metadata and PNG attachments.
- `target/surefire-reports/<lane>/`: TestNG/Surefire results and original stack traces.
- `target/diagnostics/<lane>/`: bounded fallback PNGs only if the Allure attachment operation fails.

`clean` removes both lanes' previous build output. To retain both fresh lanes locally, first run deterministic `clean verify`, then an authorized live `verify` without `clean`; do this only when no previous live results remain. Repeating a lane without cleaning can accumulate Allure UUID files. Reports are not generated, hosted, or published in this milestone.

## Troubleshooting

The initial live run's three failures occurred in `driver.get("https://automationexercise.com/products")`, before the explicit heading wait. All three screenshots showed catalogue content; two showed advertising overlays. The former `NORMAL` strategy coupled test entry to full document completion even though the required content had rendered. `EAGER` narrows that navigation barrier while preserving the 30-second bound, explicit application waits and assertions. The initial evidence contains no network waterfall or readiness trace, so the exact delayed resource and its first-/third-party ownership cannot be established retrospectively. A successful local run does not establish that all external resources will always complete promptly.

The Windows-only `windows-local-ipc` profile sets `jdk.net.unixdomain.tmpdir=target` for the forked test JVM. During implementation, Java's default Windows temporary socket location failed with `Unable to establish loopback connection` / `UnixDomainSockets.connect0: Invalid argument`. Both the local HTTP fixture and Selenium's Java HTTP client were affected. A selector-provider override did not fix it; using build output for socket scratch files did. The checkout must be outside `%LOCALAPPDATA%`: a copy beneath that directory leaves its relative `target` socket directory inside the affected location. This also applies to isolated validation copies; do not place them under the default Windows temporary directory. This profile changes no machine environment settings and is inactive on Linux. It is a documented compatibility accommodation, not a network security control.

Selenium 4.49.0 may warn that Chrome 154 is newer than its exact CDP bindings. Tests use WebDriver, not CDP; assess actual test outcomes instead of suppressing warnings or adding devtools dependencies speculatively. An SLF4J no-provider warning is also possible; required failure diagnostics use explicit bounded messages and TestNG retains original errors.

An unavailable site, ad overlay, consent screen or changed catalogue is a live failure, not a reason to retry automatically, bypass controls or silently weaken assertions. Investigate the screenshot and authorize only a focused correction if needed.

## Dependency provenance

Verified on 2026-10-05 and resolved locally: Maven 3.10.0, wrapper 3.3.4, Selenium 4.49.0, TestNG 7.12.0, Allure TestNG 3.0.0; Compiler 3.16.0, Surefire 3.5.5, Enforcer 3.6.3, Clean 3.5.0, Resources 3.5.0, Jar 3.5.1; Spotless 3.10.3 and google-java-format 1.36.1.

Sources: [Maven release](https://maven.apache.org/docs/3.10.0/release-notes.html), [Apache plugins](https://maven.apache.org/plugins/), [Surefire 3.5.5](https://maven.apache.org/surefire-archives/surefire-3.5.5/maven-surefire-plugin/examples/testng.html), [Selenium](https://www.selenium.dev/downloads/), [TestNG](https://github.com/testng-team/testng/releases), [Allure Java](https://github.com/allure-framework/allure-java/releases), [Spotless](https://github.com/diffplug/spotless/blob/main/plugin-maven/CHANGES.md), [formatter releases](https://github.com/google/google-java-format/releases). Dependency coordinates were checked in [Maven Central](https://central.sonatype.com/).

Surefire 3.5.5 deliberately uses the direct TestNG provider; 3.6.0 changes that execution boundary to the JUnit Platform. Allure 3 uses `Allure.attachment` with `AttachmentOptions`, rather than Allure 2's static `addAttachment` API. No reporting downgrade or AspectJ was needed.

The wrapper ZIP was verified against the [official SHA-512](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.10.0/apache-maven-3.10.0-bin.zip.sha512), then its SHA-256 was pinned in `.mvn/wrapper/maven-wrapper.properties`. The scripts verify downloads; they do not rehash an already extracted cached distribution on every invocation.

## Future commit preparation

When staging is separately authorized, preserve the wrapper executable bit with `git add --chmod=+x mvnw`. Do not stage during review-only passes. Review SHA-256 manifests describe exact on-disk bytes; Git may normalize CRLF/LF according to .gitattributes, so committed blob bytes and hashes can differ.
