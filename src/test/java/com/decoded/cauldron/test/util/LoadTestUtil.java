package com.decoded.cauldron.test.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for performing load tests on cauldron server.
 */
public class LoadTestUtil {
  private static final Logger LOG = LoggerFactory.getLogger(LoadTestUtil.class);

  /**
   * Load Test Wrapper.
   *
   * @param loadTest         a runnable
   * @param threadCount      the number of threads to use when testing
   * @param callCount        the call count
   * @param submitRatePerSec the rate per second. If 0, there is no rate limit.
   */
  public static void loadTest(Runnable loadTest, int threadCount, int callCount, int timeoutMs, int submitRatePerSec) {

    long pause = submitRatePerSec > 0 ? 1000 / submitRatePerSec : 0;
    final long start = System.currentTimeMillis();

    LOG.info("Executing load test with " + threadCount + " threads, and total call count " + callCount);
    ExecutorService threadyForAction = Executors.newFixedThreadPool(threadCount);

    if (pause > 0) {
      pause = Math.max(pause, 2);
    }

    for (int i = 0; i < callCount; i++) {
      threadyForAction.submit(loadTest::run);
      if (pause > 0) {
        try {
          Thread.sleep(pause);
        } catch (InterruptedException ex) {
          break;
        }
      }
    }

    LOG.info("Processed " + callCount + " thread submissions, waiting...");
    threadyForAction.shutdown();
    try {
      threadyForAction.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
      long totalTime = System.currentTimeMillis() - start;
      LOG.info("Completed load test in " + totalTime + " ms");
    } catch (InterruptedException ex) {
      fail(ex);
    }
  }
}
