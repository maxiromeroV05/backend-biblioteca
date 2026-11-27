package com.biblioteca.service.impl;

import com.biblioteca.entity.Categoria;
import com.biblioteca.exception.OperacionInvalidaException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.exception.RegistroDuplicadoException;
import com.biblioteca.repository.CategoriaRepository;
import com.biblioteca.service.CategoriaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // Inyección de dependencias por constructor (buena práctica:
    // permite que el campo sea final y facilita hacer tests).
    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    @Override
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la categoría con id " + id));
    }

    @Override
    public Categoria crear(Categoria categoria) {
        categoriaRepository.findByNombreIgnoreCase(categoria.getNombre()).ifPresent(existente -> {
            throw new RegistroDuplicadoException("Ya existe una categoría con el nombre '" + categoria.getNombre() + "'");
        });
        categoria.setId(null); // por si el cliente manda un id, lo ignoramos al crear
        return categoriaRepository.save(categoria);
    }

    @Override
    public Categoria actualizar(Long id, Categoria categoria) {
        Categoria existente = buscarPorId(id);

        // Si cambia el nombre, hay que validar que el nuevo nombre no esté en uso por otra categoría.
        if (!existente.getNombre().equalsIgnoreCase(categoria.getNombre())) {
            categoriaRepository.findByNombreIgnoreCase(categoria.getNombre()).ifPresent(otra -> {
                throw new RegistroDuplicadoException("Ya existe una categoría con el nombre '" + categoria.getNombre() + "'");
            });
        }

        existente.setNombre(categoria.getNombre());
        existente.setDescripcion(categoria.getDescripcion());
        return categoriaRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);
        if (!categoria.getLibros().isEmpty()) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar la categoría porque tiene libros asociados");
        }
        categoriaRepository.delete(categoria);
    }
}
