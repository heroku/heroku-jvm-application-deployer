package com.heroku.sdk.deploy.lib;

import com.heroku.sdk.deploy.util.PathUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class PathUtilsTest {
    private Path basePath = Paths.get("/home/user/projects/project");
    private Path existingBasePath;

    @Test
    public void testNormalizeAll() {
        ArrayList<Path> pathsToNormalize = new ArrayList<>();
        ArrayList<Path> expectedNormalizedPaths = new ArrayList<>();

        pathsToNormalize.add(Paths.get("README.md"));
        expectedNormalizedPaths.add(Paths.get("README.md"));

        pathsToNormalize.add(Paths.get("docs//HACKING.md"));
        expectedNormalizedPaths.add(Paths.get("docs/HACKING.md"));

        pathsToNormalize.add(Paths.get("target/dependencies/../project.war"));
        expectedNormalizedPaths.add(Paths.get("target/project.war"));

        pathsToNormalize.add(Paths.get("/home/user/projects/project/target/dependencies/dep1.jar"));
        expectedNormalizedPaths.add(Paths.get("target/dependencies/dep1.jar"));

        pathsToNormalize.add(Paths.get("../../projects/project/start.sh"));
        expectedNormalizedPaths.add(Paths.get("start.sh"));

        pathsToNormalize.add(Paths.get("../README.md"));
        // We expect this to be filtered out, hence no expectedNormalizedPaths entry here.

        List<Path> normalizedPaths = PathUtils.normalizeAll(basePath, pathsToNormalize);
        assertEquals(expectedNormalizedPaths, normalizedPaths);
    }

    @Test
    public void testIsValidPath() {
        assertFalse(PathUtils.isValidPath(basePath, Paths.get("/home/user/projects/project/../foo.txt")));
        assertFalse(PathUtils.isValidPath(basePath, Paths.get("../foo.txt")));
        assertFalse(PathUtils.isValidPath(basePath, Paths.get("/foo.txt")));
        assertTrue(PathUtils.isValidPath(basePath, Paths.get("foo.txt")));
        assertTrue(PathUtils.isValidPath(basePath, Paths.get("files/important/bar.txt")));
    }

    @Test
    public void testNormalize() {
        assertEquals(Paths.get("foo.sh"), PathUtils.normalize(basePath, Paths.get("foo.sh")));
        assertEquals(Paths.get("target/webapp.war"), PathUtils.normalize(basePath, Paths.get("target//./webapp.war")));
        assertEquals(Paths.get("bar.jar"), PathUtils.normalize(basePath, Paths.get("../project/bar.jar")));
    }

    @Before
    public void setUp() throws IOException {
        existingBasePath = Files.createTempDirectory("heroku-deploy-test");
        Files.createFile(existingBasePath.resolve("test"));
        Files.createFile(existingBasePath.resolve("foo"));
        Files.createFile(existingBasePath.resolve("bar"));

        Path targetDirectory = Files.createDirectory(existingBasePath.resolve("target"));
        Files.createFile(targetDirectory.resolve("app.war"));
        Files.createFile(targetDirectory.resolve("app.jar"));

        Path targetDependenciesDirectory = Files.createDirectory(targetDirectory.resolve("dependencies"));
        Files.createFile(targetDependenciesDirectory.resolve("junit.jar"));
    }

    @After
    public void tearDown() throws IOException {
        Files.walk(existingBasePath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        existingBasePath = null;
    }

    @Test
    public void testExpand() throws IOException {
        ArrayList<Path> expectedForAll = new ArrayList<>();
        expectedForAll.add(Paths.get("test"));
        expectedForAll.add(Paths.get("target/app.jar"));
        expectedForAll.add(Paths.get("target/dependencies/junit.jar"));
        expectedForAll.add(Paths.get("target/app.war"));
        expectedForAll.add(Paths.get("foo"));
        expectedForAll.add(Paths.get("bar"));

        assertEquals(expectedForAll, PathUtils.expandDirectory(existingBasePath, Paths.get(".")));

        ArrayList<Path> expectedForTarget = new ArrayList<>();
        expectedForTarget.add(Paths.get("target/app.jar"));
        expectedForTarget.add(Paths.get("target/dependencies/junit.jar"));
        expectedForTarget.add(Paths.get("target/app.war"));

        assertEquals(expectedForTarget, PathUtils.expandDirectory(existingBasePath, Paths.get("target")));

        assertEquals(expectedForTarget, PathUtils.expandDirectory(existingBasePath, Paths.get("././/weird/../target")));

        assertEquals(Collections.singletonList(Paths.get("foo")), PathUtils.expandDirectory(existingBasePath, Paths.get("foo")));
    }
}
