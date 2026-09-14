package com.biblioteca.controller;

import com.biblioteca.dto.CategoriaDTO;
import com.biblioteca.entity.Categoria;
import com.biblioteca.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints REST para gestionar las categorías de libros.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> listarTodas() {
        List<CategoriaDTO> categorias = categoriaService.listarTodas().stream()
                .map(CategoriaDTO::desdeEntidad)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> buscarPorId(@PathVariable Long id) {
        Categoria categoria = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(CategoriaDTO.desdeEntidad(categoria));
    }

    @PostMapping
    public ResponseEntity<CategoriaDTO> crear(@Valid @RequestBody CategoriaDTO categoriaDTO) {
        Categoria creada = categoriaService.crear(categoriaDTO.aEntidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaDTO.desdeEntidad(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaDTO categoriaDTO) {
        Categoria actualizada = categoriaService.actualizar(id, categoriaDTO.aEntidad());
        return ResponseEntity.ok(CategoriaDTO.desdeEntidad(actualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
