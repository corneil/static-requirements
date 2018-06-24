package com.github.corneil.requirements;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class RequiresStaticProcessor extends AbstractProcessor {
	private static final boolean debug = Boolean.parseBoolean(System.getProperty("static.processor.debug", "false"));
	private static final String REQUIRES_METHOD = "'%s' requires method '%s' as in '%s'";
	private static final String CANNOT_FIND_ELEMENT = "Cannot find element for '%s'";
	private static final String PARAMETER_TYPES_DIFFERENT = "Parameter %d types different: %s != %s in %s";
	private final Set<String> processed = new ConcurrentSkipListSet<>();
	private final Map<String, Set<String>> mapped = new ConcurrentHashMap<>();

	private static String strip(String s1, String s2) {
		if (s1.endsWith(s2)) {
			return s1.substring(0, s1.length() - s2.length());
		}
		return s1;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		final Set<String> result = new HashSet<>();
		result.add(RequiresStatic.class.getCanonicalName());
		return result;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		final String current = System.getProperty("java.version");
		for (SourceVersion version : SourceVersion.values()) {
			if (current.equals(version.name())) {
				return version;
			}
		}
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (debug) {
			System.out.println(String.format("process:%s:%s", annotations, roundEnv));
		}
		boolean failure = false;

		for (final Element element : roundEnv.getRootElements()) {
			if (!checkElement(element)) {
				failure = true;
			} else {
				for (Element enclosed : element.getEnclosedElements()) {
					if (!checkElement(enclosed)) {
						failure = true;
					}
				}
			}
		}
		return failure;
	}

	private boolean checkElement(Element element) {
		if (debug) {
			System.out.println(String.format("checkElement:%s", element));
		}
		if (element.getKind().equals(ElementKind.CLASS)) {
			try {
				checkClass((TypeElement) element);
			} catch (RequiresStaticException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
				return false;
			}
		}
		return true;
	}

	private void checkClass(TypeElement element) throws RequiresStaticException {
		if (debug) {
			System.out.println(String.format("checkClass:%s", element));
		}
		if (!processed.contains(element.getSimpleName().toString())) {
			processed.add(element.getSimpleName().toString());

			final Set<String> classes = findRequiresStaticTemplate(element);
			if (debug) {
				System.out.println(String.format("checkClass:templateClasses:%s", classes));
			}
			if (!classes.isEmpty()) {
				mapped.put(element.getSimpleName().toString(), classes);
			}
			for (String clsName : classes) {
				TypeElement templateClass = this.processingEnv.getElementUtils().getTypeElement(clsName);
				if (templateClass == null) {
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(CANNOT_FIND_ELEMENT, clsName));
				} else {
					checkWithTemplate(templateClass, element);
				}
			}
		}
	}

	private void checkWithTemplate(TypeElement templateClass, TypeElement checkClass) throws RequiresStaticException {
		for (Element element : templateClass.getEnclosedElements()) {
			if (element.getKind().equals(ElementKind.METHOD) && element.getModifiers().contains(Modifier.STATIC)) {
				if (debug) {
					System.out.println(String.format("checkWithTemplate:%s", element));
				}
				if (!checkHasMethod(checkClass, (ExecutableElement) element)) {
					throw new RequiresStaticException(String.format(REQUIRES_METHOD, checkClass.getQualifiedName(), element.getSimpleName(), templateClass.getQualifiedName()));
				}
			}
		}
	}

	private boolean checkHasMethod(TypeElement checkClass, ExecutableElement templateMethod) {
		if (debug) {
			System.out.println(String.format("checkHasMethod:%s", templateMethod));
		}
		for (Element enclosed : checkClass.getEnclosedElements()) {
			if (enclosed.getKind().equals(ElementKind.METHOD) && templateMethod.getSimpleName().equals(enclosed.getSimpleName())) {
				ExecutableElement checkMethod = (ExecutableElement) enclosed;
				if (checkCompatibleModifiers(checkMethod, templateMethod) &&
					checkHasParameters(checkMethod, templateMethod)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkCompatibleModifiers(ExecutableElement checkMethod, ExecutableElement templateMethod) {
		if (checkMethod.getModifiers().equals(templateMethod.getModifiers())) {
			return true;
		}
		if (checkMethod.getModifiers().containsAll(templateMethod.getModifiers())) {
			return true;
		}
		for (Modifier templateModifier : templateMethod.getModifiers()) {
			if (templateModifier.equals(Modifier.PRIVATE) && !checkMethod.getModifiers().contains(Modifier.PRIVATE)) {
				return false;
			}
			if (templateModifier.equals(Modifier.PROTECTED) && !checkMethod.getModifiers().contains(Modifier.PROTECTED) &&
				!checkMethod.getModifiers().contains(Modifier.PUBLIC)) {
				return false;
			}
			if (templateModifier.equals(Modifier.PUBLIC) && !checkMethod.getModifiers().contains(Modifier.PUBLIC)) {
				return false;
			}
			if (templateModifier.equals(Modifier.STATIC) && !checkMethod.getModifiers().contains(Modifier.STATIC)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkHasParameters(ExecutableElement checkMethod, ExecutableElement templateMethod) {
		final List<? extends VariableElement> templateParameters = templateMethod.getParameters();
		final List<? extends VariableElement> checkParameters = checkMethod.getParameters();
		if (templateParameters.size() != checkParameters.size()) {
			return false;
		}
		for (int i = 0; i < templateParameters.size(); i++) {
			VariableElement checkParameter = checkParameters.get(i);
			VariableElement templateParameter = templateParameters.get(i);
			if (!processingEnv.getTypeUtils().isSameType(checkParameter.asType(), templateParameter.asType())) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format(PARAMETER_TYPES_DIFFERENT, i, templateParameter.asType(), checkParameter.asType(), checkMethod.getSimpleName()));
				return false;
			}
		}
		return true;
	}

	private Set<String> findRequiresStaticTemplate(TypeElement typeElement) {
		final Set<String> result = new HashSet<>();
		if (typeElement != null) {
			if (debug) {
				System.out.println(String.format("findRequiresStaticTemplate:%s", typeElement));
			}
			for (TypeMirror itf : typeElement.getInterfaces()) {
				if (debug) {
					System.out.println(String.format("findRequiresStaticTemplate:interfaces:%s", itf));
				}
				String requiresTemplate = findTemplate(itf);
				if (requiresTemplate != null) {
					result.add(requiresTemplate);
				}
			}
		}
		return result;
	}

	private String findTemplate(TypeMirror itf) {
		if (debug) {
			System.out.println(String.format("findTemplate:itf=%s", itf));
		}
		TypeElement interfaceElement = (TypeElement) processingEnv.getTypeUtils().asElement(itf);
		List<? extends AnnotationMirror> annotationMirrors = processingEnv.getElementUtils().getAllAnnotationMirrors(interfaceElement);
		if (debug) {
			System.out.println(String.format("findTemplate:annotationMirrors=%s", annotationMirrors));
		}
		final String canonicalName = RequiresStatic.class.getCanonicalName();
		for (AnnotationMirror annotationMirror : annotationMirrors) {
			if (annotationMirror.getAnnotationType().toString().equals(canonicalName)) {
				for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
					if (entry.getKey().toString().equals("value()")) {
						return strip(entry.getValue().toString(), ".class");
					}
				}
			}
		}
		return null;
	}
}
