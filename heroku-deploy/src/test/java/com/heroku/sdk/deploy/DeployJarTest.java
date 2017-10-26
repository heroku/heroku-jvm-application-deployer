package com.heroku.sdk.deploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.heroku.api.BuildpackInstallation;
import com.heroku.api.Formation;
import com.heroku.sdk.deploy.endpoints.Builds;
import org.junit.After;
import org.junit.Test;

/**
 * @author Joe Kutner on 10/26/17.
 *         Twitter: @codefinger
 */
public class DeployJarTest extends BaseDeployTest {

  @After
  public void clearSysProps() {
    System.clearProperty("heroku.jarFile");
    System.clearProperty("heroku.jarOpts");
  }

  @Test
  public void testSimpleJar() throws Exception {
    setSimpleJar();

    DeployJar.deploy();

    assertEquals(3, this.api.listReleases(this.appName).size());

    List<BuildpackInstallation> buildpacks = this.api.listBuildpackInstallations(this.appName);
    assertEquals("heroku/jvm", buildpacks.get(0).getBuildpack().getName());
    Formation f = this.api.listFormation(this.appName).get(0);

    assertEquals(1, f.getQuantity());
    assertEquals("web", f.getType());
    assertTrue(f.getCommand().startsWith("java $JAVA_OPTS -jar"));
  }

  @Test
  public void testSimpleJarWithCustomOpts() throws Exception {
    System.setProperty("heroku.jarOpts", "-Dfoo=bar");
    setSimpleJar();

    DeployJar.deploy();

    assertEquals(3, this.api.listReleases(this.appName).size());
    Formation f = this.api.listFormation(this.appName).get(0);
    assertEquals(1, f.getQuantity());
    assertEquals("web", f.getType());
    assertTrue("Did not set custom jarOpts", f.getCommand().endsWith("-Dfoo=bar $JAR_OPTS"));
  }

  @Test
  public void testSimpleJarWithCustomBuildpacks() throws Exception {
    System.setProperty("heroku.buildpacks", "heroku/exec,heroku/jvm");
    setSimpleJar();

    DeployJar.deploy();

    assertEquals(3, this.api.listReleases(this.appName).size());

    List<BuildpackInstallation> buildpacks = this.api.listBuildpackInstallations(this.appName);
    assertEquals("heroku/exec", buildpacks.get(0).getBuildpack().getName());
    assertEquals("heroku/jvm", buildpacks.get(1).getBuildpack().getName());

    Formation f = this.api.listFormation(this.appName).get(0);
    assertEquals(1, f.getQuantity());
    assertEquals("web", f.getType());
  }

  @Test
  public void testSimpleJarWithPresetBuildpacks() throws Exception {
    this.api.updateBuildpackInstallations(this.appName, Arrays.asList(Builds.METRICS_BUILDPACK_URL, "heroku/jvm"));

    setSimpleJar();

    DeployJar.deploy();

    assertEquals(3, this.api.listReleases(this.appName).size());

    List<BuildpackInstallation> buildpacks = this.api.listBuildpackInstallations(this.appName);
    assertEquals("heroku/metrics", buildpacks.get(0).getBuildpack().getName());
    assertEquals("heroku/jvm", buildpacks.get(1).getBuildpack().getName());

    Formation f = this.api.listFormation(this.appName).get(0);
    assertEquals(1, f.getQuantity());
    assertEquals("web", f.getType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSimpleJarWithoutJvmBuildpack() throws Exception {
    this.api.updateBuildpackInstallations(this.appName, Collections.singletonList("heroku/java"));

    setSimpleJar();

    DeployJar.deploy();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithoutJarfile() throws Exception {
    DeployJar.deploy();
  }

  private void setSimpleJar() {
    String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
    String jarFile = relPath + "../../src/test/resources/sample-jar.jar";
    System.setProperty("heroku.jarFile", jarFile);
  }
}
