package com.github.corneil.requirements;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes({"com.github.corneil.requirements.RequiresStatic"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class RequiresStaticProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (final Element element : roundEnv.getElementsAnnotatedWith(RequiresStatic.class)) {
		}
		return true;
	}
}
