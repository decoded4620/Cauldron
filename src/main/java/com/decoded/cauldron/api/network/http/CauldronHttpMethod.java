package com.decoded.cauldron.api.network.http;

/**
 * Expands {@link HttpMethod} for endpoint definitions to be more finely directed to resource methods for Cauldron Server.
 */
public enum CauldronHttpMethod {
  ACTION,
  CREATE,
  GET,
  UPDATE,
  PARTIAL_UPDATE,
  DELETE,
  GET_ALL,
  UPDATE_ALL,
  PARTIAL_UPDATE_ALL,
  DELETE_ALL,
  BATCH_GET,
  BATCH_UPDATE,
  BATCH_PARTIAL_UPDATE,
  BATCH_DELETE,
  BATCH_CREATE
}
