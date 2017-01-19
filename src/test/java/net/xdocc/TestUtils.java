/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 *
 * @author draft
 */
public class TestUtils {
    public static void copyFile(String source, Path src, String dst) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(source);
        if(in == null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream("/"+source);
        }
        Path dstPath = src.resolve(dst);
        Files.createDirectories(dstPath.getParent());
        Files.copy(in, dstPath);
        in.close();
    }
    
    public static void createFile(Path source, String path, String content)
            throws IOException {
        Path file = source.resolve(path);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        Files.write(file, content.getBytes());
    }
    
    public static void deleteDirectories(Path... paths) throws IOException {
        for(Path path:paths) {
            deleteDirectory(path);
        }
    }

    public static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
