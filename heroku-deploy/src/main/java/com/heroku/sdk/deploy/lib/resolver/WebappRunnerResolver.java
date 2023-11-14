package com.heroku.sdk.deploy.lib.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heroku.sdk.deploy.api.HerokuDeployApiException;
import com.heroku.sdk.deploy.api.SourceBlob;
import com.heroku.sdk.deploy.util.CustomHttpClientBuilder;
import com.heroku.sdk.deploy.util.Util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves group id and download URLs for webapp-runner. At one point its group id changed, creating the need
 * to discern which version uses which group id and download URL.
 */
public class WebappRunnerResolver {

    public static String getLatestVersion() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        CloseableHttpClient client = CustomHttpClientBuilder.build();

        HttpGet request = new HttpGet("https://repo1.maven.org/maven2/com/heroku/webapp-runner/maven-metadata.xml");
        CloseableHttpResponse response = client.execute(request);

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.getEntity().getContent());
                XPath xpath = XPathFactory.newInstance().newXPath();
                return xpath.compile("/metadata/versioning/latest/text()").evaluate(document);

            default:
                throw new RuntimeException("");
        }
    }

    public static String getGroupIdForVersion(String version) {
        return isLegacyVersion(version) ? "com.github.jsimone" : "com.heroku";
    }

    public static URI getUrlForVersion(String version) {
        String pattern = isLegacyVersion(version)
                ? "https://repo1.maven.org/maven2/com/github/jsimone/webapp-runner/%s/webapp-runner-%s.jar"
                : "https://repo1.maven.org/maven2/com/heroku/webapp-runner/%s/webapp-runner-%s.jar";

        return URI.create(String.format(pattern, version, version));
    }

    private static boolean isLegacyVersion(String version) {
        return knownLegacyVersions.contains(version);
    }

    // Known versions that use the legacy group id "com.github.jsimone". Since there are only a handful of
    // legacy versions around, maintaining a static list is more robust than using a more complex version matching
    // mechanism.
    private static final List<String> knownLegacyVersions = Arrays.asList(
            "9.0.27.1",
            "9.0.27.0",
            "9.0.24.1",
            "9.0.24.0",
            "9.0.22.0",
            "9.0.20.1",
            "9.0.20.0",
            "9.0.19.1",
            "9.0.19.0",
            "9.0.17.0",
            "9.0.16.0",
            "9.0.14.0",
            "9.0.13.0",
            "9.0.11.0",
            "9.0.8.1",
            "9.0.8.0",
            "8.5.47.2",
            "8.5.47.1",
            "8.5.47.0",
            "8.5.45.0",
            "8.5.43.1",
            "8.5.43.0",
            "8.5.41.1",
            "8.5.41.0",
            "8.5.40.1",
            "8.5.40.0",
            "8.5.39.0",
            "8.5.38.0",
            "8.5.37.1",
            "8.5.37.0",
            "8.5.35.0",
            "8.5.34.1",
            "8.5.34.0",
            "8.5.33.0",
            "8.5.32.1",
            "8.5.32.0",
            "8.5.31.1",
            "8.5.31.0",
            "8.5.30.0",
            "8.5.29.0",
            "8.5.28.0",
            "8.5.27.0",
            "8.5.24.0",
            "8.5.23.1",
            "8.5.23.0",
            "8.5.20.1",
            "8.5.20.0",
            "8.5.15.1",
            "8.5.15.0",
            "8.5.11.3",
            "8.5.11.2",
            "8.5.11.1",
            "8.5.11.0",
            "8.5.9.0",
            "8.5.5.2",
            "8.5.5.1",
            "8.5.5.0",
            "8.0.52.0",
            "8.0.51.0",
            "8.0.50.0",
            "8.0.47.0",
            "8.0.44.0",
            "8.0.39.0",
            "8.0.33.4",
            "8.0.33.3",
            "8.0.33.2",
            "8.0.33.1",
            "8.0.33.0",
            "8.0.30.2",
            "8.0.30.1",
            "8.0.30.0",
            "8.0.24.1",
            "8.0.24.0",
            "8.0.23.0",
            "8.0.18.0-M1",
            "7.0.91.0",
            "7.0.88.0",
            "7.0.86.0",
            "7.0.85.0",
            "7.0.84.0",
            "7.0.82.0",
            "7.0.57.2",
            "7.0.57.1",
            "7.0.40.2",
            "7.0.40.1",
            "7.0.40.0",
            "7.0.34.3",
            "7.0.34.2",
            "7.0.34.1",
            "7.0.34.0",
            "7.0.30.1",
            "7.0.29.3",
            "7.0.27.1",
            "7.0.22.3",
            "7.0.22.2",
            "7.0.22.1",
            "7.0.22"
    );
}
