package com.heroku.sdk.deploy;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Joe Kutner on 10/26/17.
 *         Twitter: @codefinger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  DeployJarTest.class,
  DeployWarTest.class
})
public class DeployTestSuite {
}
