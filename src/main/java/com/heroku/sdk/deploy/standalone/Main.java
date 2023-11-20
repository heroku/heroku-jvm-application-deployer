package com.heroku.sdk.deploy.standalone;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.deploymemt.Deployer;
import com.heroku.sdk.deploy.lib.deploymemt.DeploymentDescriptor;
import com.heroku.sdk.deploy.lib.resolver.ApiKeyResolver;
import com.heroku.sdk.deploy.lib.resolver.WebappRunnerResolver;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobDescriptor;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobPackager;
import com.heroku.sdk.deploy.util.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "heroku-jvm-application-deployer", mixinStandardHelpOptions = true,
        description = "Application for deploying Java applications to Heroku.",
        defaultValueProvider = DefaultValueProvider.class)
public class Main implements Callable<Integer> {
    @Parameters(index = "0", description = "The JAR or WAR file to deploy.")
    private Path mainFile;

    @Option(names = {"-a", "--app"}, description = "The name of the Heroku app to deploy to.")
    private Optional<String> appName = Optional.empty();

    @Option(names = {"-b", "--buildpack"}, arity = "*", defaultValue = "heroku/jvm", description = "")
    private List<String> buildpacks = new ArrayList<>();

    @Option(names = {"--webapp-runner-version"}, description = "The version of webapp-runner to use. Defaults to the most recent version (${DEFAULT-VALUE}).")
    private String webappRunnerVersion;

    @Option(names = {"--jar-opts"}, description = "")
    private Optional<String> jarFileOpts = Optional.empty();

    @Option(names = {"-j", "--jdk"}, description = "")
    private Optional<String> jdkString = Optional.empty();

    @Option(names = {"--disable-auto-includes"}, description = "", defaultValue = "false")
    private boolean disableAutoIncludes = false;

    @Option(names = {"-i", "--include"}, arity = "*", description = "Additional files or directories to include.")
    private List<Path> includedPaths = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        System.out.println("Heroku JVM Application Deployer");
        System.out.println();

        if (!appName.isPresent()) {
            System.err.println("Error: app name not set. Use --app to set the Heroku app.");
            System.exit(-1);
        }

        final Path projectDirectory = Paths.get(System.getProperty("user.dir"));

        final Optional<String> apiKey = ApiKeyResolver.resolve(projectDirectory);
        if (!apiKey.isPresent()) {
            System.err.println("Error: Heroku API key could not be found!");
            System.err.println("Set it via the HEROKU_API_KEY environment variable or ensure you're logged in Heroku CLI (e.g. heroku auth:whoami).");
            System.exit(-1);
        }

        SourceBlobDescriptor sourceBlobDescriptor = new SourceBlobDescriptor();
        includedPaths.add(projectDirectory.resolve(mainFile).normalize());

        Optional<String> mainFileExtension = PathUtils.getFileExtension(mainFile);
        if (mainFileExtension.isPresent() && mainFileExtension.get().equalsIgnoreCase("war")) {
            System.out.printf("-----> Downloading webapp-runner %s...\n", webappRunnerVersion);
            Path webappRunnerPath = FileDownloader.download(WebappRunnerResolver.getUrlForVersion(webappRunnerVersion));
            sourceBlobDescriptor.addLocalPath(WEBAPP_RUNNER_SOURCE_BLOB_PATH, webappRunnerPath, false);
        }

        if (!disableAutoIncludes) {
            List<Path> defaultPaths = Arrays.asList(
                Paths.get("Procfile"),
                Paths.get("system.properties"),
                Paths.get(".jdk-overlay")
            );

            for (Path defaultPath : defaultPaths) {
                Path resolvedDefaultPath = projectDirectory.resolve(defaultPath);
                if (Files.exists(resolvedDefaultPath)) {
                    System.out.printf("-----> Automatically including %s, disable with --disable-auto-includes\n", defaultPath);
                    includedPaths.add(resolvedDefaultPath);
                }
            }
        }

        // Add files to source blob descriptor, expanding directory paths if necessary.
        for (Path includedPath : includedPaths) {
            if (Files.isDirectory(includedPath)) {
                try (Stream<Path> paths = Files.walk(includedPath)) {
                    paths
                        .filter(Files::isRegularFile)
                        .map(projectDirectory::resolve)
                        .forEach(path -> {
                            Optional<Path> normalizedPath = PathUtils.normalize(projectDirectory, path);

                            if (normalizedPath.isPresent()) {
                                sourceBlobDescriptor.addLocalPath(normalizedPath.get(), path, false);
                            } else {
                                System.err.printf("Error: can't include path '%s': normalization failed!\n", includedPath);
                                System.exit(-1);
                            }
                        });
                }
            } else if (Files.isRegularFile(includedPath)) {
                Optional<Path> normalizedPath = PathUtils.normalize(projectDirectory, includedPath);

                if (normalizedPath.isPresent()) {
                    sourceBlobDescriptor.addLocalPath(normalizedPath.get(), includedPath, false);
                } else {
                    System.err.printf("Error: can't include path '%s': normalization failed!\n", includedPath);
                    System.exit(-1);
                }
            } else if (!Files.exists(includedPath)) {
                System.err.printf("Error: can't include path '%s': not found!\n", includedPath);
                System.exit(-1);
            } else {
                System.err.printf("Error: can't include path '%s'. Only existing regular files and directories are supported!\n", includedPath);
                System.exit(-1);
            }
        }

        // Add an auto-generated Procfile to the source blob if no Procfile has been added yet.
        Path procfilePath = Paths.get("Procfile");
        if (sourceBlobDescriptor.containsPath(procfilePath)) {
            if (jarFileOpts.isPresent()) {
                System.out.println("Warning: Procfile exists, --jar-opts will have no effect.");
            }
        } else {
            Procfile defaultProcfile = generateProcfile().orElse(Procfile.empty());
            sourceBlobDescriptor.addSyntheticFile(procfilePath, defaultProcfile.asString(), true);
        }

        // Add an auto-generated system.properties to the source blob if no system.properties has been added yet.
        if (jdkString.isPresent()) {
            Path systemPropertiesPath = Paths.get("system.properties");
            if (sourceBlobDescriptor.containsPath(systemPropertiesPath)) {
                System.out.println("Warning: system.properties file exists, -j/--jdk will have no effect.");
            } else {
                sourceBlobDescriptor.addSyntheticFile(
                    "system.properties",
                    String.format("java.runtime.version=%s", jdkString.get()),
                    true
                );
            }
        }

        Path sourceBlobArchive = SourceBlobPackager.pack(sourceBlobDescriptor, OUTPUT_ADAPTER);

        DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor(
                appName.get(),
                buildpacks,
                Collections.emptyMap(),
                sourceBlobArchive,
                GitUtils.getHeadCommitHash(projectDirectory).orElse("unknown"));


        String version = PropertiesUtils
                .loadPomPropertiesOrEmptyFromClasspath(Main.class, "com.heroku", "heroku-jvm-application-deployer")
                .getProperty("version", "unknown");

        boolean deploySuccessful = Deployer.deploy(
                apiKey.get(),
                "heroku-jvm-application-deployer",
                version,
                deploymentDescriptor,
                OUTPUT_ADAPTER);

        if (deploySuccessful) {
            return 0;
        } else {
            return -1;
        }
    }

    private Optional<Procfile> generateProcfile() {
        final Path projectDirectory = Paths.get(System.getProperty("user.dir"));

        return PathUtils.getFileExtension(mainFile).flatMap(extension -> {
            // We fall back to an empty string if the path cannot be normalized. This will result in a
            // user-readable error from JvmProjectSourceBlobCreator and the Procfile will never be deployed.
            String normalizedMainFile = PathUtils.normalize(projectDirectory, mainFile)
                    .map(PathUtils::separatorsToUnix)
                    .orElse("");

            if (extension.equalsIgnoreCase("jar")) {
                return Optional.of(Procfile.singleton("web", String.format(
                        "java $JAVA_OPTS -jar %s %s $JAR_OPTS",
                        normalizedMainFile,
                        jarFileOpts.orElse("")
                )));
            } else if (extension.equalsIgnoreCase("war")) {
                return Optional.of(Procfile.singleton("web", String.format(
                        "java $JAVA_OPTS -jar %s $WEBAPP_RUNNER_OPTS --port $PORT %s",
                        WEBAPP_RUNNER_SOURCE_BLOB_PATH,
                        normalizedMainFile
                )));
            }

            return Optional.empty();
        });
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    private static final Path WEBAPP_RUNNER_SOURCE_BLOB_PATH = Paths.get(".heroku/webapp-runner.jar");
    private static final OutputAdapter OUTPUT_ADAPTER = new StdOutOutputAdapter(true);
}
