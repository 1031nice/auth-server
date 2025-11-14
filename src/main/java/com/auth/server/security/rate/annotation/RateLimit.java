package com.auth.server.security.rate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
  String value() default "default";

  int capacity() default -1;

  int refillRate() default -1;

  int refillPeriodSeconds() default -1;
}

