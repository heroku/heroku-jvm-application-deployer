package com.heroku.sdk.deploy;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ConfigVars {

  private Deployer deployer;

  private String encodedApiKey;

  public ConfigVars(Deployer deployer, String encodedApiKey) {
    this.deployer = deployer;
    this.encodedApiKey = encodedApiKey;
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
    String urlStr = Slug.BASE_URL + "/apps/" + URLEncoder.encode(deployer.getName(), "UTF-8") + "/config-vars";

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Accept", "application/vnd.heroku+json; version=3");

    Map m = RestClient.get(urlStr, headers);
    Map<String, String> configVars = new HashMap<String, String>();
    for (Object key : m.keySet()) {
      Object value = m.get(key);
      if ((key instanceof String) && (value instanceof String)) {
        configVars.put(key.toString(), value.toString());
      } else {
        throw new Exception("Unexpected return type: " + m);
      }
    }
    return configVars;
  }

  protected void setConfigVars(Map<String, String> configVars) throws IOException {
    if (!configVars.isEmpty()) {
      String urlStr = Slug.BASE_URL + "/apps/" + URLEncoder.encode(deployer.getName(), "UTF-8") + "/config-vars";

      String data = "{";
      boolean first = true;
      for (String key : configVars.keySet()) {
        String value = configVars.get(key);
        if (!first) data += ", ";
        first = false;
        data += "\"" + key + "\"" + ":" + "\"" + StringEscapeUtils.escapeJson(value) + "\"";
      }
      data += "}";

      Map<String, String> headers = new HashMap<String, String>();
      headers.put("Authorization", encodedApiKey);
      headers.put("Accept", "application/vnd.heroku+json; version=3");

      RestClient.patch(urlStr, data, headers);
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
