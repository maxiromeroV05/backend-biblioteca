package com.biblioteca.service;

import com.biblioteca.entity.Categoria;

import java.util.List;

public interface CategoriaService {

    List<Categoria> listarTodas();

    Categoria buscarPorId(Long id);

    Categoria crear(Categoria categoria);

    Categoria actualizar(Long id, Categoria categoria);

    void eliminar(Long id);
}
