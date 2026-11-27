package com.biblioteca.controller;

import com.biblioteca.dto.kafka.metrics.KafkaMetricsResponse;
import com.biblioteca.service.KafkaMetricasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instantánea de métricas para el panel (polling REST).
 *
 * GET /api/kafka/metrics -> offsets, lag por consumer group, distribución del
 * consumo, eventos/minuto y fallos. El intervalo recomendado viaja en la
 * respuesta y también se puede fijar con app.kafka.metrics.polling-interval-seconds.
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaMetricsController {

    private final KafkaMetricasService metricasService;

    public KafkaMetricsController(KafkaMetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<KafkaMetricsResponse> obtener() {
        return ResponseEntity.ok(metricasService.obtener());
    }
}
