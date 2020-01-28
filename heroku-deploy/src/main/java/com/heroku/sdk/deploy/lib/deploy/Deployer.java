package com.heroku.sdk.deploy.lib.deploy;

import com.heroku.api.HerokuAPI;
import com.heroku.api.Source;
import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.api.BuildInfo;
import com.heroku.sdk.deploy.api.HerokuDeployApi;
import com.heroku.sdk.deploy.util.io.UploadProgressHttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public final class Deployer {

    public static boolean deploy(String apiKey, DeploymentDescriptor deploymentDescriptor, OutputAdapter listener) throws IOException, InterruptedException {
        HerokuAPI herokuApi = new HerokuAPI(apiKey);
        HerokuDeployApi herokuDeployApi = new HerokuDeployApi("client-tbd", apiKey);

        Source source = herokuApi.createSource();
        Source.Blob sourceBlob = source.getSource_blob();

        listener.logInfo("-----> Uploading build...");
        uploadSourceBlob(deploymentDescriptor.getSourceBlobPath(), URI.create(sourceBlob.getPut_url()), listener::logUploadProgress);
        listener.logInfo("       - success");

        listener.logInfo("-----> Deploying...");
        BuildInfo buildInfo = herokuDeployApi.createBuild(
                deploymentDescriptor.getAppName(),
                URI.create(sourceBlob.getGet_url()),
                deploymentDescriptor.getVersion().orElse("unknown"),
                deploymentDescriptor.getBuildpacks());

        herokuDeployApi
                .followBuildOutputStream(URI.create(buildInfo.outputStreamUrl)) // TODO: This can be null?!  (id=forbidden)
                .map(line -> "remote: " + line)
                .forEachOrdered(listener::logInfo);

        buildInfo = pollForNonPendingBuildInfo(deploymentDescriptor.getAppName(), buildInfo.id, herokuDeployApi);

        if (!buildInfo.status.equals("succeeded")) {
            listener.logDebug("Failed Build ID: " + buildInfo.id);
            listener.logDebug("Failed Build Status: " + buildInfo.status);
            listener.logDebug("Failed Build UpdatedAt: " + buildInfo.updatedAt);
            return false;
        }

        listener.logInfo("-----> Done");
        return true;
    }

    private static BuildInfo pollForNonPendingBuildInfo(String appName, String buildId, HerokuDeployApi herokuDeployApi) throws IOException, InterruptedException {
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
