package com.decoded.cauldron.test.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utilities for Futures and Completable objects.
 */
public class AsyncUtils {
  /**
   * Safely wait on a future and fail if we are interrupted.
   *
   * @param future  the future
   * @param timeout the timeout value
   */
  public static void wait(CompletableFuture<?> future, long timeout) {
    try {
      future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException | ExecutionException | InterruptedException ex) {
      // TBD
      fail(ex);
    }
  }
}
