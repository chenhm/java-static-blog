package com.chenhm.blog.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

import org.springframework.util.Assert;

public class FileUtils {
    public static void copyRecursively(Path src, Path dest, Predicate<String> predicate) throws IOException {
        Assert.notNull(src, "Source Path must not be null");
        Assert.notNull(dest, "Destination Path must not be null");
        BasicFileAttributes srcAttr = Files.readAttributes(src, BasicFileAttributes.class);

        if (srcAttr.isDirectory()) {
            Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if(predicate.test(src.relativize(dir).toString()))
                        return FileVisitResult.SKIP_SUBTREE;
                    Files.createDirectories(dest.resolve(src.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(!predicate.test(src.relativize(file).toString())){
                        Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        else if (srcAttr.isRegularFile()) {
            Files.copy(src, dest);
        }
        else {
            throw new IllegalArgumentException("Source File must denote a directory or file");
        }
    }

}
