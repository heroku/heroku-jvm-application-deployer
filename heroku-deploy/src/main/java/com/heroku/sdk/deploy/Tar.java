package com.heroku.sdk.deploy;

import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Tar {
  public static File create(String filename, String directory, File outputDir) throws IOException, ArchiveException, InterruptedException {

    if (useNativeTar()) {
      String gzipFilename = filename + ".tgz";
      ProcessBuilder pb = new ProcessBuilder().command("tar", "pczf", gzipFilename, directory).directory(outputDir);
      pb.start().waitFor();
      return new File(outputDir, gzipFilename);
    } else {
      return new Pack(outputDir, directory).apply(filename, outputDir);
    }
  }

  public static void extract(File tarFile, File outputDir) throws IOException, InterruptedException, ArchiveException {
    if (useNativeTar()) {
      ProcessBuilder pb = new ProcessBuilder().command("tar", "pxf", tarFile.getAbsolutePath(), "-C", outputDir.getAbsolutePath());
      pb.start().waitFor();
    } else {
      new Unpack(tarFile, outputDir).apply();
    }
  }

  public static class Pack {
    private File workingDir;
    private String directory;

    public Pack(File workingDir, String directory) {
      this.workingDir = workingDir;
      this.directory = directory;
    }

    private List<File> recursiveListFiles(File f) {
      List<File> allFiles = new ArrayList<File>();

      for (File subFile : f.listFiles()) {
        if (subFile.isDirectory()) {
          allFiles.addAll(recursiveListFiles(subFile));
        }
        allFiles.add(subFile);
      }
      return allFiles;
    }

    private void addFilesToTar(ArchiveOutputStream tarBall, File dir) throws IOException {
      for (File file : recursiveListFiles(dir)) {
        if (!file.isDirectory()) {
          TarArchiveEntry tarFile = new TarArchiveEntry(file, relativize(file));
          tarFile.setSize(file.length());
          if (java.nio.file.Files.isExecutable(java.nio.file.FileSystems.getDefault().getPath(file.getAbsolutePath()))) {
            tarFile.setMode(493);
          }
          tarBall.putArchiveEntry(tarFile);
          IOUtils.copy(new FileInputStream(file), tarBall);
          tarBall.closeArchiveEntry();
        }
      }
    }

    private void compress(File sourceFile, File targetFile) throws IOException {
      FileOutputStream fos = new FileOutputStream(targetFile);
      GZIPOutputStream gzs = new GZIPOutputStream(fos);
      FileInputStream fis = new FileInputStream(sourceFile);

      try {
        IOUtils.copy(fis, gzs);
      } finally {
        gzs.close();
        fis.close();
      }
    }

    private File apply(String archiveBasename, File outputDir) throws ArchiveException, IOException {
      File archive = new File(outputDir, (archiveBasename + ".tar"));
      FileOutputStream tarOutput = new FileOutputStream(archive);

      TarArchiveOutputStream tarBall = (TarArchiveOutputStream)new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.TAR, tarOutput);
      tarBall.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      try {
        addFilesToTar(tarBall, new File(workingDir, directory));
      } finally {
        tarBall.close();
      }

      File outputFile = new File(outputDir, (archiveBasename));
      compress(archive, outputFile);
      FileUtils.deleteQuietly(archive);
      return outputFile;
    }

    private String relativize(File path) {
      String relativePath = new File(this.workingDir, this.directory).toURI().relativize(path.toURI()).getPath();

      // don't use File.separator because it will be wrong on Windows
      return this.directory + "/" + relativePath;
    }
  }

  public static class Unpack {

    private File tarFile;
    private File outputDir;

    public Unpack(File tarFile, File outputDir) {
      this.tarFile = tarFile;
      this.outputDir = outputDir;
    }

    private ArchiveInputStream extract(InputStream input) throws ArchiveException {
      return new ArchiveStreamFactory().createArchiveInputStream(input);
    }

    public void apply() throws IOException, ArchiveException {
      FileInputStream fis = new FileInputStream(tarFile);
      GZIPInputStream gzs = new GZIPInputStream(fis);
      try {
        ArchiveInputStream archiveInputStream = extract(new BufferedInputStream(gzs));

        ArchiveEntry entry = archiveInputStream.getNextEntry();
        while (entry != null) {
          if (entry.isDirectory()) {
            FileUtils.forceMkdir(new File(outputDir, entry.getName()));
          } else {
            File destPath = new File(outputDir, entry.getName());

            IOUtils.copy(archiveInputStream, new FileOutputStream(destPath));

            if (((TarArchiveEntry) entry).getMode() == 493) {
              destPath.setExecutable(true);
            }
          }
          entry = archiveInputStream.getNextEntry();
        }
      } finally {
        gzs.close();
      }

    }
  }

  private static Boolean useNativeTar() {
    return !SystemSettings.hasNio();
  }
}
