package com.decoded.cauldron.netty;

import com.decoded.cauldron.api.config.HttpServerConfiguration;
import com.decoded.cauldron.api.network.TcpProtocol;
import com.decoded.cauldron.api.network.security.crypto.CryptographyService;
import com.decoded.cauldron.api.network.security.crypto.google.GoogleTinkConfiguration;
import com.decoded.cauldron.api.network.security.crypto.google.GoogleTinkCryptographyService;
import com.decoded.cauldron.netty.network.NettyHttpNetworkResource;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import java.io.File;
import java.io.IOException;
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
  private CryptographyService cryptographyService;
  private TcpProtocol tcpProtocol;

  private boolean supportH2;
  private int idleTimeOut;
  private int maxContentLength = 20000000;

  /**
   * Constructor.
   *
   * @param sslContext                 an {@link SslContext}
   * @param httpRoutingMap             a {@link Map} of routes to {@link NettyHttpNetworkResource}
   * @param regenerateCryptographyKeys regenerates the cryptography keys upon restarting
   */
  public NettyCauldronServerInitializer(SslContext sslContext,
                                        Map<String, ? super NettyHttpNetworkResource> httpRoutingMap,
                                        boolean regenerateCryptographyKeys,
                                        TcpProtocol protocol) {
    this.sslContext = sslContext;
    this.httpRoutingMap = httpRoutingMap;
    this.tcpProtocol = protocol;
    initializeCryptographyService(regenerateCryptographyKeys);
  }

  @Override
  protected void initChannel(final SocketChannel ch) {
    LOG.info("Init Netty Cauldron Server Channel Initializer: " + ch.localAddress().toString());
    ChannelPipeline pipeline = ch.pipeline();
    if (sslContext != null) {
      setupSsl(ch);
      pipeline.addLast(NettyHelpers.HTTP_1TO2_HANDLER_NAME, new Http2OrHttpHandler(maxContentLength, 20000, true));
    } else {
      if (tcpProtocol == TcpProtocol.HTTP_1_1) {
        pipeline.addLast(new HttpServerCodec());
      } else if (tcpProtocol == TcpProtocol.HTTP_2) {
        pipeline.addLast(NettyHelpers.HTTP2_HANDLER_NAME, new Http2PrefaceOrHttpHandler(maxContentLength));
      }
    }

    pipeline.addLast(new HttpContentCompressor());
    pipeline.addLast(new HttpObjectAggregator(maxContentLength));
    pipeline.addLast(new NettyCauldronHttpHandler(cryptographyService, httpRoutingMap));
  }

  private void setupSsl(SocketChannel socketChannel) {
    ChannelPipeline pipeline = socketChannel.pipeline();
    pipeline.addLast(sslContext.newHandler(socketChannel.alloc()));
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


  private void loadHttpServerConfiguration(String configurationFileUri) {

    LOG.info("Load Container Configuration: " + configurationFileUri);

    // -----------------
    // CONFIGS
    // application and object configuration capability
    // must be bound before all dependants.
    File serverConfigurationFile = new File(configurationFileUri);

    if (serverConfigurationFile.exists()) {
      LOG.info("Loaded module configuration from file: [" + serverConfigurationFile.getAbsolutePath() + "]");
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

      try {
        HttpServerConfiguration httpServerConfiguration = mapper.readValue(serverConfigurationFile, HttpServerConfiguration.class);

        LOG.info("Server configuration loaded: " + httpServerConfiguration.toString());
      } catch (JsonParseException | JsonMappingException ex) {
        LOG.error("Json Parsing Exception during configuration", ex);
        throw new IllegalArgumentException(ex);
      } catch (IOException ex) {
        LOG.error("Exception reading configuration file: " + configurationFileUri, ex);
        throw new IllegalStateException(ex);
      }
    } else {
      LOG.error("Module configuration at: " + serverConfigurationFile.getAbsolutePath() + " could not be loaded");
    }
  }
}
