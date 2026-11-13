package com.shoplab.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.gateway.ratelimit.RateLimitProperties.Limit;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Thuật toán token bucket trên <b>Redis thật</b> — nơi duy nhất kiểm được Lua.
 *
 * <p>Mock Redis ở đây là tự lừa mình: thứ dễ sai là <i>script Lua</i> (kiểu số, thứ tự ARGV, {@code
 * PEXPIRE}, "kiểm hết rồi mới trừ"), và chỉ Redis thật mới chạy đúng script đó.
 *
 * <p>Thời gian được truyền vào ({@code nowMs}) nên test <b>không</b> phải {@code sleep} để chờ
 * refill — vừa nhanh vừa không flaky.
 */
@Testcontainers
class RedisTokenBucketIT {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static StringRedisTemplate redis;
  private static RedisTokenBucket bucket;

  @BeforeAll
  static void connect() {
    LettuceConnectionFactory factory =
        new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    factory.afterPropertiesSet();
    redis = new StringRedisTemplate(factory);
    redis.afterPropertiesSet();
    bucket = new RedisTokenBucket(redis);
  }

  private static String tokensOf(String key) {
    Object value = redis.opsForHash().get(key, "tokens");
    return value == null ? null : value.toString();
  }

  @Test
  @DisplayName("burst là trần cứng: đúng `burst` request được qua, request thứ burst+1 bị chặn")
  void burstShouldBeHardLimit() {
    Limit limit = new Limit(1, 3);
    String key = "rl:test:burst";

    assertThat(bucket.consume(null, limit, key, limit, 1_000L).allowed()).isTrue();
    assertThat(bucket.consume(null, limit, key, limit, 1_000L).allowed()).isTrue();
    RateLimitDecision third = bucket.consume(null, limit, key, limit, 1_000L);
    assertThat(third.allowed()).isTrue();
    assertThat(third.remaining()).isZero();

    // 1 rps ⇒ phải chờ 1 s mới có token tiếp theo — đây là con số client thấy ở Retry-After.
    RateLimitDecision fourth = bucket.consume(null, limit, key, limit, 1_000L);
    assertThat(fourth.allowed()).isFalse();
    assertThat(fourth.scope()).isEqualTo(RateLimitDecision.Bucket.IP);
    assertThat(fourth.retryAfterMs()).isEqualTo(1_000L);
  }

  @Test
  @DisplayName("refill theo thời gian trôi: 1,5 s × 1 rps ⇒ có lại đúng 1 token (không phải 2)")
  void refillShouldFollowElapsedTime() {
    Limit limit = new Limit(1, 3);
    String key = "rl:test:refill";
    long t0 = 5_000L;

    for (int i = 0; i < 3; i++) {
      bucket.consume(null, limit, key, limit, t0);
    }
    assertThat(bucket.consume(null, limit, key, limit, t0).allowed()).isFalse();

    // +1500 ms ở 1 rps = 1,5 token ⇒ làm tròn xuống 1 ⇒ được đúng 1 request.
    assertThat(bucket.consume(null, limit, key, limit, t0 + 1_500).allowed()).isTrue();
    assertThat(bucket.consume(null, limit, key, limit, t0 + 1_500).allowed()).isFalse();

    // Rất lâu sau: nạp đầy nhưng KHÔNG vượt burst (nếu không thì burst = vô nghĩa).
    assertThat(bucket.consume(null, limit, key, limit, t0 + 600_000).remaining())
        .isEqualTo(limit.burst() - 1);
  }

  @Test
  @DisplayName("xô IP cạn ⇒ chặn, và xô USER KHÔNG bị trừ oan (kiểm hết rồi mới trừ)")
  void rejectedRequestShouldNotConsumeOtherBucket() {
    Limit userLimit = new Limit(100, 100);
    Limit ipLimit = new Limit(1, 1);
    String userKey = "rl:test:atomic:u:buyer-1";
    String ipKey = "rl:test:atomic:ip:10.0.0.5";

    assertThat(bucket.consume(userKey, userLimit, ipKey, ipLimit, 1_000L).allowed()).isTrue();
    assertThat(tokensOf(userKey)).isEqualTo("99");

    RateLimitDecision rejected = bucket.consume(userKey, userLimit, ipKey, ipLimit, 1_000L);
    assertThat(rejected.allowed()).isFalse();
    assertThat(rejected.scope())
        .as("xô cạn là IP, không phải user")
        .isEqualTo(RateLimitDecision.Bucket.IP);
    assertThat(tokensOf(userKey))
        .as("user không được mất token cho request bị chặn")
        .isEqualTo("99");
  }

  @Test
  @DisplayName(
      "xô user cạn trước ⇒ báo scope USER (client biết bị chặn theo tài khoản hay theo IP)")
  void exhaustedUserBucketShouldBeReported() {
    Limit userLimit = new Limit(1, 1);
    Limit ipLimit = new Limit(100, 100);
    String userKey = "rl:test:scope:u:buyer-2";
    String ipKey = "rl:test:scope:ip:10.0.0.6";

    assertThat(bucket.consume(userKey, userLimit, ipKey, ipLimit, 1_000L).allowed()).isTrue();
    RateLimitDecision rejected = bucket.consume(userKey, userLimit, ipKey, ipLimit, 1_000L);

    assertThat(rejected.allowed()).isFalse();
    assertThat(rejected.scope()).isEqualTo(RateLimitDecision.Bucket.USER);
    assertThat(rejected.limit()).isEqualTo(userLimit.burst());
  }

  @Test
  @DisplayName("key có TTL = thời gian nạp đầy ⇒ IP lạ không làm phình Redis vĩnh viễn")
  void bucketKeyShouldExpire() {
    Limit limit = new Limit(10, 20);
    String key = "rl:test:ttl";
    bucket.consume(null, limit, key, limit, 1_000L);

    Long ttlMillis = redis.getExpire(key, TimeUnit.MILLISECONDS);
    // 20 token / 10 rps = 2 s để xô đầy lại ⇒ TTL phải xoay quanh đó, không phải 0 (không có) hay
    // ∞.
    assertThat(ttlMillis).isNotNull().isBetween(1L, 10_000L);
  }

  @Test
  @DisplayName("hai key độc lập: chặn IP này không ảnh hưởng IP khác")
  void bucketsShouldBeIndependent() {
    Limit limit = new Limit(1, 1);

    bucket.consume(null, limit, "rl:test:indep:a", limit, 1_000L);
    assertThat(bucket.consume(null, limit, "rl:test:indep:a", limit, 1_000L).allowed()).isFalse();
    assertThat(bucket.consume(null, limit, "rl:test:indep:b", limit, 1_000L).allowed()).isTrue();
  }
}
