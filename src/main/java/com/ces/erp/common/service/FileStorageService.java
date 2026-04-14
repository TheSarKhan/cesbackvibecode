package com.ces.erp.common.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir).toAbsolutePath());
        } catch (IOException e) {
            throw new FileStorageException("Yükləmə qovluğu yaradıla bilmədi: " + uploadDir, e);
        }
    }

    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Fayl boş ola bilməz");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;

        try {
            Path dir = Paths.get(uploadDir, subDir).toAbsolutePath();
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }

            return subDir + "/" + storedName;
        } catch (IOException e) {
            throw new FileStorageException("Fayl saxlanıla bilmədi: " + e.getMessage());
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null) return;
        try {
            Path path = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Fayl silinə bilmədi: {}", relativePath);
        }
    }

    public Path resolve(String relativePath) {
        return Paths.get(uploadDir).resolve(relativePath);
    }
}
