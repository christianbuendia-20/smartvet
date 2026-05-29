package com.smartvet.app.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Persiste un archivo de imagen en el directorio de uploads y devuelve
     * el nombre único generado. Retorna {@code null} si el archivo está vacío.
     */
    String guardarFotoMascota(MultipartFile archivo);
}
