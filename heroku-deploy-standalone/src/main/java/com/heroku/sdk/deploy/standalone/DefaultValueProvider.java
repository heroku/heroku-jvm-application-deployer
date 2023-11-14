package com.heroku.sdk.deploy.standalone;

import com.heroku.sdk.deploy.lib.resolver.AppNameResolver;
import com.heroku.sdk.deploy.lib.resolver.WebappRunnerResolver;
import picocli.CommandLine;
import java.nio.file.Paths;
import java.util.Optional;

public class DefaultValueProvider implements CommandLine.IDefaultValueProvider {
    @Override
    public String defaultValue(CommandLine.Model.ArgSpec argSpec) throws Exception {

        if (argSpec instanceof CommandLine.Model.OptionSpec) {
            CommandLine.Model.OptionSpec optionSpec = (CommandLine.Model.OptionSpec) argSpec;

            if (optionSpec.shortestName().equalsIgnoreCase("-a")) {
                Optional<String> appName = AppNameResolver.resolveViaHerokuGitRemote(Paths.get(System.getProperty("user.dir")));
                if (appName.isPresent()) {
                    return appName.get();
                }
            }

            if (optionSpec.shortestName().equalsIgnoreCase("--webapp-runner-version")) {
                return WebappRunnerResolver.getLatestVersion();
            }
        }

        return null;
    }
}
