package com.heroku.deployer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import com.heroku.deployer.resolver.ApiKeyResolver;
import com.heroku.deployer.util.*;
import com.heroku.deployer.deployment.Deployer;
import com.heroku.deployer.deployment.DeploymentDescriptor;
import com.heroku.deployer.resolver.WebappRunnerResolver;
import com.heroku.deployer.sourceblob.SourceBlobDescriptor;
import com.heroku.deployer.sourceblob.SourceBlobPackager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "heroku-jvm-application-deployer", mixinStandardHelpOptions = true,
        description = "Application for deploying Java applications to Heroku.",
        defaultValueProvider = DefaultValueProvider.class)
public class Main implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1", paramLabel = "file", description = "The JAR or WAR file to deploy.")
    private Optional<Path> mainFile = Optional.empty();

    @Option(names = {"-a", "--app"}, paramLabel = "name", description = "The name of the Heroku app to deploy to. Defaults to app name from git remote.")
    private Optional<String> appName = Optional.empty();

    @Option(names = {"-b", "--buildpack"}, arity = "*", paramLabel = "buildpack", defaultValue = "heroku/jvm", description = "Defaults to ${DEFAULT-VALUE}, can be passed multiple times to use multiple buildpacks.")
    private List<String> buildpacks = new ArrayList<>();

    @Option(names = {"--webapp-runner-version"}, paramLabel = "version", description = "The version of webapp-runner to use. Defaults to the most recent version (${DEFAULT-VALUE}).")
    private String webappRunnerVersion;

    @Option(names = {"--jar-opts"}, paramLabel = "options", description = "Add command line options for when the JAR is run.")
    private Optional<String> jarFileOpts = Optional.empty();

    @Option(names = {"-j", "--jdk"}, paramLabel = "string", description = "Set the Heroku JDK selection string for the app (i.e. 17, 21.0.1).")
    private Optional<String> jdkString = Optional.empty();

    @Option(names = {"-d", "--disable-auto-includes"}, description = "Disable automatic inclusion of certain files.", defaultValue = "false")
    private boolean disableAutoIncludes = false;

    @Option(names = {"-i", "--include"}, arity = "*", paramLabel = "path", description = "Additional files or directories to include, can be passed multiple times to include multiple files and directories.")
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

        if (mainFile.isPresent()) {
            includedPaths.add(projectDirectory.resolve(mainFile.get()).normalize());

            Optional<String> mainFileExtension = PathUtils.getFileExtension(mainFile.get());
            if (mainFileExtension.isPresent() && mainFileExtension.get().equalsIgnoreCase("war")) {
                System.out.printf("-----> Downloading webapp-runner %s...\n", webappRunnerVersion);
                Path webappRunnerPath = FileDownloader.download(WebappRunnerResolver.getUrlForVersion(webappRunnerVersion));
                sourceBlobDescriptor.addLocalPath(WEBAPP_RUNNER_SOURCE_BLOB_PATH, webappRunnerPath, false);
            }
        } else {
            System.out.printf("-----> No main JAR/WAR file specified, skipping Procfile generation and webapp-runner...\n", webappRunnerVersion);
        }

        if (!disableAutoIncludes) {
            List<Path> defaultPaths = Arrays.asList(
                Paths.get("Procfile"),
                Paths.get("system.properties"),
                Paths.get(".jdk-overlay"),
                Paths.get("project.toml")
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

        // Add an auto-generated project-toml to the source blob if no project.toml has been added yet.
        Path projectTomlPath = Paths.get("project.toml");
        if (!sourceBlobDescriptor.containsPath(projectTomlPath) && HerokuCli.runIsCnb(projectDirectory, appName)) {
            String defaultProjectToml = IOUtils.toString(getClass().getResourceAsStream("/default-project.toml"), StandardCharsets.UTF_8);
            sourceBlobDescriptor.addSyntheticFile(projectTomlPath, defaultProjectToml, true);
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

        Path sourceBlobArchive = SourceBlobPackager.pack(sourceBlobDescriptor);
        String sourceBlobArchiveSha256 = DigestUtils.sha256Hex(Files.newInputStream(sourceBlobArchive));

        DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor(
                appName.get(),
                buildpacks,
                Collections.emptyMap(),
                sourceBlobArchive,
                GitUtils.getHeadCommitHash(projectDirectory).orElse("nogit-" + sourceBlobArchiveSha256));


        String version = PropertiesUtils
                .loadPomPropertiesOrEmptyFromClasspath(Main.class, "com.heroku", "heroku-jvm-application-deployer")
                .getProperty("version", "unknown");

        boolean deploySuccessful = Deployer.deploy(
                apiKey.get(),
                "heroku-jvm-application-deployer",
                version,
                deploymentDescriptor);

        if (deploySuccessful) {
            return 0;
        } else {
            return -1;
        }
    }

    private Optional<Procfile> generateProcfile() {
        if (!mainFile.isPresent()) {
            return Optional.empty();
        }

        final Path projectDirectory = Paths.get(System.getProperty("user.dir"));

        return PathUtils.getFileExtension(mainFile.get()).flatMap(extension -> {
            // We fall back to an empty string if the path cannot be normalized. This will result in a
            // user-readable error from JvmProjectSourceBlobCreator and the Procfile will never be deployed.
            String normalizedMainFile = PathUtils.normalize(projectDirectory, mainFile.get())
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
}
