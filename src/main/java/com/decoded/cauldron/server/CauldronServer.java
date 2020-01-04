package com.decoded.cauldron.server;

import java.util.concurrent.CompletableFuture;


/**
 * Interface for a Cauldron Server. It can expose network resources of any type, for instance, Http Network Resources.
 */
public interface CauldronServer {
  CompletableFuture<Void> start();

  CompletableFuture<Void> stop();

  int getResourceCount();

  int getEndpointCount();

  boolean isStarted();

  boolean isStarting();

  boolean isShuttingDown();
}

