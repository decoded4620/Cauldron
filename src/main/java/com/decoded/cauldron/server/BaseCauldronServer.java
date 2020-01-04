package com.decoded.cauldron.server;

import com.decoded.cauldron.api.annotation.NetResource;
import com.decoded.cauldron.api.annotation.ServerModule;
import com.decoded.cauldron.api.network.AbstractNetworkResource;
import com.decoded.cauldron.api.network.http.HttpResource;
import com.decoded.cauldron.server.exception.CauldronServerException;
import com.decoded.cauldron.server.module.CauldronModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base Cauldron Server Abstract. Cauldron is an Http Server Framework Wrapped around implementations of an Http Server Layer. This can be Netty,
 * for instance.
 */
public abstract class BaseCauldronServer implements CauldronServer {
  private static final Logger LOG = LoggerFactory.getLogger(BaseCauldronServer.class);

  private String[] packagePrefixes = {"com.decoded.cauldron"};

  private int port = 8081;

  private Injector injector;
  private Map<String, ? super AbstractNetworkResource> networkResourceMap;

  public int getPort() {
    return port;
  }

  public BaseCauldronServer setPort(final int port) {
    this.port = port;
    return this;
  }

  public String[] getPackagePrefixes() {
    return Arrays.copyOf(packagePrefixes, packagePrefixes.length);
  }

  public BaseCauldronServer setPackagePrefixes(final String[] packagePrefixes) {
    this.packagePrefixes = Arrays.copyOf(packagePrefixes, packagePrefixes.length);
    return this;
  }

  public Map<String, ? super AbstractNetworkResource> getNetworkResourceMap() {
    return ImmutableMap.copyOf(networkResourceMap);
  }

  /**
   * Returns an instance of type T, using the internally registered injector. The class must be registered in of one of the server modules. These
   * use Guice under the hood.
   *
   * @param of  the class that is registered with our server injector
   * @param <T> the type to return
   *
   * @return a T
   */
  public <T> T getInstance(Class<T> of) {
    return injector.getInstance(of);
  }

  /**
   * Initialize the server with modules of a specific type. This is usually split by server implementation e.g. NettyCauldronServerModule, etc.
   *
   * @param moduleClass the module class
   */
  protected void initializeWithModulesOfType(Class<? extends CauldronModule> moduleClass) {
    LOG.info("Initializing Modules of type: " + moduleClass.getName() + " for Cauldron Server");
    List<CauldronModule> cauldronModules = new ArrayList<>();

    Arrays.stream(getPackagePrefixes()).forEach(packagePrefix -> {
      Reflections reflections = buildReflections(packagePrefix);
      cauldronModules.addAll(getCauldronModules(reflections, moduleClass));
    });

    if (cauldronModules.size() == 0) {
      throw new CauldronServerException(
          "Expected one or more Server Modules to be registered with the " + ServerModule.class.getCanonicalName() + " annotation");
    }

    injector = Guice.createInjector(cauldronModules);

    Map<String, ? super AbstractNetworkResource> cauldronNetworkResources = new HashMap<>();
    Arrays.stream(getPackagePrefixes()).forEach(packagePrefix -> getNetworkResources(packagePrefix, injector, cauldronNetworkResources));

    if (cauldronNetworkResources.size() == 0) {
      LOG.warn("No Network Resources were exposed by the server");
    }

    networkResourceMap = Collections.unmodifiableMap(cauldronNetworkResources);
  }

  /**
   * Initializes the basic http routing map. E.g. when a request is received, the path is parsed in order to direct it to a resource This map
   * contains the mapping of the paths to resources which handle requests for those paths.
   *
   * @param httpResourceBaseClass the base class which all Http Resources under this server implementation are expected to be.
   * @param <X>                   The Base Resource Class type.
   *
   * @return a Map of paths to resources which were detected matching with their {@link NetResource} annotation settings.
   */
  protected <X extends HttpResource> Map<String, X> initializeHttpRoutingMap(Class<X> httpResourceBaseClass) {
    Map<String, X> routingMap = new HashMap<>();
    getNetworkResourceMap().forEach((path, resource) -> {
      if (httpResourceBaseClass.isAssignableFrom(resource.getClass())) {
        routingMap.put(path, httpResourceBaseClass.cast(resource));
      }
    });

    LOG.info("Built Routing Map for " + getClass().getCanonicalName() + ", resource paths created: " + routingMap.size());

    return Collections.unmodifiableMap(routingMap);
  }

  /**
   * Returns a new Reflections on the specified package and scanners.
   *
   * @return Reflections
   */
  protected Reflections buildReflections(String packagePrefix) {
    return new Reflections(packagePrefix, new TypeElementsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());
  }

  /**
   * Class inheritance guard. Descendant must inherit Base or we throw an exception.
   *
   * @param baseClass       the base class
   * @param descendantClass the descendant
   * @param <X>             the base type
   * @param <Y>             the descendant type
   */
  protected <X, Y> void throwIfNotExtends(Class<X> baseClass, Class<Y> descendantClass) {
    if (!baseClass.isAssignableFrom(descendantClass)) {
      throw new CauldronServerException("Class " + descendantClass.getCanonicalName() + " must extend " + baseClass.getCanonicalName());
    }
  }


  @SuppressWarnings("unchecked")
  protected List<CauldronModule> getCauldronModules(Reflections reflections, Class<? extends CauldronModule> filterClass) {
    return reflections.getTypesAnnotatedWith(ServerModule.class).stream().map(annotatedClass -> {
      throwIfNotExtends(CauldronModule.class, annotatedClass);
      return (Class<CauldronModule>) annotatedClass;
    }).filter(filterClass::isAssignableFrom).map(this::createInstance).collect(Collectors.toList());
  }

  /**
   * Creates an instance of the specified class. Fails with a {@link CauldronServerException} if the instance cannot be constructed.
   *
   * @param instantiableClass the class to create
   * @param <T>               the type to create
   *
   * @return the ResponseFormat instance
   */
  protected <T> T createInstance(Class<T> instantiableClass) {
    LOG.info("createInstance --> " + instantiableClass.getCanonicalName());
    try {
      return instantiableClass.newInstance();
    } catch (InstantiationException | IllegalAccessException ex) {
      LOG.error("Could not instantiate module type, it either has no default constructor, or the " + "constructor is not public.");
      throw new CauldronServerException("Error instantiating Server Module for injection", ex);
    }
  }

  @SuppressWarnings("unchecked")
  protected void getNetworkResources(String packagePrefix, Injector injector, Map<String, ? super AbstractNetworkResource> resourceMap) {
    LOG.info("Scanning for Network Resources in package: " + packagePrefix);
    Reflections reflections = buildReflections(packagePrefix);
    Set<Class<?>> networkResourceClasses = reflections.getTypesAnnotatedWith(NetResource.class);
    LOG.info("Found " + networkResourceClasses.size() + " classes");
    networkResourceClasses.forEach(netResourceClass -> {
      throwIfNotExtends(AbstractNetworkResource.class, netResourceClass);
      LOG.info("processing class " + netResourceClass.getCanonicalName());
      NetResource annotation = netResourceClass.getDeclaredAnnotation(NetResource.class);
      if (annotation == null) {
        throw new IllegalStateException("The net resource annotation is missing from " + netResourceClass.getCanonicalName());
      }

      resourceMap.put(annotation.route(), injector.getInstance((Class<AbstractNetworkResource>) netResourceClass));
    });

    LOG.info("Found " + resourceMap.size() + " possible Network Resource Candidates");
  }
}
