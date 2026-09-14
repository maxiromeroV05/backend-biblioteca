package com.biblioteca.controller;

import com.biblioteca.dto.AutorDTO;
import com.biblioteca.entity.Autor;
import com.biblioteca.service.AutorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints REST para gestionar los autores.
 */
@RestController
@RequestMapping("/api/autores")
public class AutorController {

    private final AutorService autorService;

    public AutorController(AutorService autorService) {
        this.autorService = autorService;
    }

    @GetMapping
    public ResponseEntity<List<AutorDTO>> listarTodos() {
        List<AutorDTO> autores = autorService.listarTodos().stream()
                .map(AutorDTO::desdeEntidad)
                .collect(Collectors.toList());
        return ResponseEntity.ok(autores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AutorDTO> buscarPorId(@PathVariable Long id) {
        Autor autor = autorService.buscarPorId(id);
        return ResponseEntity.ok(AutorDTO.desdeEntidad(autor));
    }

    @PostMapping
    public ResponseEntity<AutorDTO> crear(@Valid @RequestBody AutorDTO autorDTO) {
        Autor creado = autorService.crear(autorDTO.aEntidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(AutorDTO.desdeEntidad(creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AutorDTO> actualizar(@PathVariable Long id, @Valid @RequestBody AutorDTO autorDTO) {
        Autor actualizado = autorService.actualizar(id, autorDTO.aEntidad());
        return ResponseEntity.ok(AutorDTO.desdeEntidad(actualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        autorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
