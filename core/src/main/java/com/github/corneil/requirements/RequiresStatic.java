package com.github.corneil.requirements;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequiresStatic {
	/**
	 * The class that will act as the template.
	 * All static methods will be required in implementation.
	 * @return
	 */
	Class value();
}
