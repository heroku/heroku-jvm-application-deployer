package com.heroku.sdk;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Tar {
  public static File create(String filename, String directory, File outputDir) throws IOException, ArchiveException, InterruptedException {

//    if (useNativeTar()) {
      String gzipFilename = filename + ".tgz";
      ProcessBuilder pb = new ProcessBuilder().command("tar", "pczf", gzipFilename, directory).directory(outputDir);
      pb.start().waitFor();
      return new File(outputDir, gzipFilename);
//    } else {
//      return new Pack(filename, outputDir, directory, outputDir).apply();
//    }
  }

  public static void extract(File tarFile, File outputDir) throws IOException, InterruptedException {
//    if (useNativeTar()) {
      ProcessBuilder pb = new ProcessBuilder().command("tar", "pxf", tarFile.getAbsolutePath(), "-C", outputDir.getAbsolutePath());
      pb.start().waitFor();
//    } else {
//      new Unpack(tarFile, outputDir).apply();
//    }
  }

  public static class Pack {
    private String archiveBasename;
    private File workingDir;
    private String directory;
    private File outputDir;

    public Pack(String archiveBasename, File workingDir, String directory, File outputDir) {
      this.archiveBasename = archiveBasename;
      this.workingDir = workingDir;
      this.directory = directory;
      this.outputDir = outputDir;
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
          TarArchiveEntry tarFile = new TarArchiveEntry(file, relativize(workingDir, file));
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

    private File apply() throws ArchiveException, IOException {
      File archive = new File(outputDir, (archiveBasename + ".tar"));
      FileOutputStream tarOutput = new FileOutputStream(archive);



      TarArchiveOutputStream tarBall = (TarArchiveOutputStream)new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.TAR, tarOutput);
      tarBall.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      try {
        addFilesToTar(tarBall, new File(workingDir, directory));
      } finally {
        tarBall.close();
      }

      File outputFile = new File(outputDir, (archiveBasename + ".tgz"));
//      sbt.IO.gzip(archive, outputFile);
//      sbt.IO.delete(archive);
      return outputFile;
    }

    private String relativize(File base, File path) {
      return base.toURI().relativize(path.toURI()).getPath();
    }
  }

  public static class Unpack {

    private File tarFile;
    private File outputDir;

    public Unpack(File tarFile, File outputDir) {
      this.tarFile = tarFile;
      this.outputDir = outputDir;
    }

    private InputStream decompress(BufferedInputStream input) throws CompressorException {
      return new CompressorStreamFactory().createCompressorInputStream(input);
    }

    private ArchiveInputStream extract(InputStream input) throws ArchiveException {
      return new ArchiveStreamFactory().createArchiveInputStream(input);
    }

//    val input = extract(decompress(new BufferedInputStream(new FileInputStream(tarFile))))
//    private Stream<ArchiveEntry> stream: Stream[ArchiveEntry] = input.getNextEntry match {
//      case null => Stream.empty
//      case entry => entry #:: stream
//    }

    public File apply() {
      return null;
    }
  }

  private static Boolean useNativeTar() {
    return !SystemSettings.hasNio();
  }
}
