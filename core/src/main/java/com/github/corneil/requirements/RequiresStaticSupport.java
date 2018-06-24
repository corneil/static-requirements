package com.github.corneil.requirements;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RequiresStaticSupport {

	/**
	 * This method can be used to verify the requirement that a class implements static methods like a supplied template class.
	 *
	 * @param The class to check for @RequiresStatic annotations
	 * @return true if the class hierarchy has one or RequireStatic annotations and each of their requirements are met.
	 * 			Will return false if the class has no annotations.
	 * @throws RequiresStaticException if any of the requirements are not met.
	 * @see RequiresStatic
	 */
	public static boolean checkClass(Class<?> checkMe) throws RequiresStaticException {
		boolean foundAny = false;
		for (Class<?> templateClass : findRequestStaticClass(checkMe)) {
			for (Method method : templateClass.getMethods()) {
				if (Modifier.isStatic(method.getModifiers())) {
					checkMethod(checkMe, method, templateClass);
				}
			}
			foundAny = true;
		}
		return foundAny;
	}

	private static final String REQUIRES_METHOD = "%s requires method \"%s\" as in %s";

	private RequiresStaticSupport() {
	}

	private static boolean compatibleModifiers(int method, int template) {
		if (template == method) {
			return true;
		}
		boolean isPublic = Modifier.isPublic(method);
		boolean isProtected = Modifier.isProtected(method);
		if (Modifier.isProtected(template) && !isProtected && !isPublic) {
			return false;
		} else if (Modifier.isPublic(template) && !isPublic) {
			return false;
		}
		return !(Modifier.isStatic(template) && !Modifier.isStatic(method));
	}

	private static Collection<Class<?>> findRequestStaticClass(Class<?> checkMe) {
		Set<Class<?>> result = new HashSet<>();
		try {
			RequiresStatic requiresStatic = checkMe.getAnnotation(RequiresStatic.class);
			if (requiresStatic != null) {
				result.add(requiresStatic.value());
			}
		} catch (Throwable x) {
			// There is no annotation
		}
		for (Class<?> cls : checkMe.getInterfaces()) {
			result.addAll(findRequestStaticClass(cls));
		}
		if (checkMe.getSuperclass() != null) {
			result.addAll(findRequestStaticClass(checkMe.getSuperclass()));
		}
		return result;
	}
	private static void checkMethod(Class<?> checkMe, Method method, Class<?> templateClass) throws RequiresStaticException {
		if (method.getParameterTypes() != null && method.getParameterTypes().length > 0) {
			try {
				Method checkMethod = checkMe.getMethod(method.getName(), method.getParameterTypes());
				if (!compatibleModifiers(method.getModifiers(), checkMethod.getModifiers())) {
					throw new RequiresStaticException(String.format(REQUIRES_METHOD, checkMe.getCanonicalName(), method.toGenericString(), templateClass.getCanonicalName()));
				}
			} catch (NoSuchMethodException x) {
				throw new RequiresStaticException(String.format(REQUIRES_METHOD, checkMe.getCanonicalName(), method.toGenericString(), templateClass.getCanonicalName()));
			}
		} else {
			try {
				Method checkMethod = checkMe.getMethod(method.getName());
				if (!compatibleModifiers(method.getModifiers(), checkMethod.getModifiers())) {
					throw new RequiresStaticException(String.format(REQUIRES_METHOD, checkMe.getCanonicalName(), method.toGenericString(), templateClass.getCanonicalName()));
				}
			} catch (NoSuchMethodException x) {
				throw new RequiresStaticException(String.format(REQUIRES_METHOD, checkMe.getCanonicalName(), method.toGenericString(), templateClass.getCanonicalName()));
			}
		}
	}
}
