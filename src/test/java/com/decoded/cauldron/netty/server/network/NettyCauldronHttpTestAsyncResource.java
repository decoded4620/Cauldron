package com.decoded.cauldron.netty.server.network;

import com.decoded.cauldron.api.annotation.HttpEndpoint;
import com.decoded.cauldron.api.annotation.NetResource;
import com.decoded.cauldron.api.annotation.QueryParam;
import com.decoded.cauldron.api.network.http.CauldronHttpMethod;
import com.decoded.cauldron.api.network.http.MimeType;
import com.decoded.cauldron.api.network.http.validators.TestStringInputValidator;
import com.decoded.cauldron.models.Candy;
import com.decoded.cauldron.netty.network.NettyHttpNetworkResource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for Netty Async Resource Implementation.
 */
@NetResource(route = "/testNettyAsync")
public class NettyCauldronHttpTestAsyncResource extends NettyHttpNetworkResource {
  // thread local resources for requests
  private static final Logger LOG = LoggerFactory.getLogger(NettyCauldronHttpTestAsyncResource.class);
  private ExecutorService executorService = Executors.newFixedThreadPool(10);

  public NettyCauldronHttpTestAsyncResource() {
  }

  /**
   * Returns a Candy.
   *
   * @param id the id of the candy
   *
   * @return a {@link Candy}
   */
  @HttpEndpoint(method = CauldronHttpMethod.GET, responseMimeType = MimeType.APPLICATION_JSON)
  public CompletableFuture<Candy> get(@QueryParam(name = "id", validator = TestStringInputValidator.class) final String id) {
    CompletableFuture<Candy> future = new CompletableFuture<>();
    executorService.submit(() -> {
      Candy t = new Candy();
      t.ingredients = new String[] {"x", "y"};
      t.name = "WTF";
      t.id = "xx";

      future.complete(t);
    });

    return future;
  }
}
