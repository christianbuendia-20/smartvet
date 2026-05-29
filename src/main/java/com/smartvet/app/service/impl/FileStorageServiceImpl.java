package com.smartvet.app.service.impl;

import com.smartvet.app.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "mascotas");

    @Override
    public String guardarFotoMascota(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        try {
            Files.createDirectories(UPLOAD_DIR);

            String nombreUnico = UUID.randomUUID() + "_" + archivo.getOriginalFilename();
            Path destino = UPLOAD_DIR.resolve(nombreUnico).normalize();
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            log.info("Foto de mascota guardada: {}", destino.toAbsolutePath());
            return nombreUnico;
        } catch (IOException ex) {
            log.error("Error al guardar foto de mascota: {}", ex.getMessage(), ex);
            throw new RuntimeException("No se pudo guardar la imagen. Inténtalo de nuevo.", ex);
        }
    }
}
