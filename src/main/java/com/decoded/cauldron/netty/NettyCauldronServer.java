package com.decoded.cauldron.netty;

import com.decoded.cauldron.netty.network.NettyHttpNetworkResource;
import com.decoded.cauldron.netty.server.module.NettyCauldronServerModule;
import com.decoded.cauldron.server.BaseCauldronServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Netty Cauldron Server.
 */
public class NettyCauldronServer extends BaseCauldronServer {
  private static final int DEFAULT_SHUTDOWN_WAIT = 20000;
  private static Logger LOG = LoggerFactory.getLogger(NettyCauldronServer.class);
  private Map<String, ? super NettyHttpNetworkResource> httpRoutingMap;
  private volatile boolean isStarted;
  private volatile boolean isStarting;
  private volatile boolean isShuttingDown;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  /**
   * Constructor.
   */
  public NettyCauldronServer() {
    initializeWithModulesOfType(NettyCauldronServerModule.class);
    this.httpRoutingMap = initializeHttpRoutingMap(NettyHttpNetworkResource.class);
    LOG.info("Initializing Netty Cauldron Server");
  }

  @Override
  public boolean isStarted() {
    return isStarted && !isShuttingDown;
  }

  @Override
  public boolean isShuttingDown() {
    return isShuttingDown;
  }

  @Override
  public boolean isStarting() {
    return isStarting;
  }

  public SslContext getSslContext() {
    // TODO - enable ssl
    return null;
  }

  @Override
  public CompletableFuture<Void> start() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    if (isStarted || isStarting) {
      LOG.warn("Netty Cauldron Server " + (isStarting ? " is already starting" : " has already started"));
      future.complete(null);
      return future;
    }

    isStarting = true;
    LOG.info("Start Netty Cauldron Server requested...");
    CompletableFuture.runAsync(() -> doNettySpecificStart(future));
    return future;
  }

  @Override
  public CompletableFuture<Void> stop() {
    CompletableFuture<Void> stopFuture = new CompletableFuture<>();
    if (isStarted && !isShuttingDown) {
      isShuttingDown = true;
      LOG.info("Stop Netty Cauldron Server requested...");

      CountDownLatch latch = new CountDownLatch(2);

      workerGroup.shutdownGracefully().addListener(f -> {
        LOG.info("Netty Worker group shutdown complete...");
        latch.countDown();
      });
      bossGroup.shutdownGracefully().addListener(f -> {
        LOG.info("Netty Boss group shutdown complete...");
        latch.countDown();
      });

      try {
        if (!latch.await(DEFAULT_SHUTDOWN_WAIT, TimeUnit.MILLISECONDS)) {
          LOG.error("Could not wait for shutdown!");
        }
        isShuttingDown = false;
        isStarted = false;
        LOG.info("Netty Cauldron Server shutdown complete");
        stopFuture.complete(null);
      } catch (InterruptedException ex) {
        LOG.error("Did not fully shutdown");
      }
    } else {
      LOG.warn("Netty Cauldron Server " + (isShuttingDown ? "is already shutting" : "has already shut") + " down");
      stopFuture.complete(null);
    }

    return stopFuture;
  }

  @Override
  public int getEndpointCount() {
    return 0;
  }

  @Override
  public int getResourceCount() {
    return 0;
  }

  private void doNettySpecificStart(CompletableFuture<Void> future) {
    LOG.info("Netty Cauldron Server Thread Starting");
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();

    try {
      // todo figure out options
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.option(ChannelOption.SO_BACKLOG, 1024);

      // TODO - from config
      bootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.TRACE))
          .childHandler(new NettyCauldronServerInitializer(getSslContext(), httpRoutingMap));

      isStarting = false;
      isStarted = true;
      Channel channel = bootstrap.bind(getPort()).sync().channel();
      LOG.info("Server running, waiting for messages on channel " + channel.localAddress().toString());
      future.complete(null);
      channel.closeFuture().sync();
    } catch (InterruptedException ex) {
      LOG.error("Error ", ex);
    } finally {
      stop();
      LOG.info("Server loop exiting...");
    }
  }
}
