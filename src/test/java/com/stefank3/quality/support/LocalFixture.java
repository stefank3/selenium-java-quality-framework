package com.stefank3.quality.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

/** One loopback-only HTML fixture with no external resources or application simulation. */
public final class LocalFixture implements AutoCloseable {
  private final HttpServer server;

  /** Binds an ephemeral loopback port and serves the maintained synthetic document. */
  public LocalFixture() throws IOException {
    byte[] document;
    try (var input = LocalFixture.class.getResourceAsStream("/fixtures/lifecycle.html")) {
      if (input == null) {
        throw new IOException("Missing local fixture");
      }
      document = input.readAllBytes();
    }
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try (exchange) {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange
                .getResponseHeaders()
                .set(
                    "Content-Security-Policy",
                    "default-src 'none'; script-src 'unsafe-inline'; img-src data:; connect-src 'none'");
            exchange.sendResponseHeaders(200, document.length);
            exchange.getResponseBody().write(document);
          }
        });
    server.start();
  }

  /** Returns the allocated fixture origin, never a user-supplied address. */
  public URI origin() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  /** Stops the server immediately, including after a failed browser setup. */
  @Override
  public void close() {
    server.stop(0);
  }
}
