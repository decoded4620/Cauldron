package com.decoded.cauldron.api.network.http;

/**
 * Methods supported for http requests.
 */
public enum HttpMethod {
  /**
   * GET Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  GET,
  /**
   * HEAD Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  HEAD,
  /**
   * POST Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  POST,
  /**
   * PUT Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  PUT,
  /**
   * DELETE Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  DELETE,
  /**
   * CONNECT Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  CONNECT,
  /**
   * OPTIONS Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  OPTIONS,
  /**
   * TRACE Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc7231#section-4>RFC 7231, Section 4</a>
   */
  TRACE,
  /**
   * PATCH Request support.
   *
   * @see <a href=https://tools.ietf.org/html/rfc5789#section-2>RFC 5789, Section 2</a>
   */
  PATCH
}
