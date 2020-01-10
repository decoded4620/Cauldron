package com.decoded.cauldron.netty;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;

/**
 * Netty Helper API.
 */
public class NettyHelpers {

  public static final String H2_HANDLER = "h2";
  public static final String H2_UPGRADE = "h2upgrade";
  public static final String H1_CODEC = "codec";
  public static final String AGGREGATOR = "aggregator";
  public static final String HTTP_1TO2_HANDLER_NAME = "h1.1/h2";
  public static final String HTTP2_HANDLER_NAME = "h2c";

  /**
   * Builds a Netty HttpToHttp2 Adapter and Handler.
   *
   * @param maxContentLength the maximum content length for netty message content.
   *
   * @return an {@link Http2ConnectionHandler}
   */
  public static Http2ConnectionHandler getNewHttp2ConnectionHandler(int maxContentLength) {
    DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
    InboundHttp2ToHttpAdapter listener = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false)
        .validateHttpHeaders(false)
        .maxContentLength(maxContentLength)
        .build();

    return new HttpToHttp2ConnectionHandlerBuilder().frameListener(listener)
        .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
        .connection(connection)
        .build();
  }

  /**
   * Add an idle state handler to a channel pipeline.
   *
   * @param pipeline    the Netty {@link ChannelPipeline}
   * @param idleTimeOut the timeout in milliseconds
   */
  public static void addIdleStateHandler(final ChannelPipeline pipeline, int idleTimeOut) {
    if (idleTimeOut > 0) {
      pipeline.addLast("timeout", new IdleStateHandler(0, 0, idleTimeOut, TimeUnit.MILLISECONDS));
    }
  }

  /**
   * Configures an Http2 {@link ChannelPipeline}.
   *
   * @param p                the channel pipeline.
   * @param maxContentLength the maximum content length (bytes).
   * @param idleTimeOut      the idle timeout in milliseconds.
   */
  public static void configurePipelineForHttp2(final ChannelPipeline p, int maxContentLength, int idleTimeOut) {
    p.addLast(H2_HANDLER, getNewHttp2ConnectionHandler(maxContentLength));
    NettyHelpers.addIdleStateHandler(p, idleTimeOut);
  }

  /**
   * Configure the pipeline for http1.
   *
   * @param p                the {@link ChannelPipeline}.
   * @param maxContentLength maximum content length.
   * @param idleTimeOut      the idle timeout in milliseconds.
   */
  public static void configurePipelineForHttp1(final ChannelPipeline p, int maxContentLength, int idleTimeOut) {
    p.addLast(H1_CODEC, new HttpServerCodec());
    NettyHelpers.addIdleStateHandler(p, idleTimeOut);
    aggregator(p, maxContentLength);
  }

  /**
   * Adds an aggregator to the pipeline for http1.
   *
   * @param p                the pipeline
   * @param maxContentLength the maximum content length.
   */
  public static void aggregator(final ChannelPipeline p, int maxContentLength) {
    p.addLast(AGGREGATOR, new HttpObjectAggregator(maxContentLength));
  }
}
