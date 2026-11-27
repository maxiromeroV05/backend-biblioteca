package com.biblioteca.service;

import com.biblioteca.entity.Prestamo;

import java.util.List;

public interface PrestamoService {

    List<Prestamo> listarTodos();

    Prestamo buscarPorId(Long id);

    List<Prestamo> listarPorUsuario(Long usuarioId);

    /**
     * Crea un préstamo nuevo. El parámetro prestamo debe traer, como
     * mínimo, el id del libro y el id del usuario cargados en sus
     * respectivos objetos.
     */
    Prestamo crear(Prestamo prestamo);

    Prestamo actualizar(Long id, Prestamo prestamo);

    Prestamo registrarDevolucion(Long id);

    void eliminar(Long id);
}
