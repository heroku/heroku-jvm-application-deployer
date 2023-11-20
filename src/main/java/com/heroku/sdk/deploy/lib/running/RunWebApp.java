package com.heroku.sdk.deploy.lib.running;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.resolver.WebappRunnerResolver;
import com.heroku.sdk.deploy.util.FileDownloader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RunWebApp {

    public static void run(Path warFile, List<String> javaOptions, List<String> webappRunnerOptions, String webappRunnerVersion, OutputAdapter outputAdapter) throws IOException, InterruptedException {
        outputAdapter.logInfo("Downloading webapp-runner...");
        Path webappRunnerJarPath = null;
        try {
             webappRunnerJarPath = FileDownloader.download(WebappRunnerResolver.getUrlForVersion(webappRunnerVersion));
        } catch (FileNotFoundException e) {
            outputAdapter.logDebug(String.format("Could not download webapp-runner %s. Please check if this is a valid version.", webappRunnerVersion));
            System.exit(-1);
        }

        ArrayList<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        command.addAll(javaOptions);
        command.add("-jar");
        command.add(webappRunnerJarPath.toString());
        command.addAll(webappRunnerOptions);
        command.add(warFile.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command.toArray(new String[0]));
        Process process = processBuilder.start();

        StreamGobbler stdOutStreamGobbler = new StreamGobbler(process.getInputStream(), outputAdapter);
        StreamGobbler stdErrStreamGobbler = new StreamGobbler(process.getErrorStream(), outputAdapter);

        stdOutStreamGobbler.start();
        stdErrStreamGobbler.start();

        process.waitFor();
    }

    private static class StreamGobbler extends Thread {
        private InputStream inputStream;
        private OutputAdapter outputAdapter;

        public StreamGobbler(InputStream inputStream, OutputAdapter outputAdapter) {
            super("StreamGobbler");
            this.inputStream = inputStream;
            this.outputAdapter = outputAdapter;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = br.readLine()) != null) {
                    outputAdapter.logInfo(line);
                }
            } catch (IOException e) {
                outputAdapter.logError(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }
}
