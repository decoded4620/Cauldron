package com.decoded.cauldron.api.config;

import static com.decoded.cauldron.api.config.Glimmer.getFieldAnnotation;
import static com.decoded.cauldron.api.config.Glimmer.newInstance;
import static com.decoded.cauldron.api.config.Glimmer.setFieldValue;

import com.decoded.cauldron.server.exception.CauldronServerException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigMemorySize;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CauldronTypeSafeConfig<T> {
  private static final Logger LOG = LoggerFactory.getLogger(CauldronTypeSafeConfig.class);
  private Class<T> typeClass;

  public CauldronTypeSafeConfig(Class<T> typeClass) {
    this.typeClass = typeClass;
  }

  @SuppressWarnings("unchecked")
  private boolean setNonPrimitiveValue(Class outputType, Field field, T inst, Config config, String path) {
    LOG.debug("setNonPrimitiveValue() -> Complex output type: " + outputType.getCanonicalName());

    if (outputType == String.class) {
      return setFieldValue(field, inst, config.getString(path));
    } else if (outputType == Duration.class) {
      return setFieldValue(field, inst, config.getDuration(path));
    } else if (outputType == ConfigMemorySize.class) {
      return setFieldValue(field, inst, config.getMemorySize(path));
    } else if (outputType == Object.class) {
      return setFieldValue(field, inst, config.getAnyRef(path));
    } else if (outputType == Config.class) {
      return setFieldValue(field, inst, config.getConfig(path));
    } else if (outputType == ConfigObject.class) {
      return setFieldValue(field, inst, config.getObject(path));
    } else if (outputType == ConfigValue.class) {
      return setFieldValue(field, inst, config.getValue(path));
    } else if (outputType == List.class) {
      List<?> objectList = config.getAnyRefList(path);

      if (objectList.isEmpty()) {
        return setFieldValue(field, inst, Collections.emptyList());
      }

      Class<?> listElementType = objectList.get(0).getClass();

      if (listElementType == Boolean.class) {
        return setFieldValue(field, inst, config.getBooleanList(path));
      } else if (listElementType == Integer.class) {
        return setFieldValue(field, inst, config.getIntList(path));
      } else if (listElementType == Double.class) {
        return setFieldValue(field, inst, config.getDoubleList(path));
      } else if (listElementType == Long.class) {
        return setFieldValue(field, inst, config.getLongList(path));
      } else if (listElementType == String.class) {
        return setFieldValue(field, inst, config.getStringList(path));
      } else if (listElementType == Duration.class) {
        return setFieldValue(field, inst, config.getDurationList(path));
      } else if (listElementType == ConfigMemorySize.class) {
        return setFieldValue(field, inst, config.getMemorySizeList(path));
      } else if (listElementType == Object.class) {
        return setFieldValue(field, inst, config.getAnyRefList(path));
      } else if (listElementType == Config.class) {
        return setFieldValue(field, inst, config.getConfigList(path));
      } else if (listElementType == ConfigObject.class) {
        return setFieldValue(field, inst, config.getObjectList(path));
      } else if (listElementType == ConfigValue.class) {
        return setFieldValue(field, inst, config.getList(path));
      } else {
        throw new ConfigException.BadBean(
            "Bean property '" + path + "' of class " + typeClass.getName() + " has unsupported list element type " + listElementType);
      }
    } else {
      return new CauldronTypeSafeConfig<>(outputType).convert(config.getConfig(path))
          .map(convertedValue -> setFieldValue(field, inst, convertedValue))
          .isPresent();
    }
  }

  private void setPrimitiveValue(Class outputType, Field field, T inst, Config config, String path) {
    LOG.debug("setPrimitiveValue() -> Primitive output type: " + outputType.getCanonicalName());
    // easy
    if (outputType == Boolean.class || outputType == boolean.class) {
      setFieldValue(field, inst, config.getBoolean(path));
    } else if (outputType == Integer.class || outputType == int.class) {
      setFieldValue(field, inst, config.getInt(path));
    } else if (outputType == Long.class || outputType == long.class) {
      setFieldValue(field, inst, config.getLong(path));
    } else if (outputType == Float.class || outputType == float.class) {
      setFieldValue(field, inst, (float) config.getDouble(path));
    } else if (outputType == Double.class || outputType == double.class) {
      setFieldValue(field, inst, config.getDouble(path));
    }
  }

  /**
   * Converts a HOCON {@link Config} into type Type T. where T has identical field names / types to the config, or where a {@literal @}{@link
   * CfgKey} mapping exists.
   *
   * @param config a Config
   *
   * @return Optional instance of T after conversion.
   */
  public Optional<T> convert(Config config) {
    return newInstance(typeClass).map(inst -> {
      final Field[] fields = typeClass.getFields();

      if (fields.length == 0) {
        LOG.warn("No public configuration fields were supplied for class; " + typeClass.getName());
      }

      Arrays.stream(fields).forEach(field -> {
        Class outputType = field.getType();
        final String configPathKey = getFieldAnnotation(field, CfgKey.class).map(CfgKey::path).orElse(field.getName());

        LOG.debug("outputField: " + field.getName() + " to key: " + configPathKey + ", for type: " + outputType.getCanonicalName());

        Config opConfig = null;
        if (!configPathKey.isEmpty()) {
          int lastIdx = configPathKey.lastIndexOf(ConfigScope.PATH_SEPARATOR);
          if (lastIdx > -1) {
            String pathPrefix = configPathKey.substring(0, lastIdx - 1);
            try {
              opConfig = config.getConfig(pathPrefix);
            } catch (Exception ex) {
              LOG.warn("Config " + pathPrefix + " is not present");
            }
            opPath = configPathKey.substring(lastIdx);
          } else {
            opConfig = config;
            opPath = configPathKey;
          }

          LOG.debug("SourcePath: " + configPathKey + ", Operational Path: " + opPath);

          if (opConfig != null && opConfig.hasPath(configPathKey)) {
            // if the model is expecting a primitive, try to extract a primitive from config
            if (outputType.isPrimitive()) {
              setPrimitiveValue(outputType, field, inst, opConfig, opPath);
            } else {
              setNonPrimitiveValue(outputType, field, inst, opConfig, opPath);
            }
          } else {
            LOG.warn("operationConfig: " + configPathKey + " doesn't exist, skipping!");
          }
        }

      });
      return Optional.of(inst);
    }).orElseThrow(() -> new CauldronServerException("Could not create instance of configuration: " + typeClass.getName()));
  }
}
