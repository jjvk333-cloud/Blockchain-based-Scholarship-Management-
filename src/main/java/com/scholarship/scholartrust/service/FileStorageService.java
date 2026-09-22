package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.entity.DocumentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".jpg", ".jpeg", ".png", ".txt");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "text/plain"
    );

    public FileStorageService(@Value("${file.upload-dir:uploads/documents}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create directory where uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, Long applicationId, DocumentType docType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum allowed size of 10MB: " + file.getSize() + " bytes");
        }

        String rawFileName = file.getOriginalFilename();
        if (rawFileName == null || rawFileName.isBlank()) {
            rawFileName = "document.bin";
        }
        String cleanFileName = StringUtils.cleanPath(rawFileName);

        if (cleanFileName.contains("..")) {
            throw new IllegalArgumentException("Invalid path sequence in filename: " + cleanFileName);
        }

        // Validate extension
        String lowerCaseName = cleanFileName.toLowerCase(Locale.ROOT);
        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerCaseName::endsWith);
        if (!validExt) {
            throw new IllegalArgumentException("Unsupported file extension. Allowed extensions: " + ALLOWED_EXTENSIONS);
        }

        // Validate content type if provided
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            boolean validMime = ALLOWED_CONTENT_TYPES.stream()
                    .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType) || contentType.startsWith(allowed));
            if (!validMime) {
                throw new IllegalArgumentException("Unsupported file MIME type: " + contentType + ". Allowed: " + ALLOWED_CONTENT_TYPES);
            }
        }

        // Sanitize base name
        String safeBaseName = cleanFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Generate unique filename: app_{id}_{docType}_{uuid}_{safeBaseName}
        String uniqueFileName = String.format("app_%d_%s_%s_%s",
                applicationId,
                docType.name().toLowerCase(Locale.ROOT),
                UUID.randomUUID().toString().substring(0, 8),
                safeBaseName);

        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName).normalize();
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new IllegalArgumentException("Security violation: Target file path is outside upload directory.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            // Return portable relative path
            return "uploads/documents/" + uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + uniqueFileName + ". Please try again!", ex);
        }
    }

    public String calculateSha256(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return calculateSha256(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Could not read file input stream for SHA-256 calculation.", ex);
        }
    }

    public String calculateSha256(Path filePath) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return calculateSha256(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Could not read file at " + filePath + " for SHA-256 recalculation.", ex);
        }
    }

    private String calculateSha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesCount;
            while ((bytesCount = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesCount);
            }
            byte[] bytes = digest.digest();
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new RuntimeException("Error computing SHA-256 hash digest", ex);
        }
    }

    public Resource loadFileAsResource(String filePathString) {
        try {
            Path filePath = resolvePath(filePathString);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found or not readable: " + filePathString);
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("File path is invalid: " + filePathString, ex);
        }
    }

    public Path resolvePath(String filePathString) {
        Path p = Paths.get(filePathString);
        if (p.isAbsolute() && Files.exists(p)) {
            return p.normalize();
        }
        // Try resolving filename or relative path against fileStorageLocation
        String fileNameOnly = p.getFileName() != null ? p.getFileName().toString() : filePathString;
        Path inStorage = this.fileStorageLocation.resolve(fileNameOnly).normalize();
        if (Files.exists(inStorage)) {
            return inStorage;
        }
        // Try resolving as relative path from current working directory
        Path fromCwd = Paths.get(".").toAbsolutePath().normalize().resolve(filePathString).normalize();
        if (Files.exists(fromCwd)) {
            return fromCwd;
        }
        return inStorage;
    }
}