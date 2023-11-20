package com.heroku.deployer.resolver;

import com.heroku.deployer.util.Procfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

public class ProcfileResolver {

    public static Procfile resolve(Path projectDirectory, Supplier<Procfile> customResolver) throws IOException {
        Procfile procfile = Procfile.fromFile(projectDirectory.resolve("Procfile"));
        return procfile.merge(customResolver.get());
    }
}
