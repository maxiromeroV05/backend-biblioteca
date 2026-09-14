package com.biblioteca.controller;

import com.biblioteca.dto.UsuarioDTO;
import com.biblioteca.entity.Usuario;
import com.biblioteca.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints REST para gestionar los usuarios/socios de la biblioteca.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        List<UsuarioDTO> usuarios = usuarioService.listarTodos().stream()
                .map(UsuarioDTO::desdeEntidad)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(UsuarioDTO.desdeEntidad(usuario));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        Usuario creado = usuarioService.crear(usuarioDTO.aEntidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDTO.desdeEntidad(creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuarioDTO) {
        Usuario actualizado = usuarioService.actualizar(id, usuarioDTO.aEntidad());
        return ResponseEntity.ok(UsuarioDTO.desdeEntidad(actualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
