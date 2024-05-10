package com.vantar.util.file;

import com.vantar.util.string.StringUtil;
import org.apache.commons.io.FileUtils;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;


public class DirUtil {

    public static void browseDir(String path, Callback callback) {
        browseDir(path, false, callback);
    }

    public static void browseDirRecursive(String path, Callback callback) {
        browseDir(path, true, callback);
    }

    private static void browseDir(String path, boolean recursive, Callback callback) {
        if (!FileUtil.exists(path)) {
            return;
        }
        FileFilter filter = file -> {
            if (file.isDirectory()) {
                if (recursive) {
                    browseDir(file.getAbsolutePath(), true, callback);
                }
                return true;
            }
            return false;
        };
        Arrays.stream(new File(path).listFiles(filter)).forEach(callback::found);
    }

    public static void browse(String path, Callback callback) {
        browse(path, false, callback);
    }

    public static void browseRecursive(String path, String extension, Callback callback) {
        browse(path, true, callback);
    }

    private static void browse(String path, boolean recursive, Callback callback) {
        Arrays.stream(new File(path).listFiles()).forEach(callback::found);
    }

    public static void browseByExtension(String path, String extension, Callback callback) {
        browseByExtension(path, extension, false, callback);
    }

    public static void browseByExtensionRecursive(String path, String extension, Callback callback) {
        browseByExtension(path, extension, true, callback);
    }

    private static void browseByExtension(String path, String extension, boolean recursive, Callback callback) {
        FileFilter filter = file -> {
            if (file.isDirectory()) {
                if (recursive) {
                    browseByExtension(file.getAbsolutePath(), extension, true, callback);
                }
                return false;
            }
            return file.getAbsolutePath().endsWith(extension);
        };
        Arrays.stream(new File(path).listFiles(filter)).forEach(callback::found);
    }

    public static void browseByContains(String path, String string, Callback callback) {
        browseByContains(path, string, false, callback);
    }

    public static void browseByContainsRecursive(String path, String string, Callback callback) {
        browseByContains(path, string, true, callback);
    }

    private static void browseByContains(String path, String string, boolean recursive, Callback callback) {
        FileFilter filter = file -> {
            if (file.isDirectory()) {
                if (recursive) {
                    browseByExtension(file.getAbsolutePath(), string, true, callback);
                }
                return false;
            }
            return file.getAbsolutePath().contains(string);
        };
        Arrays.stream(new File(path).listFiles(filter)).forEach(callback::found);
    }

    public static void copy(String sourceDirectory, String destinationDirectory) throws IOException {
        File srcDir = new File(sourceDirectory);
        File destDir = new File(destinationDirectory);
        List<String> exclusionList = null;
        String srcDirCanonicalPath = srcDir.getCanonicalPath();
        String destDirCanonicalPath = destDir.getCanonicalPath();
        if (destDirCanonicalPath.startsWith(srcDirCanonicalPath)) {
            File[] srcFiles = listFiles(srcDir);
            if (srcFiles.length > 0) {
                exclusionList = new ArrayList<>(srcFiles.length);
                for (File srcFile : srcFiles) {
                    File copiedFile = new File(destDir, srcFile.getName());
                    exclusionList.add(copiedFile.getCanonicalPath());
                }
            }
        }
        doCopyDirectory(srcDir, destDir, exclusionList);
    }

    private static void doCopyDirectory(File srcDir, File destDir, List<String> exclusionList) throws IOException {
        File[] srcFiles = listFiles(srcDir);
        makeDirectory(destDir.getAbsolutePath());
        for (File srcFile : srcFiles) {
            File dstFile = new File(destDir, srcFile.getName());
            if (exclusionList == null || !exclusionList.contains(srcFile.getCanonicalPath())) {
                if (srcFile.isDirectory()) {
                    doCopyDirectory(srcFile, dstFile, exclusionList);
                } else {
                    FileUtil.copy(srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
                }
            }
        }
    }

    private static File[] listFiles(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Unknown I/O error listing contents of directory: " + directory);
        }
        return files;
    }

    public static boolean removeDirectory(String dir) {
        File directoryToBeDeleted = new File(dir);
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                removeDirectory(file.getAbsolutePath());
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static String getTempDirectory() {
        return StringUtil.rtrim(System.getProperty("java.io.tmpdir"), '/') + '/';
    }

    public static String makeTempDirectory() {
        String path = getTempDirectory() + "vt" + StringUtil.getRandomString(8) + '/';
        removeDirectory(path);
        return makeDirectory(path) ? path : null;
    }

    /**
     * true = dir exists now
     */
    public static boolean makeDirectory(String path) {
        if (FileUtil.exists(path)) {
            return true;
        }

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                Files.createDirectories(Paths.get(path));
                return true;
            } catch (IOException e) {
                FileUtil.log.error(" !! {}", path, e);
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
            FileUtil.log.error(" !! {}", path, e);
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
            FileUtil.log.error(" !! {}", path, e);
        }
    }

    public static void rename(String oldPath, String newPath) {
        (new File(oldPath)).renameTo(new File(newPath));
    }

    /**
     * Move/rename source directory to target. if target is not empty source contents will be copied into target
     * @param source
     * @param target
     */
    public static void move(String source, String target) {
        try {
            Files.move(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            FileUtil.log.error(" !! {} > {}", source, target, e);
        }
    }


    public interface Callback {

        void found(File file);
    }







    public interface Event {

        boolean found(File file);
    }

    public static boolean browseFile(String path, Event event) {
        File[] files = new File(path).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (browseFile(file.getAbsolutePath(), event)) {
                        return true;
                    }
                } else {
                    if (event.found(file)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
