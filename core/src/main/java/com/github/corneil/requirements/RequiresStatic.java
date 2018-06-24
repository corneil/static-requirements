package com.github.corneil.requirements;

import java.lang.annotation.*;

/**
 * This annotation can be added to an interface and will specific another class to be used as a template when verifying the target class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequiresStatic {
	/**
	 * The class that will act as the template.
	 * All static methods will be required in implementation.
	 *
	 * @return The class the will act as the template
	 */
	Class value();
}
