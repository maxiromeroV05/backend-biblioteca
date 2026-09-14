package com.biblioteca.controller;

import com.biblioteca.dto.LibroDTO;
import com.biblioteca.entity.Autor;
import com.biblioteca.entity.Categoria;
import com.biblioteca.entity.Libro;
import com.biblioteca.service.LibroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints REST para gestionar el catálogo de libros.
 */
@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public ResponseEntity<List<LibroDTO>> listarTodos() {
        List<LibroDTO> libros = libroService.listarTodos().stream()
                .map(LibroDTO::desdeEntidad)
                .collect(Collectors.toList());
        return ResponseEntity.ok(libros);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<LibroDTO>> buscar(@RequestParam String q) {
        List<LibroDTO> libros = libroService.buscar(q).stream()
                .map(LibroDTO::desdeEntidad)
                .collect(Collectors.toList());
        return ResponseEntity.ok(libros);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibroDTO> buscarPorId(@PathVariable Long id) {
        Libro libro = libroService.buscarPorId(id);
        return ResponseEntity.ok(LibroDTO.desdeEntidad(libro));
    }

    @PostMapping
    public ResponseEntity<LibroDTO> crear(@Valid @RequestBody LibroDTO libroDTO) {
        Libro creado = libroService.crear(aEntidad(libroDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(LibroDTO.desdeEntidad(creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibroDTO> actualizar(@PathVariable Long id, @Valid @RequestBody LibroDTO libroDTO) {
        Libro actualizado = libroService.actualizar(id, aEntidad(libroDTO));
        return ResponseEntity.ok(LibroDTO.desdeEntidad(actualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convierte el DTO recibido a una entidad Libro. El autor y la
     * categoría se cargan solo con su id: el servicio es quien busca
     * las entidades completas y valida que realmente existan.
     */
    private Libro aEntidad(LibroDTO dto) {
        Libro libro = new Libro();
        libro.setId(dto.getId());
        libro.setTitulo(dto.getTitulo());
        libro.setIsbn(dto.getIsbn());
        libro.setEditorial(dto.getEditorial());
        libro.setAnioPublicacion(dto.getAnioPublicacion());
        libro.setCantidadTotal(dto.getCantidadTotal());

        Autor autor = new Autor();
        autor.setId(dto.getAutorId());
        libro.setAutor(autor);

        Categoria categoria = new Categoria();
        categoria.setId(dto.getCategoriaId());
        libro.setCategoria(categoria);

        return libro;
    }
}
