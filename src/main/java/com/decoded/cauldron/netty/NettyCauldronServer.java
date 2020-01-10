package com.decoded.cauldron.netty;

import com.decoded.cauldron.api.network.TcpProtocol;
import com.decoded.cauldron.netty.network.NettyHttpNetworkResource;
import com.decoded.cauldron.netty.server.module.NettyCauldronServerModule;
import com.decoded.cauldron.server.BaseCauldronServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
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
  static final boolean SSL = false; //System.getProperty("ssl") != null;

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

  /**
   * Returns the SSL Context if the system enables it.
   * @return SslContext
   */
  public SslContext getSslContext() {
    SslContext sslCtx = null;
    try {
      if (SSL) {
        SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
            .sslProvider(provider)
            /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
             * Please refer to the HTTP/2 specification for cipher requirements. */
            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1))
            .build();
      } else {
        sslCtx = null;
      }
    } catch (SSLException | CertificateException ex) {
      LOG.error("SSL Exception");
    }

    return sslCtx;
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
          .childHandler(new NettyCauldronServerInitializer(getSslContext(), httpRoutingMap, false, TcpProtocol.HTTP_2));

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
