package com.decoded.cauldron.api.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Configuration / Reflection utilities which support HOCON format and building instances of Java Models from configuration files.
 */
public class Glimmer {

  /**
   * Set the value of the field on a specified instance of object of type T, with value of type V.
   *
   * @param field    the field
   * @param instance the object target
   * @param value    the value to set
   * @param <T>      the type of object
   * @param <V>      the type of value
   *
   * @return true if the value was set, false otherwise.
   */
  public static <T, V> boolean setFieldValue(Field field, T instance, V value) {
    try {
      field.set(instance, value);
      return true;
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      return false;
    }
  }

  /**
   * Optionally returns the Annotation of the specified annotation class type if it exists.
   *
   * @param field           the field
   * @param annotationClass the annotation class to get
   * @param <T>             the type of annotation class
   *
   * @return Otional instance of T, the declared annotation
   */
  public static <T extends Annotation> Optional<T> getFieldAnnotation(Field field, Class<T> annotationClass) {
    try {
      return Optional.ofNullable(field.getDeclaredAnnotation(annotationClass));
    } catch (NullPointerException ex) {
      return Optional.empty();
    }
  }

  /**
   * Optionally returns a new instance of clazz, or empty.
   *
   * @param clazz a Class
   * @param <T>   instance of clazz
   *
   * @return Optional of the class
   */
  public static <T> Optional<T> newInstance(Class<T> clazz) {
    try {
      return Optional.of(clazz.newInstance());
    } catch (IllegalAccessException | InstantiationException ex) {
      return Optional.empty();
    }
  }
}