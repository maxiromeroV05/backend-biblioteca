package com.biblioteca.dto;

import com.biblioteca.entity.Autor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO usado para exponer/recibir datos de Autor en la API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutorDTO {

    private Long id;

    @NotBlank(message = "El nombre del autor es obligatorio")
    @Size(max = 80)
    private String nombre;

    @NotBlank(message = "El apellido del autor es obligatorio")
    @Size(max = 80)
    private String apellido;

    @Size(max = 80)
    private String nacionalidad;

    private LocalDate fechaNacimiento;

    @Size(max = 1000)
    private String biografia;

    public static AutorDTO desdeEntidad(Autor autor) {
        return new AutorDTO(
                autor.getId(),
                autor.getNombre(),
                autor.getApellido(),
                autor.getNacionalidad(),
                autor.getFechaNacimiento(),
                autor.getBiografia()
        );
    }

    public Autor aEntidad() {
        Autor autor = new Autor();
        autor.setId(this.id);
        autor.setNombre(this.nombre);
        autor.setApellido(this.apellido);
        autor.setNacionalidad(this.nacionalidad);
        autor.setFechaNacimiento(this.fechaNacimiento);
        autor.setBiografia(this.biografia);
        return autor;
    }
}
