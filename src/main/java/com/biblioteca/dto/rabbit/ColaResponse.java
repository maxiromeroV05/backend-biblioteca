package com.biblioteca.dto.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cola tal como la ve el frontend (incluye estadísticas de solo lectura). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColaResponse {

    private String name;
    private boolean durable;
    private String deadLetterExchange;
    private String deadLetterRoutingKey;
    private Long messageTtl;
    private long messages;
    private long consumers;
}
