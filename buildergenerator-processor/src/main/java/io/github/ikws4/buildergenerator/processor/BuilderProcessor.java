package io.github.ikws4.buildergenerator.processor;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import io.github.ikws4.buildergenerator.Builder;
import io.github.ikws4.buildergenerator.BuilderProperty;

@AutoService(Processor.class)
public class BuilderProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Class<?>[] supportedAnnotations = {Builder.class};
    Set<String> supportedAnnotationTypes = new HashSet<>();
    for (Class<?> annotation : supportedAnnotations) {
      supportedAnnotationTypes.add(annotation.getCanonicalName());
    }
    return supportedAnnotationTypes;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) { // currently only Builder is supported
      for (Element builder : roundEnv.getElementsAnnotatedWith(annotation)) {

        // get all properties annotated with @BuilderProperty
        List<Element> properties = builder.getEnclosedElements()
          .stream()
          .filter(p -> p.getAnnotation(BuilderProperty.class) != null)
          .collect(Collectors.toList());

        String packageName = processingEnv.getElementUtils()
          .getPackageOf(builder)
          .getQualifiedName()
          .toString();

        JavaFile javaFile = JavaFile.builder(packageName, generateBuilderClass(builder, properties))
          .build();

        try {
          javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return true;
  }

  private TypeSpec generateBuilderClass(Element classElement, List<Element> properties) {
    ClassName className = ClassName.bestGuess(classElement.getSimpleName().toString() + "Builder");
    return TypeSpec.classBuilder(className)
      .addFields(generateBuilderField(properties))
      .addMethods(generateBuilderMethods(className, properties))
      .addMethod(generateBuilderBuildMethod(ClassName.get(classElement.asType()), properties))
      .build();
  }

  private List<FieldSpec> generateBuilderField(List<Element> properties) {
    return properties.stream()
      .map(p -> {
        TypeName type = ClassName.get(p.asType());
        String fieldName = p.getSimpleName().toString();
        return FieldSpec.builder(type, fieldName, Modifier.PRIVATE).build();
      })
      .collect(Collectors.toList());
  }

  private Iterable<MethodSpec> generateBuilderMethods(ClassName className,
      List<Element> properties) {
    return properties.stream()
      .map(p -> {
        String methodName = p.getSimpleName().toString();
        TypeName paramType = ClassName.get(p.asType());
        String paramName = p.getSimpleName().toString();
        return MethodSpec.methodBuilder(methodName)
          .returns(className)
          .addParameter(paramType, paramName)
          .addStatement("this.$N = $N", paramName, paramName)
          .addStatement("return this")
          .build();
      })
      .collect(Collectors.toList());
  }

  private MethodSpec generateBuilderBuildMethod(TypeName typeName, List<Element> properties) {
    
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build")
      .returns(typeName)
      .addModifiers(Modifier.PUBLIC)
      .addStatement("$T instance = new $T()", typeName, typeName);

    properties.stream().forEach(p -> {
      String fieldName = p.getSimpleName().toString();
      methodBuilder.addStatement("instance.$N = $N", fieldName, fieldName);
    });
    
    methodBuilder.addStatement("return instance");
    return methodBuilder.build();
  }

}
