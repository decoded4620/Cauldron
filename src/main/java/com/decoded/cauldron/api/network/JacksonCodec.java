package com.decoded.cauldron.api.network;

import com.decoded.cauldron.api.network.codec.ServerCodec;
import com.decoded.javautil.json.JacksonUtil;


public class JacksonCodec implements ServerCodec<String> {

  @Override
  public String encode(final Object source) {
    return JacksonUtil.serialize(source);
  }
}
