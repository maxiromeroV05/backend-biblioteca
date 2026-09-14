package com.biblioteca.dto;

import com.biblioteca.entity.Libro;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de Libro. En lugar de exponer las entidades completas de Autor
 * y Categoria, solo se exponen sus id (para crear/editar) y su
 * nombre (autorNombre/categoriaNombre), pensado para que el frontend
 * pueda mostrar la info sin pedir varias veces al backend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibroDTO {

    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150)
    private String titulo;

    @NotBlank(message = "El ISBN es obligatorio")
    @Size(max = 20)
    private String isbn;

    @Size(max = 100)
    private String editorial;

    private Integer anioPublicacion;

    @NotNull(message = "La cantidad total es obligatoria")
    @Min(value = 0, message = "La cantidad total no puede ser negativa")
    private Integer cantidadTotal;

    // Este campo es de solo lectura para el cliente: lo calcula el
    // backend al crear el libro y al gestionar los préstamos.
    private Integer cantidadDisponible;

    @NotNull(message = "Debe indicarse el id del autor")
    private Long autorId;

    // Solo lectura, se completa al armar la respuesta.
    private String autorNombre;

    @NotNull(message = "Debe indicarse el id de la categoría")
    private Long categoriaId;

    // Solo lectura, se completa al armar la respuesta.
    private String categoriaNombre;

    /**
     * Convierte esta entidad a DTO completando los campos de solo
     * lectura con los datos del autor y la categoría relacionados.
     */
    public static LibroDTO desdeEntidad(Libro libro) {
        LibroDTO dto = new LibroDTO();
        dto.setId(libro.getId());
        dto.setTitulo(libro.getTitulo());
        dto.setIsbn(libro.getIsbn());
        dto.setEditorial(libro.getEditorial());
        dto.setAnioPublicacion(libro.getAnioPublicacion());
        dto.setCantidadTotal(libro.getCantidadTotal());
        dto.setCantidadDisponible(libro.getCantidadDisponible());
        if (libro.getAutor() != null) {
            dto.setAutorId(libro.getAutor().getId());
            dto.setAutorNombre(libro.getAutor().getNombre() + " " + libro.getAutor().getApellido());
        }
        if (libro.getCategoria() != null) {
            dto.setCategoriaId(libro.getCategoria().getId());
            dto.setCategoriaNombre(libro.getCategoria().getNombre());
        }
        return dto;
    }
}
