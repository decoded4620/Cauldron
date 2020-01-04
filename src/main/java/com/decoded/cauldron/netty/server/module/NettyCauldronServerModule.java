package com.decoded.cauldron.netty.server.module;

import com.decoded.cauldron.api.annotation.ServerModule;
import com.decoded.cauldron.server.http.InvocationContext;
import com.decoded.cauldron.server.module.CauldronModule;


@ServerModule
public class NettyCauldronServerModule extends CauldronModule {

  @Override
  protected void configure() {
    super.configure();
    bind(InvocationContext.class).asEagerSingleton();
  }
}
