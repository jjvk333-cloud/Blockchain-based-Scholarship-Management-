package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.entity.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Should store valid PDF file and return portable relative path")
    void testStoreValidPdf() {
        byte[] pdfContent = "%PDF-1.4 sample pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "student_marksheet.pdf",
                "application/pdf",
                pdfContent
        );

        String storedPath = fileStorageService.storeFile(file, 101L, DocumentType.MARKSHEET);

        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("uploads/documents/app_101_marksheet_"));
        assertTrue(storedPath.endsWith(".pdf"));
    }

    @Test
    @DisplayName("Should calculate consistent SHA-256 hash for identical file contents")
    void testCalculateSha256() {
        byte[] content = "ScholarTrust Immutable Test Content".getBytes();
        MockMultipartFile file1 = new MockMultipartFile("f1", "doc1.txt", "text/plain", content);
        MockMultipartFile file2 = new MockMultipartFile("f2", "doc2.txt", "text/plain", content);

        String hash1 = fileStorageService.calculateSha256(file1);
        String hash2 = fileStorageService.calculateSha256(file2);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should reject disallowed file extensions such as .exe")
    void testRejectDisallowedExtension() {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/x-msdownload",
                "malicious bytes".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.storeFile(maliciousFile, 102L, DocumentType.ID_PROOF));
        assertTrue(ex.getMessage().contains("Unsupported file extension"));
    }

    @Test
    @DisplayName("Should reject path traversal sequences in filename")
    void testRejectPathTraversalFilename() {
        MockMultipartFile fileWithTraversal = new MockMultipartFile(
                "file",
                "../../secret.pdf",
                "application/pdf",
                "%PDF-1.4 test".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.storeFile(fileWithTraversal, 103L, DocumentType.ID_PROOF));
        assertTrue(ex.getMessage().contains("Invalid path sequence"));
    }
}
