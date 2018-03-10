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
	Set<String> processed = new ConcurrentSkipListSet<>();
	Map<String, Set<String>> mapped = new ConcurrentHashMap<>();
	private static final boolean debug = Boolean.parseBoolean(System.getProperty("static.processor.debug", "false"));

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> result = new HashSet<>();
		result.add(RequiresStatic.class.getCanonicalName());
		return result;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		String current = System.getProperty("java.version");
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
			System.out.println("process:" + annotations + ":" + roundEnv);
		}
		boolean failure = false;

		for (final Element element : roundEnv.getRootElements()) {
			if (!checkElement(roundEnv, element)) {
				failure = true;
			}
			for (Element enclosed : element.getEnclosedElements()) {
				if (!checkElement(roundEnv, enclosed)) {
					failure = true;
				}
			}
		}
		return failure;
	}

	private boolean checkElement(RoundEnvironment roundEnv, Element element) {
		if (debug) {
			System.out.println("checkElement:" + element);
		}
		if (element.getKind().equals(ElementKind.CLASS)) {
			try {
				checkClass((TypeElement) element, roundEnv);
			} catch (RequiresStaticException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
				return false;
			}
		}
		return true;
	}

	private void checkClass(TypeElement element, RoundEnvironment roundEnv) throws RequiresStaticException {
		if (debug) {
			System.out.println("checkClass:" + element);
		}
		if (!processed.contains(element.getSimpleName().toString())) {
			processed.add(element.getSimpleName().toString());

			Set<String> classes = findRequiresStaticTemplate(element);
			if (debug) {
				System.out.println("checkClass:templateClasses:" + classes);
			}
			if (!classes.isEmpty()) {
				mapped.put(element.getSimpleName().toString(), classes);
			}
			for (String clsName : classes) {
				TypeElement templateClass = this.processingEnv.getElementUtils().getTypeElement(clsName);
				if (templateClass == null) {
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Cannot find element for %s", clsName));
				} else {
					checkWithTemplate(templateClass, (TypeElement) element);
				}
			}
		}
	}

	private void checkWithTemplate(TypeElement templateClass, TypeElement checkClass) throws RequiresStaticException {
		for (Element element : templateClass.getEnclosedElements()) {
			if (element.getKind().equals(ElementKind.METHOD) && element.getModifiers().contains(Modifier.STATIC)) {
				if (debug) {
					System.out.println("checkWithTemplate:" + element);
				}
				if (!checkHasMethod(checkClass, (ExecutableElement) element)) {
					throw new RequiresStaticException(String.format("%s requires method %s as in %s", checkClass.getQualifiedName(), element.getSimpleName(), templateClass.getQualifiedName()));
				}
			}
		}
	}

	private boolean checkHasMethod(TypeElement checkClass, ExecutableElement templateMethod) {
		if (debug) {
			System.out.println("checkHasMethod:" + templateMethod);
		}
		for (Element enclosed : checkClass.getEnclosedElements()) {
			if (enclosed.getKind().equals(ElementKind.METHOD) && templateMethod.getSimpleName().equals(enclosed.getSimpleName())) {
				ExecutableElement checkMethod = (ExecutableElement) enclosed;
				if (checkCompatibleModifiers(checkMethod, templateMethod)) {
					if (checkHasParameters(checkMethod, templateMethod)) {
						return true;
					}
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
		List<? extends VariableElement> templateParameters = templateMethod.getParameters();
		List<? extends VariableElement> checkParameters = checkMethod.getParameters();
		if (templateParameters.size() != checkParameters.size()) {
			return false;
		}
		for (int i = 0; i < templateParameters.size(); i++) {
			VariableElement checkParameter = checkParameters.get(i);
			VariableElement templateParameter = templateParameters.get(i);
			if (!processingEnv.getTypeUtils().isSameType(checkParameter.asType(), templateParameter.asType())) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Parameter %d types different: %s != %s in %s", i, templateParameter.asType(), checkParameter.asType(), checkMethod.getSimpleName()));
				return false;
			}
		}
		return true;
	}

	private Set<String> findRequiresStaticTemplate(TypeElement typeElement) {
		Set<String> result = new HashSet<>();
		if (typeElement != null) {
			if (debug) {
				System.out.println("findRequiresStaticTemplate:" + typeElement);
			}
			for (TypeMirror itf : typeElement.getInterfaces()) {
				if (debug) {
					System.out.println("findRequiresStaticTemplate:interfaces:" + itf);
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
			System.out.println("findTemplate:itf=" + itf);
		}
		TypeElement interfaceElement = (TypeElement) processingEnv.getTypeUtils().asElement(itf);
		List<? extends AnnotationMirror> annotationMirrors = processingEnv.getElementUtils().getAllAnnotationMirrors(interfaceElement);
		if (debug) {
			System.out.println("findTemplate:annotationMirrors=" + annotationMirrors);
		}
		for (AnnotationMirror annotationMirror : annotationMirrors) {
			if (annotationMirror.getAnnotationType().toString().equals(RequiresStatic.class.getCanonicalName())) {
				for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
					if (entry.getKey().toString().equals("value()")) {
						return strip(entry.getValue().toString(), ".class");
					}
				}
			}
		}
		return null;
	}

	private String strip(String s1, String s2) {
		if (s1.endsWith(s2)) {
			return s1.substring(0, s1.length() - s2.length());
		}
		return s1;
	}
}
