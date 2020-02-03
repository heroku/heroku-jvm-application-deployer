package com.heroku.sdk.deploy.lib.deploymemt;

import com.heroku.api.HerokuAPI;
import com.heroku.api.Source;
import com.heroku.sdk.deploy.api.*;
import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.util.io.UploadProgressHttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public final class Deployer {

    public static boolean deploy(String apiKey, DeploymentDescriptor deploymentDescriptor, OutputAdapter outputAdapter) throws IOException, InterruptedException {
        HerokuAPI herokuApi = new HerokuAPI(apiKey);
        // TODO: client and version string!?
        HerokuDeployApi herokuDeployApi = new HerokuDeployApi("client-tbd", "version-tbd", apiKey);

        Source source = herokuApi.createSource();
        Source.Blob sourceBlob = source.getSource_blob();

        if (!deploymentDescriptor.getConfigVars().isEmpty()) {
            outputAdapter.logInfo("-----> Setting config vars...");
            try {
                herokuApi.updateConfig(deploymentDescriptor.getAppName(), deploymentDescriptor.getConfigVars());
            } catch(Throwable t) {
                outputAdapter.logError("Could not set config vars: " + t.getMessage());
                return false;
            }
        }

        outputAdapter.logInfo("-----> Uploading build...");
        uploadSourceBlob(deploymentDescriptor.getSourceBlobPath(), URI.create(sourceBlob.getPut_url()), outputAdapter::logUploadProgress);
        outputAdapter.logInfo("       - success");

        List<String> buildpacks = deploymentDescriptor.getBuildpacks();
        if (buildpacks.isEmpty()) {
            buildpacks = Collections.singletonList("heroku/jvm");
        }

        outputAdapter.logInfo("-----> Deploying...");
        BuildInfo buildInfo;
        try {
            buildInfo = herokuDeployApi.createBuild(
                    deploymentDescriptor.getAppName(),
                    URI.create(sourceBlob.getGet_url()),
                    deploymentDescriptor.getVersion(),
                    buildpacks);
        } catch (AppNotFoundException e) {
            outputAdapter.logError("Could not find application! Make sure you configured your application name correctly.");
            return false;
        }  catch (InsufficientAppPermissionsException e) {
            outputAdapter.logError("Insufficient permissions to deploy to application! Make sure you configured your application name correctly.");
            return false;
        }  catch (HerokuDeployApiException e) {
            outputAdapter.logError("Unknown error while deploying: " + e.getMessage(), e);
            return false;
        }

        herokuDeployApi
                .followBuildOutputStream(URI.create(buildInfo.outputStreamUrl))
                .map(line -> "remote: " + line)
                .forEachOrdered(outputAdapter::logInfo);

        try {
            buildInfo = pollForNonPendingBuildInfo(deploymentDescriptor.getAppName(), buildInfo.id, herokuDeployApi);
        } catch (HerokuDeployApiException e) {
            outputAdapter.logWarn(String.format("Could not get updated build information. Will try again for some time... (%s)", e.getMessage()));
        }

        if (!buildInfo.status.equals("succeeded")) {
            outputAdapter.logDebug("Failed Build ID: " + buildInfo.id);
            outputAdapter.logDebug("Failed Build Status: " + buildInfo.status);
            outputAdapter.logDebug("Failed Build UpdatedAt: " + buildInfo.updatedAt);
            return false;
        }

        outputAdapter.logInfo("-----> Done");
        return true;
    }

    private static BuildInfo pollForNonPendingBuildInfo(String appName, String buildId, HerokuDeployApi herokuDeployApi) throws IOException, InterruptedException, HerokuDeployApiException {
        for (int i = 0; i < 15; i++) {
            BuildInfo latestBuildInfo = herokuDeployApi.getBuildInfo(appName, buildId);
            if (!latestBuildInfo.status.equals("pending")) {
                return latestBuildInfo;
            }

            Thread.sleep(2000);
        }

        return herokuDeployApi.getBuildInfo(appName, buildId);
    }

    private static void uploadSourceBlob(Path path, URI destination, BiConsumer<Long, Long> progressConsumer) throws IOException {
        long fileSize = Files.size(path);

        CloseableHttpClient client = HttpClients.createDefault();

        HttpPut request = new HttpPut(destination);
        request.setEntity(new UploadProgressHttpEntity(new FileEntity(path.toFile()), bytes -> progressConsumer.accept(bytes, fileSize)));

        client.execute(request);
    }
}
