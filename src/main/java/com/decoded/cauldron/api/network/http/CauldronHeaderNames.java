package com.decoded.cauldron.api.network.http;

/**
 * Cauldron Specific Http Headers under the X-Cauldron prefix.
 */
public class CauldronHeaderNames {
  /**
   * This allows more endpoints to be represented for a single type of Http Request for instance Http.GET can map to CauldronHttpMethod values
   * GET, GET_ALL, or BATCH_GET (which means 3 variants of a get endpoint can be exposed for a single path). Valid values are those found in
   * {@link CauldronHttpMethod}
   */
  public static final String CAULDRON_HTTP_METHOD = "X-Cauldron-Http-Method";

  /**
   * This header value is processed as an integer, and will force processing of the current request to hold onto the current thread / cpu cycle
   * using Thread.sleep for the additional time (in milliseconds).
   */
  public static final String ADDITIONAL_PROCESSING_LATENCY = "X-Cauldron-Additional-Processing-Latency";

  /**
   * Forces the server to throw a custom error status, for the current request, and raises a {@link
   * com.decoded.cauldron.server.exception.CauldronHttpException} as if by user code.
   */
  public static final String CUSTOM_ERROR_STATUS = "X-Cauldron-Custom-Error-Status";
}
