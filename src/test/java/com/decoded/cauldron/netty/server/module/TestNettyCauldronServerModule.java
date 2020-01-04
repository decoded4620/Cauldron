package com.decoded.cauldron.netty.server.module;

import com.decoded.cauldron.api.annotation.ServerModule;
import com.decoded.cauldron.netty.server.network.NettyCauldronHttpTestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("unused")
@ServerModule
public class TestNettyCauldronServerModule extends NettyCauldronServerModule {
  private static final Logger LOG = LoggerFactory.getLogger(TestNettyCauldronServerModule.class);

  @Override
  protected void configure() {
    super.configure();
    LOG.info("Test Netty Cauldron Module configure");
    bind(NettyCauldronHttpTestResource.class).asEagerSingleton();
  }
}
