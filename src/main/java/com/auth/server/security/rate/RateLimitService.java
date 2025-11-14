package com.auth.server.security.rate;

import com.auth.server.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

  private final LettuceBasedProxyManager<byte[]> proxyManager;
  private final RateLimitProperties rateLimitProperties;

  public boolean tryConsume(String key, int capacity, int refillRate, int refillPeriodSeconds) {
    try {
      Supplier<BucketConfiguration> configSupplier =
          () -> {
            Bandwidth bandwidth =
                Bandwidth.classic(
                    capacity, Refill.intervally(refillRate, Duration.ofSeconds(refillPeriodSeconds)));

            return BucketConfiguration.builder().addLimit(bandwidth).build();
          };

      Bucket bucket = proxyManager.builder().build(key.getBytes(), configSupplier);

      boolean consumed = bucket.tryConsume(1);
      if (!consumed) {
        log.warn("Rate limit exceeded for key: {}", key);
      }
      return consumed;
    } catch (Exception e) {
      log.error("Error checking rate limit for key: {}", key, e);
      // Fail open: allow request if Redis is unavailable
      return true;
    }
  }

  public boolean tryConsumeWithDefault(String key) {
    RateLimitProperties.Default defaultConfig = rateLimitProperties.getDefaultConfig();
    return tryConsume(
        key,
        defaultConfig.getCapacity(),
        defaultConfig.getRefillRate(),
        defaultConfig.getRefillPeriodSeconds());
  }
}
