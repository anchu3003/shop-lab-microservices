package com.shoplab.inventory.health;

import com.shoplab.commons.health.HealthCheckResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hai probe với mục đích khác nhau — nhầm hai cái này là nguyên nhân kinh điển của "service tự
 * sát":
 *
 * <ul>
 *   <li>{@code /health} (liveness) — <b>không</b> check dependency. Process còn sống là 200.
 *   <li>{@code /ready} (readiness) — <b>có</b> check dependency; có cái chết thì 503 để load
 *       balancer tạm ngừng đẩy traffic vào instance này, nhưng không restart nó.
 * </ul>
 */
@RestController
public class HealthController {

  private final ReadinessService readinessService;

  public HealthController(ReadinessService readinessService) {
    this.readinessService = readinessService;
  }

  /** Liveness probe — cố ý không chạm dependency nào. */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(Map.of("status", "UP"));
  }

  /** Readiness probe — 200 khi mọi dependency UP, 503 khi có cái DOWN (kèm lý do để debug). */
  @GetMapping("/ready")
  public ResponseEntity<Map<String, Object>> ready() {
    List<HealthCheckResult> results = readinessService.checkAll();
    boolean ready = results.stream().allMatch(HealthCheckResult::healthy);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", ready ? "UP" : "DOWN");
    body.put("dependencies", results);

    return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }
}
