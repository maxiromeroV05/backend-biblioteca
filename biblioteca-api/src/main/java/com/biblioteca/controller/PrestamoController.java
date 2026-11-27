package com.biblioteca.controller;

import com.biblioteca.dto.PrestamoDTO;
import com.biblioteca.entity.Libro;
import com.biblioteca.entity.Prestamo;
import com.biblioteca.entity.Usuario;
import com.biblioteca.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints REST para gestionar los préstamos de libros.
 */
@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @GetMapping
    public ResponseEntity<List<PrestamoDTO>> listarTodos() {
        List<PrestamoDTO> prestamos = prestamoService.listarTodos().stream()
                .map(PrestamoDTO::desdeEntidad)
                .collect(Collectors.toList());
        return ResponseEntity.ok(prestamos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestamoDTO> buscarPorId(@PathVariable Long id) {
        Prestamo prestamo = prestamoService.buscarPorId(id);
        return ResponseEntity.ok(PrestamoDTO.desdeEntidad(prestamo));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<PrestamoDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        List<PrestamoDTO> prestamos = prestamoService.listarPorUsuario(usuarioId).stream()
                .map(PrestamoDTO::desdeEntidad)
                .collect(Collectors.toList());
        return ResponseEntity.ok(prestamos);
    }

    @PostMapping
    public ResponseEntity<PrestamoDTO> crear(@Valid @RequestBody PrestamoDTO prestamoDTO) {
        Prestamo creado = prestamoService.crear(aEntidad(prestamoDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(PrestamoDTO.desdeEntidad(creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrestamoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody PrestamoDTO prestamoDTO) {
        Prestamo actualizado = prestamoService.actualizar(id, aEntidad(prestamoDTO));
        return ResponseEntity.ok(PrestamoDTO.desdeEntidad(actualizado));
    }

    @PatchMapping("/{id}/devolver")
    public ResponseEntity<PrestamoDTO> registrarDevolucion(@PathVariable Long id) {
        Prestamo devuelto = prestamoService.registrarDevolucion(id);
        return ResponseEntity.ok(PrestamoDTO.desdeEntidad(devuelto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        prestamoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convierte el DTO recibido a una entidad Prestamo. El libro y el
     * usuario se cargan solo con su id: el servicio busca las
     * entidades completas y valida que existan y que haya
     * disponibilidad.
     */
    private Prestamo aEntidad(PrestamoDTO dto) {
        Prestamo prestamo = new Prestamo();
        prestamo.setId(dto.getId());
        prestamo.setFechaDevolucionEsperada(dto.getFechaDevolucionEsperada());
        prestamo.setObservaciones(dto.getObservaciones());

        Libro libro = new Libro();
        libro.setId(dto.getLibroId());
        prestamo.setLibro(libro);

        Usuario usuario = new Usuario();
        usuario.setId(dto.getUsuarioId());
        prestamo.setUsuario(usuario);

        return prestamo;
    }
}
