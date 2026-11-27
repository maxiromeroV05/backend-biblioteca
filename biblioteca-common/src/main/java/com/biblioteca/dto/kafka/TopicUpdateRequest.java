package com.biblioteca.dto.kafka;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * Datos para modificar un tópico (PUT /api/kafka/topics/{name}).
 * Hay que indicar al menos uno de los dos campos.
 *
 * @param partitions nuevo número de particiones. Kafka solo permite AUMENTAR
 *                   las particiones, nunca reducirlas.
 * @param configs    configuraciones a cambiar. Un valor vacío o null elimina el
 *                   override y el tópico vuelve al valor por defecto del broker.
 */
public record TopicUpdateRequest(
        @Min(value = 1, message = "Debe haber al menos 1 partición")
        @Max(value = 1000, message = "El máximo permitido es 1000 particiones")
        Integer partitions,

        Map<String, String> configs
) {
}
