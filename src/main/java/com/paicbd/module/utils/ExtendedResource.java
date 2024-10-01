package com.paicbd.module.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtendedResource {

    private final AppProperties appProperties;

    public String createDirectory(String name) throws IOException {
        String pathData = getDirectoryPath() + name;
        Path path = Paths.get(pathData);
        Files.createDirectories(path);
        return pathData;
    }


    public void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            if (directory.isFile()) {
                deleteFile(directory.getPath());
            } else {
                File[] listFiles = directory.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        deleteFile(file.getPath());
                    }
                }
                deleteFile(directory.getPath());
            }
        }
    }

    private void deleteFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        Files.delete(filePath);
    }

    private String getDirectoryPath() throws IOException {
        boolean isConfigPath = appProperties.getConfigPath().isEmpty();
        if (isConfigPath) {
            Resource resource = new ClassPathResource("/");
            return resource.getFile().getAbsolutePath() + "/";
        }
        return appProperties.getConfigPath();
    }

}
