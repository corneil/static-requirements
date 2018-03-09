package com.github.corneil.requirements;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RequiresStaticProcessor extends AbstractProcessor {
	Set<String> processed = new ConcurrentSkipListSet<>();
	private static final boolean debug = Boolean.parseBoolean(System.getProperty("static.processor.debug", "false"));

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (debug) {
			System.out.println("process:" + annotations + ":" + roundEnv);
		}
		boolean failure = false;
		for (final Element element : roundEnv.getRootElements()) {
			if (checkElement(roundEnv, element)) {
				failure = true;
			}
			for (Element enclosed : element.getEnclosedElements()) {
				if (checkElement(roundEnv, enclosed)) {
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
				return true;
			}
		}
		return false;
	}

	private void checkClass(TypeElement element, RoundEnvironment roundEnv) throws RequiresStaticException {
		if (debug) {
			System.out.println("checkClass:" + element);
		}
		if (!processed.contains(element.getSimpleName().toString())) {
			processed.add(element.getSimpleName().toString());
			Set<String> classes = findRequiresStaticTemplate(element);
			if(debug) {
				System.out.println("checkClass:templateClasses:" + classes);
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
			for (TypeMirror itf : typeElement.getInterfaces()) {
				if (debug) {
					System.out.println("findRequiresStaticTemplate:" + itf);
				}
				RequiresStatic requiresStatic = itf.getAnnotation(RequiresStatic.class);
				if (requiresStatic != null) {
					result.add(requiresStatic.getClass().getCanonicalName());
				}
			}
			if (typeElement.getSuperclass() != null) {
				result.addAll(findRequiresStaticTemplate((TypeElement) processingEnv.getTypeUtils().asElement(typeElement.getSuperclass())));
			}
		}
		return result;
	}
}
