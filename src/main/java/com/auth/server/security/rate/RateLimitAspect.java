package com.auth.server.security.rate;

import com.auth.server.config.RateLimitProperties;
import com.auth.server.security.rate.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

  private final RateLimitService rateLimitService;
  private final RateLimitKeyResolver keyResolver;
  private final RateLimitProperties rateLimitProperties;

  @Around("@annotation(rateLimit)")
  public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
    String key = keyResolver.resolveIp();

    int capacity;
    int refillRate;
    int refillPeriodSeconds;

    String endpointName = rateLimit.value();
    if (!"default".equals(endpointName)) {
      RateLimitProperties.Endpoints.EndpointConfig endpointConfig =
          getEndpointConfig(endpointName);
      if (endpointConfig != null) {
        capacity = endpointConfig.getCapacity();
        refillRate = endpointConfig.getRefillRate();
        refillPeriodSeconds = endpointConfig.getRefillPeriodSeconds();
      } else {
        // Fallback to annotation values or default
        capacity =
            rateLimit.capacity() != -1
                ? rateLimit.capacity()
                : rateLimitProperties.getDefaultConfig().getCapacity();
        refillRate =
            rateLimit.refillRate() != -1
                ? rateLimit.refillRate()
                : rateLimitProperties.getDefaultConfig().getRefillRate();
        refillPeriodSeconds =
            rateLimit.refillPeriodSeconds() != -1
                ? rateLimit.refillPeriodSeconds()
                : rateLimitProperties.getDefaultConfig().getRefillPeriodSeconds();
      }
    } else {
      // Use annotation values or default
      capacity =
          rateLimit.capacity() != -1
              ? rateLimit.capacity()
              : rateLimitProperties.getDefaultConfig().getCapacity();
      refillRate =
          rateLimit.refillRate() != -1
              ? rateLimit.refillRate()
              : rateLimitProperties.getDefaultConfig().getRefillRate();
      refillPeriodSeconds =
          rateLimit.refillPeriodSeconds() != -1
              ? rateLimit.refillPeriodSeconds()
              : rateLimitProperties.getDefaultConfig().getRefillPeriodSeconds();
    }

    boolean allowed = rateLimitService.tryConsume(key, capacity, refillRate, refillPeriodSeconds);

    if (!allowed) {
      throw new com.auth.server.exception.RateLimitExceededException(
          "Rate limit exceeded for IP: " + key);
    }

    return joinPoint.proceed();
  }

  private RateLimitProperties.Endpoints.EndpointConfig getEndpointConfig(String endpointName) {
    return switch (endpointName) {
      case "login" -> rateLimitProperties.getEndpoints().getLogin();
      case "refresh" -> rateLimitProperties.getEndpoints().getRefresh();
      case "oauth2-token" -> rateLimitProperties.getEndpoints().getOauth2Token();
      case "oauth2-authorize" -> rateLimitProperties.getEndpoints().getOauth2Authorize();
      default -> null;
    };
  }
}

