package com.biblioteca.dto;

import com.biblioteca.entity.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO usado para exponer/recibir datos de Categoria en la API,
 * sin arrastrar la lista de libros de la entidad.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaDTO {

    private Long id;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    public static CategoriaDTO desdeEntidad(Categoria categoria) {
        return new CategoriaDTO(categoria.getId(), categoria.getNombre(), categoria.getDescripcion());
    }

    public Categoria aEntidad() {
        Categoria categoria = new Categoria();
        categoria.setId(this.id);
        categoria.setNombre(this.nombre);
        categoria.setDescripcion(this.descripcion);
        return categoria;
    }
}
