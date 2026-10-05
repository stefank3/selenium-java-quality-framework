# Selenium and Playwright trade-offs

This repository deliberately demonstrates Java/Selenium/TestNG skills. Tool choice should follow product constraints, team experience and the evidence needed.

| Concern | Selenium in this repository | Playwright alternative |
| --- | --- | --- |
| Ecosystem | Java, Maven and TestNG ownership is explicit | Java bindings exist; the Node.js Playwright Test runner offers a different integrated workflow |
| Synchronization | Explicit WebDriverWait conditions make state transitions visible | Locator actions include actionability checks and auto-waiting; business assertions still require thought |
| Isolation | A fresh ChromeDriver session for each invocation | Browser contexts provide lightweight isolated sessions |
| Browser management | Selenium Manager matches the installed Chrome driver | Playwright distributes browser builds matched to its release |
| Diagnostics | TestNG/Allure plus a bounded failure screenshot | Tracing, screenshots and other tooling can reduce custom diagnostic work |
| Scale | WebDriver/Grid is an established remote-browser model; Grid is out of scope here | Parallel workers/contexts are available in Playwright Test; scaling still needs isolation and resource planning |

Neither tool removes the need for meaningful assertions, stable selectors, controlled data or careful external-site execution. Do not compare this small sequential framework to every capability of the Playwright Node runner as though both were equally configured.

Official references, checked 2026-10-05: [Selenium waits](https://www.selenium.dev/documentation/webdriver/waits/), [Selenium Manager](https://www.selenium.dev/documentation/selenium_manager/), [Playwright Java](https://playwright.dev/java/), [Playwright actionability](https://playwright.dev/java/docs/actionability), [Playwright isolation](https://playwright.dev/java/docs/browser-contexts).
