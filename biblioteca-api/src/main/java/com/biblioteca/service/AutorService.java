package com.biblioteca.service;

import com.biblioteca.entity.Autor;

import java.util.List;

public interface AutorService {

    List<Autor> listarTodos();

    Autor buscarPorId(Long id);

    Autor crear(Autor autor);

    Autor actualizar(Long id, Autor autor);

    void eliminar(Long id);
}
