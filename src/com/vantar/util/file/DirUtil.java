package com.vantar.util.file;

import java.io.*;
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
        FileUtil.makeDirectory(destDir.getAbsolutePath());
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


    public interface Callback {

        void found(File file);
    }
}
