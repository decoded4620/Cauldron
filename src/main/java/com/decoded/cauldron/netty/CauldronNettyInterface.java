package com.decoded.cauldron.netty;

import com.decoded.cauldron.netty.context.NettyHttpRequestContext;
import com.decoded.cauldron.server.http.InvocationContext;
import com.decoded.cauldron.server.http.Status;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the interface between Cauldron and Netty.
 */
public class CauldronNettyInterface {
  private static final Map<Status, HttpResponseStatus> cauldronStatusToNettyStatusMap;

  static {

    Map<Status, HttpResponseStatus> map = new HashMap<>();
    map.put(Status.CONTINUE_100, HttpResponseStatus.CONTINUE);
    map.put(Status.SWITCHING_PROTOCOLS_101, HttpResponseStatus.SWITCHING_PROTOCOLS);
    map.put(Status.PROCESSING_102, HttpResponseStatus.PROCESSING);
    map.put(Status.OK_200, HttpResponseStatus.OK);
    map.put(Status.CREATED_201, HttpResponseStatus.CREATED);
    map.put(Status.ACCEPTED_202, HttpResponseStatus.ACCEPTED);
    map.put(Status.NON_AUTHORITATIVE_INFORMATION_203, HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION);
    map.put(Status.NO_CONTENT_204, HttpResponseStatus.NO_CONTENT);
    map.put(Status.RESET_CONTENT_205, HttpResponseStatus.RESET_CONTENT);
    map.put(Status.PARTIAL_CONTENT_206, HttpResponseStatus.PARTIAL_CONTENT);
    map.put(Status.MULTI_STATUS_207, HttpResponseStatus.MULTI_STATUS);
    map.put(Status.MULTIPLE_CHOICES_300, HttpResponseStatus.MULTIPLE_CHOICES);
    map.put(Status.MOVED_PERMANENTLY_301, HttpResponseStatus.MOVED_PERMANENTLY);
    map.put(Status.FOUND_302, HttpResponseStatus.FOUND);
    map.put(Status.SEE_OTHER_303, HttpResponseStatus.SEE_OTHER);
    map.put(Status.NOT_MODIFIED_304, HttpResponseStatus.NOT_MODIFIED);
    map.put(Status.USE_PROXY_305, HttpResponseStatus.USE_PROXY);
    map.put(Status.TEMPORARY_REDIRECT_307, HttpResponseStatus.TEMPORARY_REDIRECT);
    map.put(Status.PERMANENT_REDIRECT_308, HttpResponseStatus.PERMANENT_REDIRECT);
    map.put(Status.BAD_REQUEST_400, HttpResponseStatus.BAD_REQUEST);
    map.put(Status.UNAUTHORIZED_401, HttpResponseStatus.UNAUTHORIZED);
    map.put(Status.PAYMENT_REQUIRED_402, HttpResponseStatus.PAYMENT_REQUIRED);
    map.put(Status.FORBIDDEN_403, HttpResponseStatus.FORBIDDEN);
    map.put(Status.NOT_FOUND_404, HttpResponseStatus.NOT_FOUND);
    map.put(Status.NOT_ALLOWED_405, HttpResponseStatus.METHOD_NOT_ALLOWED);
    map.put(Status.NOT_ACCEPTABLE_406, HttpResponseStatus.NOT_ACCEPTABLE);
    map.put(Status.PROXY_AUTHENTICATION_REQUIRED_407, HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED);
    map.put(Status.REQUEST_TIMEOUT_408, HttpResponseStatus.REQUEST_TIMEOUT);
    map.put(Status.CONFLICT_409, HttpResponseStatus.CONFLICT);
    map.put(Status.GONE_410, HttpResponseStatus.GONE);
    map.put(Status.LENGTH_REQUIRED_411, HttpResponseStatus.LENGTH_REQUIRED);
    map.put(Status.PRECONDITION_FAILED_412, HttpResponseStatus.PRECONDITION_FAILED);
    map.put(Status.REQUEST_ENTITY_TOO_LARGE_413, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
    map.put(Status.REQUEST_URI_TOO_LONG_414, HttpResponseStatus.REQUEST_URI_TOO_LONG);
    map.put(Status.UNSUPPORTED_MEDIA_TYPE_415, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
    map.put(Status.REQUESTED_RANGE_NOT_SATISFIABLE_416, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    map.put(Status.EXPECTATION_FAILED_417, HttpResponseStatus.EXPECTATION_FAILED);
    map.put(Status.MISDIRECTED_REQUEST_421, HttpResponseStatus.MISDIRECTED_REQUEST);
    map.put(Status.UNPROCESSABLE_ENTITY_422, HttpResponseStatus.UNPROCESSABLE_ENTITY);
    map.put(Status.LOCKED_423, HttpResponseStatus.LOCKED);
    map.put(Status.FAILED_DEPENDENCY_424, HttpResponseStatus.FAILED_DEPENDENCY);
    map.put(Status.UNORDERED_COLLECTION_425, HttpResponseStatus.UNORDERED_COLLECTION);
    map.put(Status.UPGRADE_REQUIRED_426, HttpResponseStatus.UPGRADE_REQUIRED);
    map.put(Status.PRECONDITION_REQUIRED_428, HttpResponseStatus.PRECONDITION_REQUIRED);
    map.put(Status.TOO_MANY_REQUESTS_429, HttpResponseStatus.TOO_MANY_REQUESTS);
    map.put(Status.REQUEST_HEADER_FIELDS_TOO_LARGE_431, HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
    map.put(Status.INTERNAL_SERVER_ERROR_500, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    map.put(Status.NOT_IMPLEMENTED_501, HttpResponseStatus.NOT_IMPLEMENTED);
    map.put(Status.BAD_GATEWAY_502, HttpResponseStatus.BAD_GATEWAY);
    map.put(Status.SERVICE_UNAVAILABLE_503, HttpResponseStatus.SERVICE_UNAVAILABLE);
    map.put(Status.GATEWAY_TIMEOUT_504, HttpResponseStatus.GATEWAY_TIMEOUT);
    map.put(Status.HTTP_VERSION_NOT_SUPPORTED_505, HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED);
    map.put(Status.VARIANT_ALSO_NEGOTIATES_506, HttpResponseStatus.VARIANT_ALSO_NEGOTIATES);
    map.put(Status.INSUFFICIENT_STORAGE_507, HttpResponseStatus.INSUFFICIENT_STORAGE);
    map.put(Status.NOT_EXTENDED_510, HttpResponseStatus.NOT_EXTENDED);
    map.put(Status.NETWORK_AUTHENTICATION_REQUIRED_511, HttpResponseStatus.NETWORK_AUTHENTICATION_REQUIRED);

    cauldronStatusToNettyStatusMap = Collections.unmodifiableMap(map);
  }

  /**
   * Returns a {@link HttpResponseStatus} that is mapped to the {@link Status} input.
   *
   * @param cauldronStatus a Cauldron Status from user code
   *
   * @return the Netty {@link HttpResponseStatus} to return to the client.
   */
  public static HttpResponseStatus getResponseStatus(Status cauldronStatus) {
    return cauldronStatusToNettyStatusMap.get(cauldronStatus);
  }

  /**
   * Returns a local socket address.
   *
   * @return the local socket address where we're hosting http services.
   */
  public static SocketAddress getLocalAddress() {
    return ((NettyHttpRequestContext) InvocationContext.getRequestContext()).getCtx().channel().localAddress();
  }

}
