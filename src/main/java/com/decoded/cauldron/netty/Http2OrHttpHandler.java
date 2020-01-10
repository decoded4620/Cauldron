package com.decoded.cauldron.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

/**
 * Netty Protocol Negotiation Handler which can handle http or http2.
 */
/*package-private*/ class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
  private final int maxContentLength;
  private final int idleTimeOut;
  private final boolean h2SupportEnabled;

  /**
   * Constructor.
   *
   * @param maxContentLength max message content length in bytes
   * @param idleTimeout      max timeout for idle connection
   * @param h2SupportEnabled true to support http2 protocol.
   */
  public Http2OrHttpHandler(int maxContentLength, int idleTimeout, boolean h2SupportEnabled) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.maxContentLength = maxContentLength;
    this.idleTimeOut = idleTimeout;
    this.h2SupportEnabled = h2SupportEnabled;
  }

  @Override
  public void configurePipeline(final ChannelHandlerContext ctx, final String protocol) {
    if (h2SupportEnabled && ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      NettyHelpers.configurePipelineForHttp2(ctx.pipeline(), this.maxContentLength, this.idleTimeOut);
    } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      NettyHelpers.configurePipelineForHttp1(ctx.pipeline(), maxContentLength, this.idleTimeOut);
    } else {
      throw new IllegalStateException("Unknown protocol: " + protocol);
    }
  }
}
