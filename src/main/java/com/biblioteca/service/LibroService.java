package com.biblioteca.service;

import com.biblioteca.entity.Libro;

import java.util.List;

public interface LibroService {

    List<Libro> listarTodos();

    Libro buscarPorId(Long id);

    List<Libro> buscar(String q);

    /**
     * Crea un libro nuevo. El parámetro libro debe traer, como mínimo,
     * el id del autor y el id de la categoría cargados en sus
     * respectivos objetos (libro.getAutor().getId(), etc); el servicio
     * se encarga de buscar las entidades completas.
     */
    Libro crear(Libro libro);

    Libro actualizar(Long id, Libro libro);

    void eliminar(Long id);

    void decrementarDisponibilidad(Long libroId);

    void incrementarDisponibilidad(Long libroId);
}
