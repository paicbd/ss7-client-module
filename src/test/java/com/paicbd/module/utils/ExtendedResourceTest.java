package com.paicbd.module.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtendedResourceTest {
    private final String gwName = "SS7GW";

    @Mock
    AppProperties appProperties;

    @InjectMocks
    ExtendedResource extendedResource;


    @Test
    void createDirectory_classPathResource() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        String path = extendedResource.createDirectory(gwName);
        assertTrue(path.contains("test-classes"));
    }

    @Test
    void createDirectory_propertyPath() throws IOException {
        Resource resource = new ClassPathResource("/");
        String propertyPath = resource.getFile().getAbsolutePath() + "/";
        when(appProperties.getConfigPath()).thenReturn(propertyPath);
        String path = extendedResource.createDirectory(gwName);
        assertTrue(path.contains(propertyPath));
    }


    @Test
    void deleteFile() throws IOException {
        Resource resource = new ClassPathResource("/");
        String propertyPath = resource.getFile().getAbsolutePath() + "/";
        File mapFile = new File(propertyPath + "map.xml");
        assertTrue(mapFile.createNewFile());
        extendedResource.deleteDirectory(mapFile);
        assertFalse(mapFile.exists());
    }


    @Test
    void deleteDirectory_empty() throws IOException {
        Resource resource = new ClassPathResource("/");
        String propertyPath = resource.getFile().getAbsolutePath() + "/";
        when(appProperties.getConfigPath()).thenReturn(propertyPath);
        String path = extendedResource.createDirectory(gwName);
        File dir = new File(path);
        assertTrue(dir.exists());
        extendedResource.deleteDirectory(dir);
        assertFalse(dir.exists());
    }


    @Test
    void deleteDirectory_withFiles() throws IOException {
        Resource resource = new ClassPathResource("/");
        String propertyPath = resource.getFile().getAbsolutePath() + "/";
        when(appProperties.getConfigPath()).thenReturn(propertyPath);
        String path = extendedResource.createDirectory(gwName);
        File dir = new File(path);
        File sctpFile = new File(dir, "sctp.xml");
        File m3uaFile = new File(dir, "m3ua.xml");
        File sccpFile = new File(dir, "sccp.xml");
        File tcapFile = new File(dir, "tcap.xml");
        File mapFile = new File(dir, "map.xml");
        assertTrue(sctpFile.createNewFile());
        assertTrue(m3uaFile.createNewFile());
        assertTrue(sccpFile.createNewFile());
        assertTrue(tcapFile.createNewFile());
        assertTrue(mapFile.createNewFile());
        extendedResource.deleteDirectory(dir);
        assertFalse(dir.exists());
        assertFalse(sctpFile.exists());
        assertFalse(m3uaFile.exists());
        assertFalse(sccpFile.exists());
        assertFalse(tcapFile.exists());
        assertFalse(mapFile.exists());
    }

}