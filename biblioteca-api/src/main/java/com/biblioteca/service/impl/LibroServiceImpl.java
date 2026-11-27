package com.biblioteca.service.impl;

import com.biblioteca.entity.Autor;
import com.biblioteca.entity.Categoria;
import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Libro;
import com.biblioteca.exception.OperacionInvalidaException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.exception.RegistroDuplicadoException;
import com.biblioteca.repository.AutorRepository;
import com.biblioteca.repository.CategoriaRepository;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.service.LibroService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibroServiceImpl implements LibroService {

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrestamoRepository prestamoRepository;

    public LibroServiceImpl(LibroRepository libroRepository,
                             AutorRepository autorRepository,
                             CategoriaRepository categoriaRepository,
                             PrestamoRepository prestamoRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.categoriaRepository = categoriaRepository;
        this.prestamoRepository = prestamoRepository;
    }

    @Override
    public List<Libro> listarTodos() {
        return libroRepository.findAll();
    }

    @Override
    public Libro buscarPorId(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el libro con id " + id));
    }

    @Override
    public List<Libro> buscar(String q) {
        return libroRepository.findByTituloContainingIgnoreCaseOrIsbnContainingIgnoreCase(q, q);
    }

    @Override
    public Libro crear(Libro libro) {
        libroRepository.findByIsbn(libro.getIsbn()).ifPresent(existente -> {
            throw new RegistroDuplicadoException("Ya existe un libro con el ISBN '" + libro.getIsbn() + "'");
        });

        Autor autor = buscarAutor(libro.getAutor().getId());
        Categoria categoria = buscarCategoria(libro.getCategoria().getId());

        libro.setId(null);
        libro.setAutor(autor);
        libro.setCategoria(categoria);
        // Al crear el libro, todos los ejemplares están disponibles.
        libro.setCantidadDisponible(libro.getCantidadTotal());

        return libroRepository.save(libro);
    }

    @Override
    public Libro actualizar(Long id, Libro libro) {
        Libro existente = buscarPorId(id);

        if (!existente.getIsbn().equalsIgnoreCase(libro.getIsbn())) {
            libroRepository.findByIsbn(libro.getIsbn()).ifPresent(otro -> {
                throw new RegistroDuplicadoException("Ya existe un libro con el ISBN '" + libro.getIsbn() + "'");
            });
        }

        Autor autor = buscarAutor(libro.getAutor().getId());
        Categoria categoria = buscarCategoria(libro.getCategoria().getId());

        // Si la cantidad total cambia, hay que ajustar la disponible
        // en la misma proporción, sin perder los préstamos ya activos.
        int prestados = existente.getCantidadTotal() - existente.getCantidadDisponible();
        int nuevaDisponible = libro.getCantidadTotal() - prestados;
        if (nuevaDisponible < 0) {
            throw new OperacionInvalidaException(
                    "No se puede reducir la cantidad total por debajo de los ejemplares actualmente prestados");
        }

        existente.setTitulo(libro.getTitulo());
        existente.setIsbn(libro.getIsbn());
        existente.setEditorial(libro.getEditorial());
        existente.setAnioPublicacion(libro.getAnioPublicacion());
        existente.setCantidadTotal(libro.getCantidadTotal());
        existente.setCantidadDisponible(nuevaDisponible);
        existente.setAutor(autor);
        existente.setCategoria(categoria);

        return libroRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Libro libro = buscarPorId(id);
        if (prestamoRepository.existsByLibroIdAndEstado(id, EstadoPrestamo.ACTIVO)) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar el libro porque tiene préstamos activos");
        }
        libroRepository.delete(libro);
    }

    @Override
    public void decrementarDisponibilidad(Long libroId) {
        Libro libro = buscarPorId(libroId);
        if (libro.getCantidadDisponible() <= 0) {
            throw new OperacionInvalidaException("No quedan ejemplares disponibles de este libro");
        }
        libro.setCantidadDisponible(libro.getCantidadDisponible() - 1);
        libroRepository.save(libro);
    }

    @Override
    public void incrementarDisponibilidad(Long libroId) {
        Libro libro = buscarPorId(libroId);
        if (libro.getCantidadDisponible() < libro.getCantidadTotal()) {
            libro.setCantidadDisponible(libro.getCantidadDisponible() + 1);
            libroRepository.save(libro);
        }
    }

    private Autor buscarAutor(Long autorId) {
        return autorRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el autor con id " + autorId));
    }

    private Categoria buscarCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la categoría con id " + categoriaId));
    }
}
