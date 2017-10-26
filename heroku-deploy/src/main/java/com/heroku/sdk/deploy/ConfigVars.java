package com.heroku.sdk.deploy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.heroku.api.HerokuAPI;

public class ConfigVars {

  private Deployer deployer;

  private HerokuAPI api;

  public ConfigVars(Deployer deployer, String apiKey) {
    this.deployer = deployer;
    this.api = new HerokuAPI(apiKey);
  }

  public void merge(Map<String, String> configVars) throws Exception {
    Map<String, String> existingConfigVars = getConfigVars();
    deployer.logDebug("Heroku existing config variables: " + existingConfigVars.keySet());

    Map<String, String> newConfigVars = new HashMap<String, String>();
    for (String key : configVars.keySet()) {
      newConfigVars.putAll(addConfigVar(key, configVars.get(key), existingConfigVars));
    }
    setConfigVars(newConfigVars);
  }

  protected Map<String, String> getConfigVars() throws Exception {
    return this.api.listConfig(deployer.getName());
  }

  protected void setConfigVars(Map<String, String> configVars) throws IOException {
    if (!configVars.isEmpty()) {
      api.updateConfig(deployer.getName(), configVars);
    }
  }

  private Map<String, String> addConfigVar(String key, String value, Map<String, String> existingConfigVars) {
    return addConfigVar(key, value, existingConfigVars, true);
  }

  private Map<String, String> addConfigVar(String key, String value, Map<String, String> existingConfigVars, Boolean force) {
    Map<String, String> m = new HashMap<String, String>();
    if (!existingConfigVars.containsKey(key) || (!value.equals(existingConfigVars.get(key)) && force)) {
      m.put(key, value);
    }
    return m;
  }
}
