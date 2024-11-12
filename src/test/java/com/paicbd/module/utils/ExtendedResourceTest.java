package com.paicbd.module.utils;

import com.paicbd.smsc.exception.RTException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtendedResourceTest {
    private static final String GW_NAME = "SS7GW";

    @Mock
    AppProperties appProperties;

    @InjectMocks
    ExtendedResource extendedResource;

    @ParameterizedTest
    @MethodSource("createDirectoriesParameters")
    @DisplayName("createDirectories, when a directory creation is requested (via classpath or path resource) then it returns the created directory path")
    void createDirectoriesWhenDirectoryCreationThenReturnDirPath(boolean isClassPath, String pathResource) {
        when(appProperties.getConfigPath()).thenReturn(pathResource);
        String path = extendedResource.createDirectory(GW_NAME);
        File file = new File(path);
        if (isClassPath) {
            assertTrue(path.contains("test-classes"));
        } else {
            assertTrue(path.contains(pathResource));
        }
        assertTrue(file.exists());
    }

    @Test
    @DisplayName("createDirectories when an invalid path is passed to the method, then a RTException is thrown")
    void createDirectoriesWhenWrongPathThenExceptionIsThrown() {
        when(appProperties.getConfigPath()).thenReturn("/etc/badPath");
        assertThrows(RTException.class, () -> extendedResource.createDirectory(GW_NAME));
    }

    @Test
    @DisplayName("deleteFiles when the files directory exists then the directory is removed")
    void deleteFilesWhenDirectoryExistsThenRemove() throws IOException {
        File mapFile = new File(new ClassPathResource("/").getFile().getAbsolutePath() + "/" + "map.xml");
        assertTrue(mapFile.createNewFile());
        extendedResource.deleteDirectory(mapFile);
        assertFalse(mapFile.exists());
    }

    @Test
    @DisplayName("deleteFilesException when an invalid path is passed to the method, then a RTException is thrown")
    void deleteFilesExceptionWhenWrongDirectoryThenExceptionIsThrown() {
        File mockDirectory = mock(File.class);
        when(mockDirectory.exists()).thenThrow(new RuntimeException("IO exception"));
        extendedResource = new ExtendedResource(appProperties);
        assertThrows(RTException.class, () -> extendedResource.deleteDirectory(mockDirectory));
    }

    @ParameterizedTest
    @MethodSource("deleteDirectoriesParameters")
    @DisplayName("deleteDirectories when directory exists then remove it whether there are files inside or not")
    void deleteDirectoriesWhenDirectoryExistsThenRemove(String fileToCreate, boolean sendInvalidDirectory) throws IOException {
        Resource resource = new ClassPathResource("/");
        String propertyPath = resource.getFile().getAbsolutePath() + "/";
        when(appProperties.getConfigPath()).thenReturn(propertyPath);
        String path = extendedResource.createDirectory(GW_NAME);
        File directory = new File(path);

        if (fileToCreate.isEmpty()) {
            //Testing folder deletion with no files inside
            assertTrue(directory.exists());
            extendedResource.deleteDirectory(directory);
            assertFalse(directory.exists());
        } else {
            //Testing folder deletion with files inside (folder + files)
            File file = new File(directory, fileToCreate);
            assertTrue(file.createNewFile());
            File directoryToDelete = sendInvalidDirectory ? new File("wrongDirectoryName") : directory;
            extendedResource.deleteDirectory(directoryToDelete);
            assertEquals(sendInvalidDirectory, directory.exists());
            assertEquals(sendInvalidDirectory, file.exists());
        }
    }

    static Stream<Arguments> createDirectoriesParameters() throws IOException {
        String classPathResource = new ClassPathResource("/").getFile().getAbsolutePath() + "/";
        return Stream.of(
                Arguments.of(Boolean.TRUE, classPathResource),
                Arguments.of(Boolean.FALSE, "")
        );
    }

    static Stream<Arguments> deleteDirectoriesParameters() {
        return Stream.of(
                Arguments.of("", Boolean.FALSE),
                Arguments.of("sctp.xml", Boolean.FALSE),
                Arguments.of("m3ua.xml", Boolean.FALSE),
                Arguments.of("sccp.xml", Boolean.FALSE),
                Arguments.of("tcap.xml", Boolean.FALSE),
                Arguments.of("map.xml", Boolean.FALSE),
                Arguments.of("m3ua.xml", Boolean.TRUE)
        );
    }
}
