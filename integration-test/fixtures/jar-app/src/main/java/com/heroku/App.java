package com.heroku;

import io.undertow.Undertow;
import io.undertow.util.Headers;

import java.util.Optional;

public class App {
    public static void main(String[] args) {
        int port = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));
        System.out.printf("Starting on port %d...\n", port);

        Undertow server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("Hello World!");
                }).build();

        server.start();
    }
}
