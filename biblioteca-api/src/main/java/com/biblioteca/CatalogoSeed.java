package com.biblioteca;

import com.biblioteca.entity.Autor;
import com.biblioteca.entity.Categoria;
import com.biblioteca.entity.Libro;
import com.biblioteca.repository.AutorRepository;
import com.biblioteca.repository.CategoriaRepository;
import com.biblioteca.repository.LibroRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Catálogo de ejemplo para el entorno local (H2 en memoria).
 * No se activa con el perfil "cloud" para no interferir con la base real.
 */
@Configuration
@Profile("!cloud")
public class CatalogoSeed {

    @Bean
    CommandLineRunner sembrarCatalogo(
            AutorRepository autores,
            CategoriaRepository categorias,
            LibroRepository libros) {
        return args -> {
            Categoria novela = categoria(categorias, "Novela", "Obras de ficción narrativa");
            Categoria historia = categoria(categorias, "Historia", "Divulgación histórica");
            Categoria tecnologia = categoria(categorias, "Tecnología", "Libros técnicos y de desarrollo");

            Autor saintExupery = autor(autores, "Antoine de", "Saint-Exupéry", "Francesa");
            Autor garciaMarquez = autor(autores, "Gabriel García", "Márquez", "Colombiana");
            Autor harari = autor(autores, "Yuval Noah", "Harari", "Israelí");
            Autor martin = autor(autores, "Robert C.", "Martin", "Estadounidense");

            libro(libros, "El principito", "978-84-206-3598-4", "Salamandra", 1943, 4,
                    saintExupery, novela);
            libro(libros, "Cien años de soledad", "978-84-376-0494-7", "Sudamericana", 1967, 3,
                    garciaMarquez, novela);
            libro(libros, "Sapiens: De animales a dioses", "978-84-9992-394-4", "Debate", 2014, 5,
                    harari, historia);
            libro(libros, "Clean Code", "978-84-92923-60-4", "Anaya", 2008, 2,
                    martin, tecnologia);
        };
    }

    private Categoria categoria(CategoriaRepository repo, String nombre, String descripcion) {
        return repo.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Categoria categoria = new Categoria();
            categoria.setNombre(nombre);
            categoria.setDescripcion(descripcion);
            return repo.save(categoria);
        });
    }

    private Autor autor(AutorRepository repo, String nombre, String apellido, String nacionalidad) {
        return repo.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(nombre, apellido)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Autor autor = new Autor();
                    autor.setNombre(nombre);
                    autor.setApellido(apellido);
                    autor.setNacionalidad(nacionalidad);
                    return repo.save(autor);
                });
    }

    private void libro(LibroRepository repo, String titulo, String isbn, String editorial,
                       Integer anio, Integer cantidad, Autor autor, Categoria categoria) {
        if (repo.findByIsbn(isbn).isPresent()) {
            return;
        }

        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setIsbn(isbn);
        libro.setEditorial(editorial);
        libro.setAnioPublicacion(anio);
        libro.setCantidadTotal(cantidad);
        libro.setCantidadDisponible(cantidad);
        libro.setAutor(autor);
        libro.setCategoria(categoria);
        repo.save(libro);
    }
}
