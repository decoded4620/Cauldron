package com.decoded.cauldron.server.module;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cauldron Injection Modules.
 */
public class CauldronModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(CauldronModule.class);

  @Override
  protected void configure() {
    LOG.info("CauldronModule::configure()");
  }
}
