package com.biblioteca.service.impl;

import com.biblioteca.entity.Autor;
import com.biblioteca.exception.OperacionInvalidaException;
import com.biblioteca.exception.RecursoNoEncontradoException;
import com.biblioteca.repository.AutorRepository;
import com.biblioteca.service.AutorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutorServiceImpl implements AutorService {

    private final AutorRepository autorRepository;

    public AutorServiceImpl(AutorRepository autorRepository) {
        this.autorRepository = autorRepository;
    }

    @Override
    public List<Autor> listarTodos() {
        return autorRepository.findAll();
    }

    @Override
    public Autor buscarPorId(Long id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el autor con id " + id));
    }

    @Override
    public Autor crear(Autor autor) {
        autor.setId(null);
        return autorRepository.save(autor);
    }

    @Override
    public Autor actualizar(Long id, Autor autor) {
        Autor existente = buscarPorId(id);
        existente.setNombre(autor.getNombre());
        existente.setApellido(autor.getApellido());
        existente.setNacionalidad(autor.getNacionalidad());
        existente.setFechaNacimiento(autor.getFechaNacimiento());
        existente.setBiografia(autor.getBiografia());
        return autorRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Autor autor = buscarPorId(id);
        if (!autor.getLibros().isEmpty()) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar el autor porque tiene libros asociados");
        }
        autorRepository.delete(autor);
    }
}
