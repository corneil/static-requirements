package com.github.corneil.requirements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresStatic {
	/**
	 * The class that will act as the template.
	 * All static methods will be required in implementation.
	 * @return
	 */
	Class value();
}
