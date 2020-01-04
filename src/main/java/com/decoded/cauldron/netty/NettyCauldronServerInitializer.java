package com.decoded.cauldron.netty;

import com.decoded.cauldron.netty.network.NettyHttpNetworkResource;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty Server Initializer.
 */
public class NettyCauldronServerInitializer extends ChannelInitializer<SocketChannel> {
  private static final Logger LOG = LoggerFactory.getLogger(NettyCauldronServerInitializer.class);
  private SslContext sslContext;
  private Map<String, ? super NettyHttpNetworkResource> httpRoutingMap;

  /**
   * Constructor.
   *
   * @param sslContext     an {@link SslContext}
   * @param httpRoutingMap a {@link Map} of routes to {@link NettyHttpNetworkResource}
   */
  public NettyCauldronServerInitializer(SslContext sslContext, Map<String, ? super NettyHttpNetworkResource> httpRoutingMap) {
    // TODO SSL
    this.sslContext = sslContext;
    this.httpRoutingMap = httpRoutingMap;
  }

  @Override
  protected void initChannel(final SocketChannel ch) {
    LOG.info("Init Netty Cauldron Server Channel Initializer: " + ch.localAddress().toString());
    ChannelPipeline pipeline = ch.pipeline();
    if (sslContext != null) {
      pipeline.addLast(sslContext.newHandler(ch.alloc()));
    }

    pipeline.addLast(new HttpContentCompressor());
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(20000000));
    pipeline.addLast(new NettyCauldronHttpResourceHandler(httpRoutingMap));
  }
}
