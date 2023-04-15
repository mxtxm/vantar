package com.vantar.util.file;

import com.vantar.exception.DateTimeException;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.zip.*;


public class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);


    public static String toFilename(String string) {
        return toFilename(string, "-");
    }

    public static String toFilenameWithoutSpace(String string) {
        return toFilename(string, "-").replace(' ', '-');
    }

    public static String toFilename(String string, String replace) {
        return StringUtil.replace(
            string,
            new char[] {'\n', '\t', '|', '/', '\\', '#', '&', '<', '>', '"', '\'', ':', '?', '*'},
            replace
        );
    }

    public static double getSizeMb(String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            double bytes = file.length();
            double kilobytes = bytes / 1024;
            double megabytes = kilobytes / 1024;
            return NumberUtil.round(megabytes, 1);
        }
        return 0;
    }

    public static String getSizeReadable(String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            return NumberUtil.getReadableByteSize(file.length());
        }
        return "0";
    }

    public static DateTime getLastModify(String filepath) {
        try {
            BasicFileAttributes attr = Files.readAttributes(Paths.get(filepath), BasicFileAttributes.class);
            return new DateTime(attr.lastModifiedTime().toString());
        } catch (IOException | DateTimeException e) {
            return null;
        }
    }

    public static double getSize(String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }

    public static boolean removeFile(String path) {
        try {
            Files.delete(Paths.get(path));
            return true;
        } catch (NoSuchFileException ignore) {

        } catch (IOException e) {
            log.error(" !! {}\n", path, e);
        }
        return false;
    }

    public static String getFileContentFromClassPath(String... paths) {
        for (String path : paths) {
            try {
                URL url = FileUtil.class.getResource(path);
                if (url == null) {
                    continue;
                }
                String value = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
                if (StringUtil.isNotEmpty(value)) {
                    return value;
                }
            } catch (IOException | URISyntaxException | NullPointerException e) {
                log.error(" !! {}", path, e);
            }
        }
        return "";
    }

    public static String getClassPathAbsolutePath(String path) {
        URL url = FileUtil.class.getResource(path);
        if (url == null) {
            return null;
        }
        try {
            return url.toURI().getRawPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static String getFileContent(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(" !! {}", path, e);
        }
        return "";
    }

    /**
     * replace text tokens in file content and write back
     */
    public static boolean replace(String path, Map<String, String> tokens) {
        Path filePath = Paths.get(path);
        Charset charset = StandardCharsets.UTF_8;

        try {
            String content = new String(Files.readAllBytes(filePath), charset);
            for (Map.Entry<String, String> entry : tokens.entrySet()) {
                content = StringUtil.replace(content, entry.getKey(), entry.getValue());
            }
            Files.write(filePath, content.getBytes(charset));
            return true;
        } catch (IOException e) {
            log.error(" !! replace({} > {})\n", filePath, tokens, e);
        }
        return false;
    }

    public static boolean write(String path, String content) {
        try {
            Files.write(Paths.get(path), content.getBytes());
            return true;
        } catch (IOException e) {
            log.error(" !! {}", path, e);
            return false;
        }
    }

    public static boolean updateProperties(String filename, Map<String, String> properties) {
        try {
            PrintWriter writer = new PrintWriter(filename + ".new", "UTF-8");
            writer.println("###");
            writer.println("# " + (new DateTime()).toString() + " - created by Vantar System Administrator");
            writer.println("#");

            String currentK = null;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String k = StringUtil.split(entry.getKey(), '.')[0];
                if (currentK == null || !currentK.equals(k)) {
                    writer.println("");
                    writer.println("");
                    writer.println("");
                    writer.println("# " + k);
                    currentK = k;
                }
                writer.println(entry.getKey() + "=" + entry.getValue());
            }

            writer.close();

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error(" !! config({} < {})\n", filename, properties, e);
            return false;
        }

        File oldFile = new File(filename);
        if (!oldFile.exists() || oldFile.renameTo(new File(filename + "-" + new DateTime().formatter().getDateTimeSimple()))) {
            return new File(filename + ".new").renameTo(new File(filename));
        }
        return false;
    }

    public static boolean zip(String dir, String zipFilename) {
        return zip(dir, zipFilename, null);
    }

    public static boolean zip(String dir, String zipPath, BeforeZipCallback callback) {
        Path sourceDir = Paths.get(dir);
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipPath)))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (callback == null || callback.accept(file.toAbsolutePath().toString())) {
                        try {
                            Path targetFile = sourceDir.relativize(file);
                            zip.putNextEntry(new ZipEntry(targetFile.toString()));
                            byte[] bytes = Files.readAllBytes(file);
                            zip.write(bytes, 0, bytes.length);
                            zip.closeEntry();
                        } catch (IOException e) {
                            log.error(" !! ziping ({} > {})\n", dir, zipPath, e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error(" !! ziping ({} > {})\n", dir, zipPath, e);
            return false;
        }
        return true;
    }

    public static boolean unzip(String zipPath, String dir) {
        int BUFFER_SIZE = 2048;
        try (ZipFile zip = new ZipFile(new File(zipPath), ZipFile.OPEN_READ)) {
            File destDirectory = new File(dir);
            destDirectory.mkdirs();

            Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = zipFileEntries.nextElement();
                File destFile = new File (destDirectory, entry.getName());

                File parentDestFile = destFile.getParentFile();
                parentDestFile.mkdirs();

                if (!entry.isDirectory()) {
                    BufferedInputStream buffer = new BufferedInputStream(zip.getInputStream(entry));
                    int currentByte;
                    byte[] data = new byte[BUFFER_SIZE];

                    try (BufferedOutputStream bufOS = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE)) {
                        while ((currentByte = buffer.read(data, 0, BUFFER_SIZE)) != -1) {
                            bufOS.write(data, 0, currentByte);
                        }
                    }

                    if (entry.getName().toLowerCase().endsWith(".zip")) {
                        String zipFilePath = destDirectory.getPath() + File.separatorChar + entry.getName();
                        unzip(zipFilePath, zipFilePath.substring(0, zipFilePath.length() - 4));
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error(" !! unzip ({} > {})\n", zipPath, dir, e);
            return false;
        }
    }

    public static Map<String, Object> getResourceStructure(String path) {
        path = '/' + StringUtil.trim(path, '/') + '/';

        Map<String, Object> structure = new HashMap<>();
        URL dir = FileUtil.class.getClassLoader().getResource(path);
        if (dir == null) {
            dir = FileUtil.class.getClassLoader().getResource(FileUtil.class.getName().replace(".", "/") + ".class");
        }
        if (dir == null) {
            return structure;
        }

        if (dir.getProtocol().equals("file")) {
            List<String> files = new ArrayList<>();

            try {
                for (File file : new File(dir.toURI()).listFiles()) {
                    String fileName = file.getName();
                    if (file.isFile()) {
                        files.add(fileName);
                    } else {
                        structure.put(fileName, getResourceStructure(path + fileName));
                    }
                }
            } catch (URISyntaxException ignore) {

            }

            structure.put("/", files);
        }

        return structure;
    }

    public static String getTempDirectory() {
        return StringUtil.rtrim(System.getProperty("java.io.tmpdir"), '/') + '/';
    }

    public static String makeTempDirectory() {
        String path = getTempDirectory() + "vt" + StringUtil.getRandomString(8) + '/';
        removeDirectory(path);
        return makeDirectory(path) ? path : null;
    }

    public static String getTempFilename() {
        return getUniqueName(getTempDirectory());
    }

    public static String getUniqueName(String directory) {
        while (true) {
            String tempPath = directory + StringUtil.getRandomString(20);
            if (!exists(tempPath)) {
                return tempPath;
            }
        }
    }

    public static boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }

    /**
     * true = dir exists now
     */
    public static boolean makeDirectory(String path) {
        if (exists(path)) {
            return true;
        }

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                Files.createDirectories(Paths.get(path));
                return true;
            } catch (IOException e) {
                log.error(" !! {}", path, e);
                return false;
            }
        }

        Set<PosixFilePermission> fullPermission = new HashSet<>(10, 1);
        fullPermission.add(PosixFilePermission.OWNER_EXECUTE);
        fullPermission.add(PosixFilePermission.OWNER_READ);
        fullPermission.add(PosixFilePermission.OWNER_WRITE);
        fullPermission.add(PosixFilePermission.GROUP_EXECUTE);
        fullPermission.add(PosixFilePermission.GROUP_READ);
        fullPermission.add(PosixFilePermission.GROUP_WRITE);
        fullPermission.add(PosixFilePermission.OTHERS_EXECUTE);
        fullPermission.add(PosixFilePermission.OTHERS_READ);
        fullPermission.add(PosixFilePermission.OTHERS_WRITE);

        try {
            Files.createDirectories(Paths.get(path), PosixFilePermissions.asFileAttribute(fullPermission));
            Files.setPosixFilePermissions(Paths.get(path), fullPermission);
            return true;
        } catch (IOException e) {
            log.error(" !! {}", path, e);
            return false;
        }
    }

    public static String[] getDirectoryFiles(String path) {
        File[] files = new File(path).listFiles();
        if (files == null) {
            return new String[] {};
        }
        String[] filenames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filenames[i] = files[i].getPath();
        }
        Arrays.sort(filenames);
        return filenames;
    }

    public static boolean removeDirectory(String path) {
        try {
            Files.walk(Paths.get(path))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .filter(item -> !item.getPath().equals(path))
                .forEach(File::delete);
            return true;
        } catch (NoSuchFileException ignore) {

        } catch (IOException e) {
            log.error(" !! {}", path, e);
        }
        return false;
    }

    public static void giveAllPermissions(String path) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return;
        }

        Set<PosixFilePermission> fullPermission = new HashSet<>(10, 1);
        fullPermission.add(PosixFilePermission.OWNER_EXECUTE);
        fullPermission.add(PosixFilePermission.OWNER_READ);
        fullPermission.add(PosixFilePermission.OWNER_WRITE);
        fullPermission.add(PosixFilePermission.GROUP_EXECUTE);
        fullPermission.add(PosixFilePermission.GROUP_READ);
        fullPermission.add(PosixFilePermission.GROUP_WRITE);
        fullPermission.add(PosixFilePermission.OTHERS_EXECUTE);
        fullPermission.add(PosixFilePermission.OTHERS_READ);
        fullPermission.add(PosixFilePermission.OTHERS_WRITE);

        try {
            Files.setPosixFilePermissions(Paths.get(path), fullPermission);
            for (String file : getDirectoryFiles(path)) {
                Files.setPosixFilePermissions(Paths.get(file), fullPermission);
            }
        } catch (IOException e) {
            log.error(" !! {}", path, e);
        }
    }

    public static boolean move(String target, String destination) {
        Path path = Paths.get(destination);
        makeDirectory(path.getParent().toString());

        try {
            Files.delete(path);
        } catch (Exception ignore) {

        }
        try {
            Files.copy(Paths.get(target), path);
            FileUtil.removeFile(target);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean copy(String target, String destination) {
        Path path = Paths.get(destination);

        try {
            Files.delete(path);
        } catch (Exception ignore) {

        }

        makeDirectory(path.getParent().toString());

        try {
            Files.copy(Paths.get(target), path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean resourceExists(String path) {
        return FileUtil.class.getResource(path) != null;
    }

    public static String textToPath(String s) {
        if (s == null) {
            return "-";
        }
        return StringUtil.removeExcept(
            StringUtil.replace(s, " ", "-").toLowerCase(),
            'a', 'b', 'c', 'd',  'e',  'f',  'g',  'h',  'i',  'j',  'k',  'l',  'm',  'n',  'o',  'p',  'q',  'r',  's',
            't', 'u',  'v',  'w',  'x',  'y',  'x',  '0',  '1',  '2', '3',  '4',  '5',  '6',  '7', '8', '9', '-'
        );
    }

    public static boolean readLines(String path, ReadCallback callback) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            while (line != null) {
                if (!callback.get(line)) {
                    break;
                }
                line = reader.readLine();
            }
            return true;
        } catch (IOException e) {
            log.error("! {}", path, e);
        }
        return false;
    }


    public interface BeforeZipCallback {

        boolean accept(String filename);
    }


    public interface ReadCallback {

        boolean get(String data);
    }
}