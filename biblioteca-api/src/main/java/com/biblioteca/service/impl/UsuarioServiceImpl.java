package com.biblioteca.service.impl;

import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Usuario;
import com.biblioteca.exception.OperacionInvalidaException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.exception.RegistroDuplicadoException;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.UsuarioRepository;
import com.biblioteca.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PrestamoRepository prestamoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con id " + id));
    }

    @Override
    public Usuario crear(Usuario usuario) {
        usuarioRepository.findByEmail(usuario.getEmail()).ifPresent(existente -> {
            throw new RegistroDuplicadoException("Ya existe un usuario registrado con el email '" + usuario.getEmail() + "'");
        });
        usuario.setId(null);
        usuario.setFechaRegistro(LocalDate.now());
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario actualizar(Long id, Usuario usuario) {
        Usuario existente = buscarPorId(id);

        if (!existente.getEmail().equalsIgnoreCase(usuario.getEmail())) {
            usuarioRepository.findByEmail(usuario.getEmail()).ifPresent(otro -> {
                throw new RegistroDuplicadoException("Ya existe un usuario registrado con el email '" + usuario.getEmail() + "'");
            });
        }

        existente.setNombre(usuario.getNombre());
        existente.setApellido(usuario.getApellido());
        existente.setEmail(usuario.getEmail());
        existente.setTelefono(usuario.getTelefono());
        existente.setDireccion(usuario.getDireccion());
        return usuarioRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Usuario usuario = buscarPorId(id);
        if (prestamoRepository.existsByUsuarioIdAndEstado(id, EstadoPrestamo.ACTIVO)) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar el usuario porque tiene préstamos activos");
        }
        usuarioRepository.delete(usuario);
    }
}
