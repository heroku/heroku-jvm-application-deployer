package com.heroku.sdk.deploy.lib.resolver;

import com.heroku.sdk.deploy.util.HerokuCli;
import org.eclipse.jgit.transport.NetRC;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class ApiKeyResolver {

    public static Optional<String> resolve(Path directory) throws IOException {
        Optional<String> environmentApiKey = readApiKeyFromEnvironment();
        if (environmentApiKey.isPresent()) {
            return environmentApiKey;
        }

        Optional<String> netRcPassword = ApiKeyResolver.readPasswordFromNetRc();
        if (netRcPassword.isPresent()) {
            return netRcPassword;
        }

        return HerokuCli.runAuthToken(directory);
    }

    private static Optional<String> readPasswordFromNetRc() {
        // Reads, depending on operating system, the users default netrc file
        NetRC netrc = new NetRC();

        return Optional
                .ofNullable(netrc.getEntry("api.heroku.com"))
                .flatMap(entry -> Optional.ofNullable(entry.password))
                .map(String::valueOf);
    }

    private static Optional<String> readApiKeyFromEnvironment() {
        return Optional
            .ofNullable(System.getenv("HEROKU_API_KEY"))
            .filter(apiKey -> !apiKey.trim().isEmpty());
    }
}

