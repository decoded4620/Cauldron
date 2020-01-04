package com.decoded.cauldron.server.http;


public class InvocationContext {
  static final InheritableThreadLocal<CauldronHttpRequestContext> requestContextThreadLocal = new InheritableThreadLocal<>();

  public static <X extends CauldronHttpRequestContext> X setRequestContext(X context) {
    requestContextThreadLocal.set(context);
    return context;
  }

  public static CauldronHttpRequestContext getRequestContext() {
    return requestContextThreadLocal.get();
  }

  public static void clearRequestContext() {
    requestContextThreadLocal.remove();
  }
}
