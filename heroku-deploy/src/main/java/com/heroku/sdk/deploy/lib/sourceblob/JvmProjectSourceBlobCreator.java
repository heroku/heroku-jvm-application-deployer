package com.heroku.sdk.deploy.lib.sourceblob;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.resolver.JdkVersionResolver;
import com.heroku.sdk.deploy.lib.resolver.ProcfileResolver;
import com.heroku.sdk.deploy.util.PathUtils;
import com.heroku.sdk.deploy.util.Procfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class JvmProjectSourceBlobCreator {
    /**
     * Creates a SourceBlobDescriptor based on a given project directory and custom resolvers. It assumes a JVM
     * application.
     *
     * Projects using heroku-deploy should stick to a core source blob that is consistent across, for example, build
     * tool plugins for Maven, sbt and others. This method takes a project directory and extracts common information
     * like process types, JDK versions and overlays from it.
     *
     * It will create synthetic a Procfile, system.properties and .heroku-deploy metadata file. In many cases, those
     * will not be the actual files found in the project directory. See the specific resolvers where specific values are
     * read from and in which order.
     *
     * The returned SourceBlobDescriptor can then be enhanced with specific files that, for example, resulted in a
     * build process.
     *
     * @param projectDirectory         Absolute path to the project directory.
     * @param clientString             The client string of the plugin/tool that will upload the source blob. It will be
     *                                 written to the .heroku-deploy metadata file.
     * @param includedProjectFiles     Paths (relative to the project directory) for inclusion into the source blob.
     *                                 They are treated as user-input and will be validated and normalized prior to
     *                                 inclusion. User-readable errors are written to the given outputAdapter.
     * @param customProcfileResolver   A supplier that is called to add additional procfile entries on top of the ones
     *                                 specified in the Procfile contained in the project directory.
     *                                 Used with ProcfileResolver.
     * @param defaultProcfile          If no process types are defined in the projects Procfile and
     *                                 customProcfileResolver, this procfile will be included in the source blob.
     * @param customJdkVersionResolver A supplier that is called to get the customers desired JDK version.
     *                                 Used for JdkVersionResolver.
     * @param outputAdapter            TODO
     * @return A SourceBlobDescriptor valid for any JVM project that can be extended with actual jar/war files later.
     * @throws IOException Resolving metadata involves IO. In case those operations fail, an IOException will be thrown.
     * @throws IllegalArgumentException If the given projectDirectory is not an absolute path.
     *
     * @see JdkVersionResolver
     * @see ProcfileResolver
     */
    public static SourceBlobDescriptor create(Path projectDirectory,
                                              String clientString,
                                              List<Path> includedProjectFiles,
                                              Supplier<Procfile> customProcfileResolver,
                                              Procfile defaultProcfile,
                                              Supplier<Optional<String>> customJdkVersionResolver,
                                              OutputAdapter outputAdapter)
            throws IOException, IllegalArgumentException {

        if (!projectDirectory.isAbsolute()) {
            throw new IllegalArgumentException("projectDirectory must be an absolute path!");
        } else {
            projectDirectory = projectDirectory.normalize();
        }

        SourceBlobDescriptor sourceBlobDescriptor = new SourceBlobDescriptor();

        sourceBlobDescriptor.addSyntheticFile(".heroku-deploy", String.format("client=%s", clientString), true);

        Procfile procfile = ProcfileResolver.resolve(projectDirectory, customProcfileResolver);

        if (procfile.isEmpty()) {
            sourceBlobDescriptor.addSyntheticFile("Procfile", defaultProcfile.asString(), true);
        } else {
            sourceBlobDescriptor.addSyntheticFile("Procfile", procfile.asString(), true);
        }

        JdkVersionResolver.resolve(projectDirectory, customJdkVersionResolver).ifPresent(version -> {
            sourceBlobDescriptor.addSyntheticFile(
                    "system.properties",
                    String.format("java.runtime.version=%s", version),
                    true);
        });

        Path jdkOverlayPath = projectDirectory.resolve(".jdk-overlay");
        if (Files.isDirectory(jdkOverlayPath)) {
            for (Path normalizedSubPath : PathUtils.expandDirectory(projectDirectory, jdkOverlayPath)) {
                sourceBlobDescriptor.addLocalPath(normalizedSubPath, projectDirectory.resolve(normalizedSubPath),false);
            }
        }

        for (Path normalized : PathUtils.normalizeAll(projectDirectory, includedProjectFiles)) {
            if (normalized.equals(Paths.get("Procfile"))) {
                outputAdapter.logWarn("Procfile was explicitly included! It will take precedence over any other configured process types.");
            }

            if (normalized.equals(Paths.get("system.properties"))) {
                outputAdapter.logWarn("system.properties explicitly included! It will take precedence over any other JDK version configuration.");
            }

            for (Path normalizedSubPath : PathUtils.expandDirectory(projectDirectory, normalized)) {
                sourceBlobDescriptor.addLocalPath(normalizedSubPath, projectDirectory.resolve(normalizedSubPath),false);
            }
        }

        return sourceBlobDescriptor;
    }
}
