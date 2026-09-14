package com.biblioteca.dto;

import com.biblioteca.entity.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO usado para exponer/recibir datos de Usuario en la API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 80)
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 120)
    private String email;

    @Size(max = 20)
    private String telefono;

    @Size(max = 200)
    private String direccion;

    // Solo lectura: se asigna automáticamente al crear el usuario.
    private LocalDate fechaRegistro;

    private Boolean activo;

    public static UsuarioDTO desdeEntidad(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getDireccion(),
                usuario.getFechaRegistro(),
                usuario.getActivo()
        );
    }

    public Usuario aEntidad() {
        Usuario usuario = new Usuario();
        usuario.setId(this.id);
        usuario.setNombre(this.nombre);
        usuario.setApellido(this.apellido);
        usuario.setEmail(this.email);
        usuario.setTelefono(this.telefono);
        usuario.setDireccion(this.direccion);
        return usuario;
    }
}
