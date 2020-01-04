package com.decoded.cauldron.internal.routing;

import com.decoded.cauldron.api.CauldronApi;
import com.decoded.cauldron.api.annotation.BodyParam;
import com.decoded.cauldron.api.annotation.QueryParam;
import com.decoded.cauldron.api.network.http.EndpointEntry;
import com.decoded.cauldron.api.network.http.EndpointResult;
import com.decoded.cauldron.api.network.http.HttpMethod;
import com.decoded.cauldron.api.network.http.HttpResource;
import com.decoded.cauldron.api.network.http.validators.InputValidator;
import com.decoded.cauldron.api.network.http.validators.NoopInputValidator;
import com.decoded.cauldron.server.exception.CauldronHttpException;
import com.decoded.cauldron.server.exception.CauldronServerException;
import com.decoded.cauldron.server.http.InvocationContext;
import com.decoded.cauldron.server.http.Status;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal Class for routing requests from the container to Http Resources.
 */
public class RequestRouter {
  private static Logger LOG = LoggerFactory.getLogger(RequestRouter.class);

  /**
   * Direct the request to the specified resource.
   *
   * @param httpResource the {@link HttpResource}
   *
   * @return an {@link EndpointResult}
   */
  public static EndpointResult routeRequestToResource(HttpResource httpResource) {
    EndpointEntry entry = httpResource.getEndpointEntry(CauldronApi.getRequestEndpointMethod());

    if (entry != null) {
      Object[] args = buildMethodInvocationArguments(entry);
      try {
        return new EndpointResult(entry.getMethod().invoke(httpResource, args), entry.getResponseMimeType());
      } catch (IllegalAccessException | InvocationTargetException ex) {
        throw new CauldronServerException("Error invoking resource: " + httpResource.getClass() + "::" + entry.getMethod().getName(), ex);
      } catch (IllegalArgumentException ex) {
        throw new CauldronServerException("Illegal input", ex);
      }
    } else {
      LOG.error("Method " + CauldronApi.getRequestEndpointMethod() + " is not mapped to a resource method. Consider checking your annotations");
      throw new CauldronServerException("No method was found for request method: " + InvocationContext.getRequestContext().getRequestMethod());
    }
  }

  private static Object converter(Object paramType, Object parameterInputValue, Optional<ParameterizedType> maybeParameterizedType) {
    if (paramType == String.class) {
      return parameterInputValue;
    } else if (paramType == Boolean.class || paramType == boolean.class) {
      return Boolean.parseBoolean((String) parameterInputValue);
    } else if (paramType == Short.class || paramType == short.class) {
      return Short.parseShort((String) parameterInputValue);
    } else if (paramType == Long.class || paramType == long.class) {
      return Long.parseLong((String) parameterInputValue);
    } else if (paramType == Double.class || paramType == double.class) {
      return Double.parseDouble((String) parameterInputValue);
    } else if (paramType == Integer.class || paramType == int.class) {
      return Integer.parseInt((String) parameterInputValue);
    } else if (paramType == Float.class || paramType == float.class) {
      return Float.parseFloat((String) parameterInputValue);
    } else if (paramType == BigDecimal.class) {
      return BigDecimal.valueOf(Double.parseDouble((String) parameterInputValue));
    } else if (paramType == BigInteger.class) {
      return BigInteger.valueOf(Long.parseLong((String) parameterInputValue));
    } else if (paramType == List.class) {
      if (!maybeParameterizedType.isPresent()) {
        // ERROR
        throw new CauldronServerException("Expected the parameterized type to be present for List type inputs");
      }
      List<Object> results = new ArrayList<>();

      maybeParameterizedType.ifPresent(parameterizedType -> {
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        Class<?> typeClass = (Class) typeArguments[0];

        List parameterInputValueList = (List) parameterInputValue;

        parameterInputValueList.forEach(inputValue -> {
          Optional<ParameterizedType> maybeListType;
          if (inputValue.getClass() == List.class) {
            ParameterizedType listType = (ParameterizedType) inputValue.getClass().getGenericSuperclass();
            maybeListType = Optional.of(listType);
          } else {
            maybeListType = Optional.empty();
          }

          results.add(converter(typeClass, inputValue, maybeListType));
        });
      });

      return results;
    } else {
      throw new CauldronServerException("Parameter input value type: " + paramType + " was not supported");
    }
  }

  private static Object buildParameterInput(Parameter parameter, Object parameterInputValue) {
    Class<?> paramType = parameter.getType();

    if (paramType != String.class) {
      if (parameter.getParameterizedType() instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
        return converter(paramType, parameterInputValue, Optional.ofNullable(parameterizedType));
      } else {
        return converter(paramType, parameterInputValue, Optional.empty());
      }
    }

    // return the value passed in if nothing matches
    return parameterInputValue;
  }

  private static Object[] buildMethodInvocationArguments(EndpointEntry endpointEntry) {
    final Parameter[] parameters = endpointEntry.getMethod().getParameters();
    final Object[] args = new Object[parameters.length];

    int idx = 0;
    for (Parameter parameter : parameters) {
      buildInputValueForEndpointEntryParameter(endpointEntry, parameter, args, idx++);
    }

    return args;
  }

  private static void validateInput(Object input, Object[] args, Class<? extends InputValidator> validatorClass, int idx) {
    try {
      // try to create a vanilla instance of the class (It must have a public default constructor).
      InputValidator inputValidator = validatorClass.newInstance();

      try {
        if (inputValidator.validate(input)) {
          args[idx] = input;
        } else {
          throw new CauldronServerException("Invalid input " + input + " supplied");
        }
      } catch (ClassCastException ex) {
        LOG.error("The validator type is not correct for your parameter type: ", ex);
        throw new CauldronServerException("Invalid validator for input type: ", ex);
      }
    } catch (InstantiationException | IllegalAccessException ex) {
      LOG.error("Could not create validator " + validatorClass.getName());
    }
  }

  private static void buildInputValueFromBodyParameter(EndpointEntry endpointEntry, Parameter parameter, Object[] args, int idx) {
    BodyParam bodyParamAnnotation = parameter.getAnnotation(BodyParam.class);
    final String expectedName = bodyParamAnnotation.name();
    final Object bodyParamValueForRequest = parameter.getType() == List.class ? InvocationContext.getRequestContext()
        .getBodyParameters(expectedName) : InvocationContext.getRequestContext().getBodyParameter(expectedName);

    if (bodyParamValueForRequest != null) {
      Object input = buildParameterInput(parameter, bodyParamValueForRequest);
      if (bodyParamAnnotation.validator() != NoopInputValidator.class) {
        validateInput(input, args, bodyParamAnnotation.validator(), idx);
      } else {
        args[idx] = input;
      }
    } else {
      if (!bodyParamAnnotation.optional()) {
        throw new CauldronHttpException(Status.BAD_REQUEST_400,
            "Multipart Parameter " + expectedName + " was not provided, and is not Optional according to method definition "
                + endpointEntry.getMethod().getName() + " in " + endpointEntry.getMethod().getDeclaringClass().getCanonicalName());
      }

      args[idx] = null;
    }
  }

  private static void buildInputValueFromQueryParameter(EndpointEntry endpointEntry, Parameter parameter, Object[] args, int idx) {
    QueryParam queryParamAnnotation = parameter.getAnnotation(QueryParam.class);
    final String expectedIncomingQueryParamName = queryParamAnnotation.name();
    final Object queryParamValueFromRequest = parameter.getType() == List.class
        ? InvocationContext.getRequestContext()
        .getQueryParameters(expectedIncomingQueryParamName)
        : InvocationContext.getRequestContext().getQueryParameter(expectedIncomingQueryParamName);

    if (queryParamValueFromRequest != null) {
      Object input = buildParameterInput(parameter, queryParamValueFromRequest);
      if (queryParamAnnotation.validator() != NoopInputValidator.class) {
        validateInput(input, args, queryParamAnnotation.validator(), idx);
      } else {
        args[idx] = input;
      }
    } else {
      if (!queryParamAnnotation.optional()) {
        throw new CauldronHttpException(Status.BAD_REQUEST_400,
            "Parameter " + expectedIncomingQueryParamName + " was not provided, and is not Optional according to method definition "
                + endpointEntry.getMethod().getName() + " in " + endpointEntry.getMethod().getDeclaringClass().getCanonicalName());
      }
      args[idx] = null;
    }
  }

  private static void buildInputValueForEndpointEntryParameter(EndpointEntry endpointEntry, Parameter parameter, Object[] args, int idx) {
    // body parameters can be pulled from requests with a request body entity (e.g. post, put, patch requests).
    if (parameter.isAnnotationPresent(BodyParam.class)) {
      HttpMethod requestMethod = InvocationContext.getRequestContext().getRequestMethod();
      if (requestMethod == HttpMethod.POST || requestMethod == HttpMethod.PUT || requestMethod == HttpMethod.PATCH) {
        buildInputValueFromBodyParameter(endpointEntry, parameter, args, idx);
      } else {
        // this is incorrect
        throw new CauldronHttpException(Status.INTERNAL_SERVER_ERROR_500,
            "You cannot expect Body Parameters from a request without a body entity or encoded body parameters.");
      }
    } else if (parameter.isAnnotationPresent(QueryParam.class)) {
      // query parameters can be pulled from any request type.
      buildInputValueFromQueryParameter(endpointEntry, parameter, args, idx);
    } else {
      args[idx] = null;
    }
  }
}
