package com.shoplab.order.idempotency;

import com.shoplab.order.config.OrderProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điều phối idempotency (ADR-0006 D2).
 *
 * <p><b>Vì sao {@code begin} chạy {@code REQUIRES_NEW}</b>: row {@code IN_PROGRESS} phải <b>commit
 * ngay</b> để request thứ hai (chạy song song) nhìn thấy và bị chặn — nếu ghi trong transaction của
 * việc tạo order thì row vô hình cho tới khi commit, và hai request có thể cùng tạo đơn.
 *
 * <p><b>Vì sao {@code complete} KHÔNG {@code REQUIRES_NEW}</b>: nó phải nằm trong cùng transaction
 * với việc tạo order ⇒ hoặc cả hai cùng sống, hoặc cùng chết. Nếu tách ra, crash giữa hai bước sẽ
 * để lại khoá {@code COMPLETED} trỏ tới đơn không tồn tại (client retry nhận replay một đơn ma).
 */
@Service
public class IdempotencyService {

  private static final Logger LOG = LoggerFactory.getLogger(IdempotencyService.class);

  private final IdempotencyRepository repository;
  private final OrderProperties properties;
  private final Clock clock;

  public IdempotencyService(
      IdempotencyRepository repository, OrderProperties properties, Clock clock) {
    this.repository = repository;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Giữ khoá cho request này.
   *
   * @return {@link Optional#empty()} ⇒ được xử lý tiếp; {@code Optional.of(record COMPLETED)} ⇒
   *     replay response cũ
   * @throws IdempotencyExceptions.MissingIdempotencyKey khi key rỗng
   * @throws IdempotencyExceptions.KeyReused khi cùng key nhưng payload khác (422)
   * @throws IdempotencyExceptions.InProgress khi request trước chưa xong (409)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<IdempotencyRecord> begin(String scope, String key, String requestHash) {
    if (key == null || key.isBlank()) {
      throw new IdempotencyExceptions.MissingIdempotencyKey();
    }
    if (key.length() > 64) {
      throw new IdempotencyExceptions.MissingIdempotencyKey();
    }
    Instant now = Instant.now(clock);
    if (repository.insertIfAbsent(scope, key, requestHash, now) == 1) {
      return Optional.empty();
    }

    IdempotencyRecord existing =
        repository
            .find(scope, key)
            .orElseThrow(
                () -> new IllegalStateException("Khoá vừa conflict nhưng không đọc được: " + key));
    if (!existing.requestHash().equals(requestHash)) {
      throw new IdempotencyExceptions.KeyReused(key);
    }
    if (existing.status() == IdempotencyStatus.IN_PROGRESS) {
      throw new IdempotencyExceptions.InProgress(key);
    }
    return Optional.of(existing);
  }

  /** Ghi lại response để retry sau replay đúng y nguyên (cùng transaction với việc tạo order). */
  @Transactional
  public void complete(String scope, String key, int statusCode, String body, UUID resourceId) {
    repository.complete(scope, key, statusCode, body, resourceId, Instant.now(clock));
  }

  /**
   * Dọn row cũ (TD-14). Chạy theo batch để không giữ lock lâu.
   *
   * @return số row đã xoá
   */
  @Transactional
  public int cleanup() {
    Instant now = Instant.now(clock);
    Duration retention = properties.idempotencyRetention();
    Duration stale = properties.inProgressStaleAfter();
    int deleted =
        repository.deleteOlderThan(
            now.minus(retention), now.minus(stale), properties.sweepBatchSize());
    if (deleted > 0) {
      LOG.info(
          "idempotency cleanup: đã xoá {} row (COMPLETED > {}, IN_PROGRESS > {})",
          deleted,
          retention,
          stale);
    }
    return deleted;
  }

  /** SHA-256 của payload đã canonical hoá — dùng để phát hiện "cùng key, khác body". */
  public static String hash(String canonicalPayload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("JVM thiếu SHA-256", ex);
    }
  }
}
