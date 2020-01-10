package com.decoded.cauldron.api.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * This represents the configuration key for a configuration.
 * In Hocon configuration, the key <code>path.to.some.key</code> with value <code>aValue</code> is represented as
 * <pre>
 * {
 *   path {
 *     to {
 *       some {
 *         key = aValue
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CfgKey {
  /**
   * The configuration path that should represent this key.
   *
   * @return a {@link String} such as <code>foo</code> or <code>foo.bar.baz</code>
   */
  String path() default "";
}
