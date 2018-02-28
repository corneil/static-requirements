package com.github.corneil.requirements;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RequiresStaticSupport {

	public static final String S_REQUIRES_METHOD_S_AS_IN_S = "%s requires method %s as in %s";

	private RequiresStaticSupport() {
	}

	private static String makeSignature(Method method) {
		StringBuilder builder = new StringBuilder();
		if (Modifier.isPublic(method.getModifiers())) {
			builder.append("public ");
		} else if (Modifier.isProtected(method.getModifiers())) {
			builder.append("protected ");
		} else if (Modifier.isPrivate(method.getModifiers())) {
			builder.append("private ");
		}
		if (Modifier.isStatic(method.getModifiers())) {
			builder.append("static ");
		}
		if (Modifier.isSynchronized(method.getModifiers())) {
			builder.append("synchronized ");
		}
		if (method.getGenericReturnType() != null) {
			builder.append("<");
			builder.append(method.getGenericReturnType().getTypeName());
			builder.append("> ");
			builder.append(method.getGenericReturnType().getTypeName());
		} else {
			Class<?> returnType = method.getReturnType();
			if (Void.class.equals(returnType) || Void.class.isAssignableFrom(returnType)) {
				builder.append("void");
			} else {
				builder.append(returnType.getCanonicalName());
			}
		}
		builder.append(" ");
		builder.append(method.getName());
		if (method.getGenericParameterTypes() != null && method.getGenericParameterTypes().length > 0) {
			builder.append("<");
			boolean first = true;
			for (Type type : method.getGenericParameterTypes()) {
				if (!first) {
					builder.append(", ");
				} else {
					first = false;
				}
				builder.append(type.getTypeName());
			}
			builder.append(">");
		}
		builder.append("(");
		if (method.getParameterCount() > 0) {
			int idx = 0;
			for (Class<?> cls : method.getParameterTypes()) {
				if (idx != 0) {
					builder.append(", ");
				}
				builder.append(cls.getCanonicalName());
				if (cls.isArray()) {
					builder.append("[]");
				}
				builder.append(" arg");
				builder.append(idx);
				idx += 1;
			}
		}
		builder.append(")");
		return method.toString();
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
		List<Class<?>> result = new ArrayList<>();
		if (checkMe.isAnnotationPresent(RequiresStatic.class)) {
			RequiresStatic requiresStatic = checkMe.getAnnotation(RequiresStatic.class);
			result.add(requiresStatic.value());
		}
		for (Class<?> cls : checkMe.getInterfaces()) {
			result.addAll(findRequestStaticClass(cls));
		}
		if (checkMe.getSuperclass() != null) {
			result.addAll(findRequestStaticClass(checkMe.getSuperclass()));
		}
		return result;
	}

	/**
	 * This method can be used to verify the requirement that a class implements static methods like a supplied template class.
	 *
	 * @param checkMe
	 * @return true if the class hierarchy has one or RequireStatic annotations and each of their requirements are met. Will return false if the class has no annotations.
	 * @throws RequiresStaticException if any of the requirements are not met.
	 */
	public static boolean checkClass(Class<?> checkMe) throws RequiresStaticException {
		boolean foundAny = false;
		for (Class<?> templateClass : findRequestStaticClass(checkMe)) {
			for (Method method : templateClass.getMethods()) {
				if (Modifier.isStatic(method.getModifiers())) {
					if (method.getParameterTypes() != null && method.getParameterTypes().length > 0) {
						try {
							Method checkMethod = checkMe.getMethod(method.getName(), method.getParameterTypes());
							if (!compatibleModifiers(method.getModifiers(), checkMethod.getModifiers())) {
								throw new RequiresStaticException(String.format(S_REQUIRES_METHOD_S_AS_IN_S, checkMe.getCanonicalName(), makeSignature(method), templateClass.getCanonicalName()));
							}
						} catch (NoSuchMethodException x) {
							throw new RequiresStaticException(String.format(S_REQUIRES_METHOD_S_AS_IN_S, checkMe.getCanonicalName(), makeSignature(method), templateClass.getCanonicalName()));
						}
					} else {
						try {
							Method checkMethod = checkMe.getMethod(method.getName());
							if (!compatibleModifiers(method.getModifiers(), checkMethod.getModifiers())) {
								throw new RequiresStaticException(String.format(S_REQUIRES_METHOD_S_AS_IN_S, checkMe.getCanonicalName(), makeSignature(method), templateClass.getCanonicalName()));
							}
						} catch (NoSuchMethodException x) {
							throw new RequiresStaticException(String.format(S_REQUIRES_METHOD_S_AS_IN_S, checkMe.getCanonicalName(), makeSignature(method), templateClass.getCanonicalName()));
						}
					}

				}
			}
			foundAny = true;
		}
		return foundAny;
	}
}
