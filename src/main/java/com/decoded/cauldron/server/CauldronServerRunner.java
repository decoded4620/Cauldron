package com.decoded.cauldron.server;

import com.decoded.cauldron.netty.NettyCauldronServer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Main Server Runner.
 */
public class CauldronServerRunner {
  private static final Logger LOG = LoggerFactory.getLogger(CauldronServerRunner.class);

  /**
   * Main Function InterruptedException.
   *
   * @param args the input args
   */
  public static void main(String[] args) {
    LOG.info("Starting Main Server Class");

    final CauldronServer cauldronServer = new NettyCauldronServer();
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(cauldronServer));
    cauldronServer.start();

    while (!cauldronServer.isShuttingDown()) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException ex) {
        break;
      }
    }
  }

  private static class ShutdownHook extends Thread {
    private final CauldronServer cauldronServer;

    public ShutdownHook(CauldronServer cauldronServer) {
      this.cauldronServer = cauldronServer;
    }

    @Override
    public void run() {
      try {
        LOG.warn("stopping cauldron server because the JVM is shutting down!");
        cauldronServer.stop().get(1000, TimeUnit.MILLISECONDS);
        LOG.info("Cauldron server stopped!");
      } catch (InterruptedException | ExecutionException | TimeoutException ex) {
        LOG.error("Could not properly stop the server in time");
      }
    }
  }
}
