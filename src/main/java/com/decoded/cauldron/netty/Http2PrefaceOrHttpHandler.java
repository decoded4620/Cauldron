package com.decoded.cauldron.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.AsciiString;
import java.util.List;

/*package-private*/ class Http2PrefaceOrHttpHandler extends ByteToMessageDecoder {
  private static final int PRI = 0x50524920;
  private final int maxContentLength;
  private String name;

  public Http2PrefaceOrHttpHandler(int maxContentLength) {
    this.maxContentLength = maxContentLength;
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
    super.handlerAdded(ctx);
    name = ctx.name();
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
    if (in.readableBytes() < 4) {
      return;
    }

    if (in.getInt(in.readerIndex()) == PRI) {
      h2c(ctx);
    } else {
      h2cOrHttp1(ctx);
    }

    ctx.pipeline().remove(this);
  }

  private void h2cOrHttp1(final ChannelHandlerContext ctx) {
    ChannelPipeline p = ctx.pipeline();
    HttpServerCodec http1codec = new HttpServerCodec();

    ChannelHandler channelHandler = new HttpServerUpgradeHandler(http1codec, protocol -> {
      if (!AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
        return null;
      }
      return new Http2ServerUpgradeCodec(NettyHelpers.getNewHttp2ConnectionHandler(maxContentLength));
    }, maxContentLength);

    String baseName = name;
    baseName = addAfter(p, baseName, NettyHelpers.H1_CODEC, http1codec);
    addAfter(p, baseName, NettyHelpers.H2_UPGRADE, channelHandler);
  }

  private void h2c(final ChannelHandlerContext ctx) {
    final ChannelPipeline p = ctx.pipeline();
    addAfter(p, name, NettyHelpers.H2_HANDLER, NettyHelpers.getNewHttp2ConnectionHandler(maxContentLength));
  }


  private String addAfter(final ChannelPipeline p, final String baseName, final String name, final ChannelHandler h) {
    p.addAfter(baseName, name, h);
    return p.context(h).name();
  }
}