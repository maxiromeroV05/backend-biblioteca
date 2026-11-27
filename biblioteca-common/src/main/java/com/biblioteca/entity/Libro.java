package com.biblioteca.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un libro del catálogo de la biblioteca.
 * cantidadTotal: cantidad de ejemplares que la biblioteca posee.
 * cantidadDisponible: cantidad de ejemplares que NO están prestados
 * en este momento (se actualiza cada vez que se crea o se devuelve
 * un préstamo).
 */
@Entity
@Table(name = "libros")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(length = 100)
    private String editorial;

    private Integer anioPublicacion;

    @Column(nullable = false)
    private Integer cantidadTotal;

    @Column(nullable = false)
    private Integer cantidadDisponible;

    @ManyToOne
    @JoinColumn(name = "autor_id")
    private Autor autor;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @OneToMany(mappedBy = "libro")
    @JsonIgnore
    private List<Prestamo> prestamos = new ArrayList<>();
}
