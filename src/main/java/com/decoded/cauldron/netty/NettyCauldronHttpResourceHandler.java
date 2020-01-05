package com.decoded.cauldron.netty;

import com.decoded.cauldron.api.CauldronApi;
import com.decoded.cauldron.api.network.JacksonCodec;
import com.decoded.cauldron.api.network.codec.ServerCodec;
import com.decoded.cauldron.api.network.http.CauldronHeaderNames;
import com.decoded.cauldron.api.network.http.EndpointResult;
import com.decoded.cauldron.api.network.http.HeaderNames;
import com.decoded.cauldron.api.network.http.HttpMethod;
import com.decoded.cauldron.api.network.http.HttpResource;
import com.decoded.cauldron.api.network.http.MimeType;
import com.decoded.cauldron.api.network.security.crypto.CryptographyService;
import com.decoded.cauldron.api.network.security.crypto.google.GoogleTinkConfiguration;
import com.decoded.cauldron.api.network.security.crypto.google.GoogleTinkCryptographyService;
import com.decoded.cauldron.internal.routing.RequestRouter;
import com.decoded.cauldron.netty.context.NettyHttpRequestContext;
import com.decoded.cauldron.netty.network.NettyHttpNetworkResource;
import com.decoded.cauldron.server.exception.CauldronHttpException;
import com.decoded.cauldron.server.exception.CauldronServerException;
import com.decoded.cauldron.server.http.InvocationContext;
import com.decoded.cauldron.server.http.Status;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Resource Handler for a Netty based Implementation.
 */
public class NettyCauldronHttpResourceHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(NettyCauldronHttpResourceHandler.class);

  private static Map<MimeType, ServerCodec<String>> codecMap = ImmutableMap.of(MimeType.APPLICATION_JSON, new JacksonCodec(),
      MimeType.TEXT_PLAIN, source -> source.toString());
  private Map<String, ? super HttpResource> router;
  private CryptographyService cryptographyService;

  /**
   * Constructor.
   *
   * @param router                     the map of resources.
   * @param regenerateCryptographyKeys regenerates the cryptography keys upon restarting
   */
  public NettyCauldronHttpResourceHandler(Map<String, ? super NettyHttpNetworkResource> router,
                                          boolean regenerateCryptographyKeys) {
    initializeRouter(router);

    initializeCryptographyService(regenerateCryptographyKeys);
  }

  private void initializeRouter(Map<String, ? super NettyHttpNetworkResource> routingMap) {
    this.router = Collections.unmodifiableMap(routingMap);
  }

  private void initializeCryptographyService(boolean regenerateKeys) {
    // TODO - move this out of the codebase and use a fabric based key
    final String masterKeyUri = "aws-kms://" + System.getenv("DEV_MASTER_KEY_ARN");
    final String cryptographicKeySetFile = "cauldron_key_set.json";
    final String keysRelativeLocation = "keys";

    GoogleTinkConfiguration cryptoConfig = new GoogleTinkConfiguration(masterKeyUri, cryptographicKeySetFile, keysRelativeLocation);
    this.cryptographyService = new GoogleTinkCryptographyService(cryptoConfig);

    cryptographyService.initialize();
    // https://us-east-2.console.aws.amazon.com/kms/home?region=us-east-2#/kms/keys
    cryptographyService.generateEncryptionKeys(regenerateKeys);
    cryptographyService.loadEncryptionKeys();
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) {
    ctx.flush();
  }

  private NettyHttpRequestContext getNewHttpRequestContext(HttpRequest httpRequest, ChannelHandlerContext ctx) {
    NettyHttpRequestContext context = InvocationContext.setRequestContext(
        new NettyHttpRequestContext(ctx).setCryptographyService(cryptographyService).setRequest(httpRequest));
    handle100ContinueExpectation(httpRequest, ctx);
    return context;
  }

  private void handle100ContinueExpectation(HttpRequest httpRequest, ChannelHandlerContext ctx) {
    if (HttpUtil.is100ContinueExpected(httpRequest)) {
      // see https://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html#sec8.2.3
      ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest httpRequest = (HttpRequest) msg;

      final NettyHttpRequestContext requestContext = getNewHttpRequestContext(httpRequest, ctx);

      FullHttpResponse response;
      HttpResource resource = (HttpResource) router.get(requestContext.getRequestPath());

      if (resource == null) {
        response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_IMPLEMENTED);
      } else {

        try {
          // blocks on resource computations and downstream operations
          EndpointResult endpointResult = RequestRouter.routeRequestToResource(resource);

          checkThrowCustomStatus();

          // builds a response given the result
          response = buildResponseWithVendorResults(endpointResult.getResult(), endpointResult.getPreferredMimeType());
          requestContext.setResponse(response);
          requestContext.addResponseHeader(HeaderNames.ALLOW, String.join(", ", CauldronApi.loadAllowedMethodsForResource(resource)));
          requestContext.addResponseHeader(HeaderNames.DATE, Calendar.getInstance().getTime().toString());
          requestContext.addResponseHeader(HeaderNames.SERVER, "Cauldron Http Server - V 1.0");
          requestContext.addResponseHeader(HeaderNames.HOST, CauldronNettyInterface.getLocalAddress().toString());
        } catch (Exception ex) {
          ex.printStackTrace();
          // builds an error response if an exception is thrown
          response = handleExecutionException(ex);
          requestContext.setResponse(response);
        }
      }

      processConnection(httpRequest, response, ctx);
      InvocationContext.clearRequestContext();
    }
  }

  private FullHttpResponse handleExecutionException(Throwable ex) {
    LOG.error("Execution Exception: " + ex.getClass() + " --> " + ex.getMessage());
    if (ex instanceof CauldronHttpException) {
      LOG.error("Cauldron Http Exception caught from user code", ex);
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
          CauldronNettyInterface.getResponseStatus(((CauldronHttpException) ex).getResponseStatus()));
    } else {
      // wtf
      LOG.error("Internal exception: ", ex);
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private void processConnection(HttpRequest httpRequest, FullHttpResponse response, ChannelHandlerContext ctx) {
    boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);

    if (response.content().readableBytes() == 0) {
      if (!response.headers().contains(HeaderNames.CONTENT_LENGTH)) {
        response.headers().add(HeaderNames.CONTENT_LENGTH, "0");
      }
    }

    checkAdditionalLatency();
    if (!keepAlive) {
      ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    } else {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      ctx.write(response);
    }
  }

  private void checkThrowCustomStatus() {
    Set<String> throwHeaders = InvocationContext.getRequestContext().getRequestHeaders(CauldronHeaderNames.CUSTOM_ERROR_STATUS);

    if (!throwHeaders.isEmpty()) {
      final String statusValue = throwHeaders.iterator().next();
      throw new CauldronHttpException(Status.valueOf(statusValue), "Custom Status Error being thrown (Dev Only)");
    }
  }

  private void checkAdditionalLatency() {
    Set<String> results = InvocationContext.getRequestContext().getRequestHeaders(CauldronHeaderNames.ADDITIONAL_PROCESSING_LATENCY);
    if (!results.isEmpty()) {
      try {
        final int latency = Integer.parseInt(results.iterator().next());
        Thread.sleep(latency);
      } catch (InterruptedException ex) {
        // nada
        LOG.error("Interrupted artificial latency", ex);
      }
    }
  }

  private FullHttpResponse buildResponseWithVendorResults(Object result, MimeType mimeType) {
    Object actualResult = result;
    if (result instanceof CompletionStage) {
      try {
        actualResult = ((CompletionStage) result).toCompletableFuture().get();
      } catch (InterruptedException | ExecutionException ex) {
        LOG.error("Error or timeout from resource future");
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
            Unpooled.wrappedBuffer(ex.getMessage().getBytes(Charsets.UTF_8)));
      }
    }

    if (actualResult == null) {
      HttpResponseStatus status = HttpResponseStatus.OK;
      // if this method was intended to return an entity
      if (InvocationContext.getRequestContext().getRequestMethod().equals(HttpMethod.GET)) {
        // default to 404?
        LOG.warn("defaulting to 404 for not found content");
        status = HttpResponseStatus.NOT_FOUND;
      }

      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
    }

    ServerCodec<String> codec = codecMap.get(mimeType);

    if (codec == null) {
      throw new CauldronServerException("Could not find a codec registered to MimeType: " + mimeType.toString());
    }

    String content = codec.encode(actualResult);

    return buildResponse(mimeType, content);
  }

  private FullHttpResponse buildResponse(MimeType mimeType, String content) {
    byte[] contents = content.getBytes(Charsets.UTF_8);

    ByteBuf buffer = Unpooled.wrappedBuffer(contents);
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
    ((NettyHttpRequestContext) InvocationContext.getRequestContext()).setResponse(response);
    InvocationContext.getRequestContext()
        .addResponseHeader(HeaderNames.CONTENT_MD5, InvocationContext.getRequestContext().getResponseContentMD5());
    InvocationContext.getRequestContext().addResponseHeader(HeaderNames.CONTENT_TYPE, mimeType.toString());
    InvocationContext.getRequestContext().addResponseHeader(HeaderNames.CONTENT_LENGTH, String.valueOf(contents.length));
    return response;
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    LOG.error("exceptionCaught: " + ctx.channel().localAddress().toString() + "<-" + ctx.channel().remoteAddress().toString(), cause);
    ctx.close();
  }
}
