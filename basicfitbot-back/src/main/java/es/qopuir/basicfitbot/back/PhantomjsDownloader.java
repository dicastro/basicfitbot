package es.qopuir.basicfitbot.back;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhantomjsDownloader {
    private static final Logger LOG = LoggerFactory.getLogger(PhantomjsDownloader.class);

    private static final String BASE_DOWNLOAD_URL = "https://bitbucket.org/ariya/phantomjs/downloads/";

    public enum Version {
        V_2_1_1("2.1.1");

        private final String version;

        private Version(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }

    enum Platform {
        WINDOWS("-windows", ".zip"), MAC("-macosx", ".zip"), LINUX_32("-linux-i386", ".tar.bz2"), LINUX_64("-linux-x86_64", ".tar.bz2");

        private final String fileBaseName = "phantomjs-";
        private final String platformName;
        private final String format;

        private Platform(String platformName, String format) {
            this.platformName = platformName;
            this.format = format;
        }

        public String getFileName(Version version) {
            return fileBaseName + version.getVersion() + platformName + format;
        }
    }

    /**
     * Download and unpack bundled phantomjs binary version into
     * ${java.io.tmpdir}/phantomjs-${phantomjs.version}/phantomjs
     * 
     * @param version
     * @return File of the unbundled phantomjs binary
     */
    public static File download(Version version) {
        String javaIoTmpdir = System.getProperty("java.io.tmpdir");
        // multiple versions can coexist
        return download(version, new File(javaIoTmpdir, "phantomjs-" + version.getVersion()), null);
    }
    
    public static File download(Version version, ProxyProperties proxy) {
        String javaIoTmpdir = System.getProperty("java.io.tmpdir");
        // multiple versions can coexist
        return download(version, new File(javaIoTmpdir, "phantomjs-" + version.getVersion()), proxy);
    }

    /**
     * Download and unpack bundled phantomjs binary version into specified directory
     * 
     * @param version
     * @param directory
     * @return file path of the unbundled phantomjs binary
     */
    public static String download(Version version, String directory) {
        File file = download(version, new File(directory), null);
        return file.getAbsolutePath();
    }
    
    public static String download(Version version, String directory, ProxyProperties proxy) {
        File file = download(version, new File(directory), proxy);
        return file.getAbsolutePath();
    }

    /**
     * Download and unpack bundled phantomjs binary version into specified directory
     * 
     * @param version
     * @param destination
     * @return File of the unbundled phantomjs binary
     */
    public static File download(Version version, File destination, ProxyProperties proxy) {
        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                throw new IllegalArgumentException("Failed to make target directory: " + destination);
            }
        }

        File destinationFile;
        boolean chmodx;
        String osname = System.getProperty("os.name").toLowerCase();
        if (osname.contains("win")) {
            destinationFile = new File(destination, "phantomjs.exe");
            download(version, Platform.WINDOWS, destinationFile, proxy);
            chmodx = false;
        } else if (osname.contains("mac os")) {
            destinationFile = new File(destination, "phantomjs");
            download(version, Platform.MAC, destinationFile, proxy);
            chmodx = true;
        } else if (osname.contains("linux")) {
            destinationFile = new File(destination, "phantomjs");
            // Linux has i386 or amd64
            String osarch = System.getProperty("os.arch");
            if (osarch.equals("i386")) {
                download(version, Platform.LINUX_32, destinationFile, proxy);
            } else {
                download(version, Platform.LINUX_64, destinationFile, proxy);
            }
            chmodx = true;
        } else {
            throw new IllegalArgumentException("Unsupported OS " + osname);
        }

        if (chmodx) {
            if (!destinationFile.setExecutable(true)) {
                throw new IllegalArgumentException("Failed to make executable " + destinationFile);
            }
        }

        return destinationFile;
    }

    private static void download(Version version, Platform platform, File destination, ProxyProperties proxy) {
        LOG.info("About to download Phantomjs version {}", version.getVersion());

        if (destination.exists() && destination.isFile() && destination.canExecute()) {
            LOG.info("Phantomjs binary is available");
            return; // keep existing
        }

        String downloadFileName = platform.getFileName(version);

        URL downloadUrl = null;
        try {
            downloadUrl = new URL(BASE_DOWNLOAD_URL + downloadFileName);
        } catch (MalformedURLException e) {
            // ignore
        }

        LOG.info("Phantomjs download url {}", downloadUrl.toString());

        String javaIoTmpdir = System.getProperty("java.io.tmpdir");

        Path downloadPath = Paths.get(javaIoTmpdir, "download", "phantomjs");
        downloadPath.toFile().mkdirs();

        File downloadFile = downloadPath.resolve(downloadFileName).toFile();

        if (downloadFile.exists() && downloadFile.isFile()) {
            LOG.info("Phantomjs already downloaded");
        } else {
            LOG.debug("Download destination {}", downloadFile.getAbsolutePath());

            InputStream stream = null;

            try {
                URLConnection connection = null;

                if (proxy.isEnabled()) {
                    InetSocketAddress sa = new InetSocketAddress(proxy.getHost(), proxy.getPort());

                    connection = downloadUrl.openConnection(new Proxy(Proxy.Type.HTTP, sa));
                } else {
                    connection = downloadUrl.openConnection();
                }

                stream = connection.getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Download url not found " + downloadUrl.toString(), e);
            }

            try (ReadableByteChannel rbc = Channels.newChannel(stream); FileOutputStream fos = new FileOutputStream(downloadFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to download resource: " + downloadUrl.toString() + " into: " + downloadFile.toString(), e);
            }
        }

        LOG.info("About to uncompress file {}", downloadFile.getAbsolutePath());

        File tarFile = downloadPath.resolve(downloadFileName.replace(".bz2", "")).toFile();

        if (!tarFile.exists()) {
            try (ReadableByteChannel rbc = Channels.newChannel(new BZip2CompressorInputStream(new FileInputStream(downloadFile)));
                    FileOutputStream fos = new FileOutputStream(tarFile)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to uncompress resource: " + downloadFile.getAbsolutePath(), e);
            }
        } else {
            LOG.info("File is already uncompressed");
        }

        Path uncompressPath = downloadPath.resolve(downloadFileName.replace(".tar.bz2", ""));
        File outputDir = uncompressPath.toFile();

        if (outputDir.exists() && outputDir.isDirectory()) {
            LOG.info("Tar is already untarred");
        } else {
            LOG.info("About to untar {} to dir {}", tarFile.getAbsolutePath(), outputDir.getAbsolutePath());

            List<File> untaredFiles = new LinkedList<File>();

            try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
                TarArchiveEntry entry = null;

                while ((entry = tarInputStream.getNextTarEntry()) != null) {
                    File outputFile = new File(outputDir, entry.getName());

                    if (entry.isDirectory()) {
                        LOG.debug("Attempting to write output directory {}", outputFile.getAbsolutePath());

                        if (!outputFile.exists()) {
                            LOG.debug("Attempting to create output directory {}", outputFile.getAbsolutePath());

                            if (!outputFile.mkdirs()) {
                                throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                            }
                        }
                    } else {
                        LOG.debug("Extracting output file {}", outputFile.getAbsolutePath());

                        try (OutputStream outputFileStream = new FileOutputStream(outputFile)) {
                            IOUtils.copy(tarInputStream, outputFileStream);
                        } catch (IOException e) {
                            LOG.error("Error while extracting file {}", outputFile.getAbsolutePath(), e);
                        }
                    }
                    untaredFiles.add(outputFile);
                }
            } catch (IOException e1) {
                LOG.error("Error while untarring file {}", tarFile.getAbsolutePath(), e1);
            }
        }

        try {
            String pattern = "phantomjs";
            Finder finder = new Finder(pattern);
            Files.walkFileTree(uncompressPath, finder);

            if (finder.getNumMatches() == 1) {
                try (FileOutputStream fos = new FileOutputStream(destination)) {
                    Files.copy(finder.getMatches().get(0), fos);
                }
            }
        } catch (IOException e) {
            LOG.error("ERROR", e);
        }
    }

    static class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private List<Path> matches = new LinkedList<Path>();

        Finder(String pattern) {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                matches.add(file);
            }
        }

        int getNumMatches() {
            return matches.size();
        }

        List<Path> getMatches() {
            return matches;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            find(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            find(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            LOG.error("ERROR", exc);
            return FileVisitResult.CONTINUE;
        }
    }

    public static void main(String[] args) {
        PhantomjsDownloader.download(Version.V_2_1_1);
    }
}