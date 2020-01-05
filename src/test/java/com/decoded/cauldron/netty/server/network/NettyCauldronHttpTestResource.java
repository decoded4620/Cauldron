package com.decoded.cauldron.netty.server.network;

import com.decoded.cauldron.api.annotation.BodyParam;
import com.decoded.cauldron.api.annotation.HttpEndpoint;
import com.decoded.cauldron.api.annotation.NetResource;
import com.decoded.cauldron.api.annotation.QueryParam;
import com.decoded.cauldron.api.network.http.CauldronHttpMethod;
import com.decoded.cauldron.api.network.http.MimeType;
import com.decoded.cauldron.api.network.http.validators.TestStringInputValidator;
import com.decoded.cauldron.api.network.http.validators.TestStringListInputValidator;
import com.decoded.cauldron.api.network.security.crypto.CryptographyService;
import com.decoded.cauldron.models.Candy;
import com.decoded.cauldron.netty.network.NettyHttpNetworkResource;
import com.decoded.cauldron.server.exception.CauldronServerException;
import com.decoded.cauldron.server.http.CauldronHttpRequestContext;
import com.decoded.cauldron.server.http.InvocationContext;
import com.decoded.cauldron.server.http.cookies.Cookie;
import com.decoded.cauldron.server.http.cookies.SameSite;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@NetResource(route = "/testNetty")
public class NettyCauldronHttpTestResource extends NettyHttpNetworkResource {
  // thread local resources for requests
  private static final Logger LOG = LoggerFactory.getLogger(NettyCauldronHttpTestResource.class);

  public NettyCauldronHttpTestResource() {
  }

  /**
   * Test Get.
   *
   * @param id the id
   *
   * @return a {@link Candy}
   */
  @HttpEndpoint(method = CauldronHttpMethod.GET, responseMimeType = MimeType.APPLICATION_JSON)
  public Candy get(@QueryParam(optional = true, name = "id", validator = TestStringInputValidator.class) final String id) {
    if (id == null) {
      return null;
    }
    CauldronHttpRequestContext requestContext = InvocationContext.getRequestContext();
    CryptographyService service = requestContext.getCryptographyService();
    try {
      String original = "Test";

      byte[] assocData = "x".getBytes("UTF-8");
      byte[] encryptedBytes = service.encrypt(original.getBytes("UTF-8"), assocData);
      String encrypted = new String(encryptedBytes, "UTF-8");
      byte[] decryptedBytes = service.decrypt(encryptedBytes, assocData);
      String decrypted = new String(decryptedBytes, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new CauldronServerException("Oops bad encoding", ex);
    }

    return new Candy();
  }

  /**
   * Test Delete.
   *
   * @param id the id
   */
  @HttpEndpoint(method = CauldronHttpMethod.DELETE)
  public void delete(@QueryParam(name = "id", validator = TestStringInputValidator.class) final String id) {

  }

  /**
   * Test delete all.
   */
  @HttpEndpoint(method = CauldronHttpMethod.DELETE_ALL)
  public void deleteAll() {

  }

  /**
   * Test get all.
   *
   * @return List of Candy.
   */
  @HttpEndpoint(method = CauldronHttpMethod.GET_ALL, responseMimeType = MimeType.APPLICATION_JSON)
  public List<Candy> getAll() {
    Candy a = new Candy();
    a.name = "Taffy";
    a.id = "a";
    Candy b = new Candy();
    b.name = "Nougat";
    b.id = "b";
    b.ingredients = new String[] {"x"};
    List<Candy> results = new ArrayList<>();
    results.add(a);
    results.add(b);
    return results;
  }

  /**
   * Test batch get.
   *
   * @param ids the identifiers
   *
   * @return a Map of String id to candy
   */
  @HttpEndpoint(method = CauldronHttpMethod.BATCH_GET, responseMimeType = MimeType.APPLICATION_JSON)
  public Map<String, Candy> batchGet(@QueryParam(name = "ids", validator = TestStringListInputValidator.class) final List<String> ids) {
    Map<String, Candy> results = ids.stream().collect(Collectors.toMap(x -> x, x -> {
      Candy t = new Candy();
      t.ingredients = new String[] {"hair", "sugar", "shitty ingredient" + x};
      t.name = "Shitty Candy " + x;
      t.id = "shitCandy" + x;
      return t;
    }));
    return results;
  }

  /**
   * Test Actions and body params.
   *
   * @param a a fictitious body param
   * @param b a fictitious body param
   * @param c a fictitious body param
   * @param d a fictitious body param
   * @param e a fictitious body param
   * @param f a fictitious body param
   * @param g a fictitious body param
   * @param h a fictitious body param
   */
  @HttpEndpoint(method = CauldronHttpMethod.ACTION)
  public void action(@BodyParam(name = "a") final int a,
                     @BodyParam(name = "b") final long b,
                     @BodyParam(name = "c") final float c,
                     @BodyParam(name = "d") final double d,
                     @BodyParam(name = "e") final String e,
                     @BodyParam(name = "f") final boolean f,
                     @BodyParam(name = "g") final BigInteger g,
                     @BodyParam(name = "h") final BigDecimal h) {

    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("a", String.valueOf(a), "/", "localhost", true, true, SameSite.NONE, -1, System.currentTimeMillis()));
    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("b", String.valueOf(b), "/", "localhost", true, false, SameSite.LAX, -1, System.currentTimeMillis()));
    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("c", String.valueOf(c), "/", "localhost", false, true, SameSite.STRICT, -1, System.currentTimeMillis()));
    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("d", String.valueOf(d), "/", "localhost", false, false, SameSite.NONE, -1, -1));
    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("e", String.valueOf(e), "/", "localhost", false, false, SameSite.NONE, -1, -1));
    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("f", String.valueOf(f), "/", "localhost", false, false, SameSite.NONE, -1, -1));
    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("g", String.valueOf(g), "/", "localhost", false, false, SameSite.NONE, -1, -1));
    InvocationContext.getRequestContext()
        .addClientCookie(Cookie.create("h", String.valueOf(h), "/", "localhost", false, false, SameSite.NONE, -1, -1));

  }
}
