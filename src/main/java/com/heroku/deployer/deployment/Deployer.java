package com.heroku.deployer.deployment;

import com.heroku.deployer.api.*;
import com.heroku.deployer.util.io.UploadProgressHttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public final class Deployer {

    public static boolean deploy(String apiKey, String clientName, String clientVersion, DeploymentDescriptor deploymentDescriptor) throws IOException, InterruptedException {
        HerokuDeployApi herokuDeployApi = new HerokuDeployApi(clientName, clientVersion, apiKey);

        SourceBlob sourceBlob;
        try {
            sourceBlob = herokuDeployApi.createSourceBlob();
        } catch (HerokuDeployApiException e) {
            System.out.println("Could not create source blob: " + e.getMessage());
            return false;
        }

        if (!deploymentDescriptor.getConfigVars().isEmpty()) {
            System.out.println("-----> Setting config vars...");
            try {
                herokuDeployApi.updateAppConfig(deploymentDescriptor.getAppName(), deploymentDescriptor.getConfigVars());
            } catch(Throwable t) {
                System.err.println("Could not set config vars: " + t.getMessage());
                return false;
            }
        }

        System.out.println("-----> Uploading build...");
        uploadSourceBlob(deploymentDescriptor.getSourceBlobPath(), URI.create(sourceBlob.getPutUrl()), (currentBytes, totalBytes) -> {});
        System.out.println("       - success");

        List<String> buildpacks = deploymentDescriptor.getBuildpacks();
        if (buildpacks.isEmpty()) {
            buildpacks = Collections.singletonList("heroku/jvm");
        }

        System.out.println("-----> Deploying...");
        BuildInfo buildInfo;
        try {
            buildInfo = herokuDeployApi.createBuild(
                    deploymentDescriptor.getAppName(),
                    URI.create(sourceBlob.getGetUrl()),
                    deploymentDescriptor.getVersion(),
                    buildpacks);
        } catch (AppNotFoundException e) {
            System.err.println("Could not find application! Make sure you configured your application name correctly.");
            return false;
        }  catch (InsufficientAppPermissionsException e) {
            System.err.println("Insufficient permissions to deploy to application! Make sure you configured your application name correctly.");
            return false;
        } catch (HerokuDeployApiException e) {
            System.err.println("Unknown error while deploying: " + e.getMessage());
            return false;
        }

        try {
            herokuDeployApi
                    .followBuildOutputStream(URI.create(buildInfo.outputStreamUrl))
                    .map(line -> "remote: " + line)
                    .forEachOrdered(System.out::println);
        } catch (SSLHandshakeException e) {
            System.out.println("Could not get remote output. You could be running an older Java version without Let's Encrypt support. Your build will continue to run, stand by.");
        }

        try {
            buildInfo = pollForNonPendingBuildInfo(deploymentDescriptor.getAppName(), buildInfo.id, herokuDeployApi);
        } catch (HerokuDeployApiException e) {
            System.out.println(String.format("Could not get updated build information. Will try again for some time... (%s)", e.getMessage()));
        }

        if (!buildInfo.status.equals("succeeded")) {
            System.out.println("Failed Build ID: " + buildInfo.id);
            System.out.println("Failed Build Status: " + buildInfo.status);
            System.out.println("Failed Build UpdatedAt: " + buildInfo.updatedAt);
            return false;
        }

        System.out.println("-----> Done");
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

        CloseableHttpClient client = HttpClients.createSystem();

        HttpPut request = new HttpPut(destination);
        request.setEntity(new UploadProgressHttpEntity(new FileEntity(path.toFile()), bytes -> progressConsumer.accept(bytes, fileSize)));

        client.execute(request);
    }
}
