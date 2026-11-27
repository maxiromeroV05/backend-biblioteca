package com.biblioteca.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa al autor de uno o varios libros.
 */
@Entity
@Table(name = "autores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Column(length = 80)
    private String nacionalidad;

    private LocalDate fechaNacimiento;

    @Column(length = 1000)
    private String biografia;

    @OneToMany(mappedBy = "autor")
    @JsonIgnore
    private List<Libro> libros = new ArrayList<>();
}
