package com.heroku.sdk.deploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.heroku.api.Formation;
import org.junit.After;
import org.junit.Test;

/**
 * @author Joe Kutner on 10/26/17.
 *         Twitter: @codefinger
 */
public class DeployWarTest extends BaseDeployTest {

  @After
  public void clearSysProps() {
    System.clearProperty("heroku.warFile");
  }

  @Test
  public void testSimpleWar() throws Exception {
    setSimpleWar();

    DeployWar.main(new String[]{});
    Thread.sleep(3000);

    assertEquals(3, this.api.listReleases(this.appName).size());
    Formation f = this.api.listFormation(this.appName).get(0);
    assertEquals(1, f.getQuantity());
    assertEquals("web", f.getType());
    assertTrue(f.getCommand().startsWith("java $JAVA_OPTS -jar webapp-runner.jar"));
  }

  @Test
  public void testSimpleWarWithCustomWebappRunnerVersion() throws Exception {
    setSimpleWar();
    System.setProperty("heroku.webappRunnerVersion", "8.0.47.0");

    DeployWar.main(new String[]{});
    Thread.sleep(3000);

    assertEquals(3, this.api.listReleases(this.appName).size());
    Formation f = this.api.listFormation(this.appName).get(0);
    assertEquals(1, f.getQuantity());
    assertEquals("web", f.getType());
    assertTrue(f.getCommand().startsWith("java $JAVA_OPTS -jar webapp-runner.jar"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithoutWarfile() throws Exception {
    DeployWar.main(new String[]{});
  }

  private void setSimpleWar() {
    String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
    String jarFile = relPath + "../../src/test/resources/sample-war.war";
    System.setProperty("heroku.warFile", jarFile);
  }
}
