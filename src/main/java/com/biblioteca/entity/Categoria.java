package com.biblioteca.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una categoría o género de libro (ej: Novela, Ciencia, Historia).
 */
@Entity
@Table(name = "categorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    // Navegación inversa: una categoría puede tener muchos libros.
    // Se ignora en el JSON para evitar ciclos infinitos al serializar.
    @OneToMany(mappedBy = "categoria")
    @JsonIgnore
    private List<Libro> libros = new ArrayList<>();
}
