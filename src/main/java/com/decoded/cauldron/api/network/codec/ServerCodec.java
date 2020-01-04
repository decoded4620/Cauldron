package com.decoded.cauldron.api.network.codec;

/**
 * A Server Codec interface.
 */
public interface ServerCodec<X> {
  X encode(Object source);
}
