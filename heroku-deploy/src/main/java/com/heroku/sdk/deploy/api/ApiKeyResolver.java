package com.heroku.sdk.deploy.api;

import com.heroku.sdk.deploy.util.HerokuCli;
import org.eclipse.jgit.transport.NetRC;

import java.io.IOException;
import java.nio.file.Path;

import java.util.Optional;

public class ApiKeyResolver {

    public static Optional<String> resolve(Path directory) throws IOException {
        Optional<String> value;

        value = Optional
                .ofNullable(System.getenv("HEROKU_API_KEY"))
                .filter(apiKey -> !apiKey.trim().isEmpty());

        if (value.isPresent()) {
            return value;
        }

        value = ApiKeyResolver.readPasswordFromNetRc();
        if (value.isPresent()) {
            return value;
        }

        value = HerokuCli.runAuthToken(directory);
        return value;
    }

    private static Optional<String> readPasswordFromNetRc() {
        NetRC netrc = new NetRC(); // Reads, depending on operating system, the users default netrc file

        return Optional
                .ofNullable(netrc.getEntry("api.heroku.com"))
                .flatMap(entry -> Optional.ofNullable(entry.password))
                .map(String::valueOf);
    }
}

