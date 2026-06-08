/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.apt;

import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.descriptor.StructuredFamilies;
import io.github.argonizer.prooopt.model.ModelTier;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Compile-time processor for {@code @PromptFunction}. Per annotated method it runs Layer-1 keyword
 * heuristics, validates single-brace {@code {param}} placeholder binding, enforces PrOOPt's generic
 * rules, and emits a prose-at-LOCAL advisory. Per enclosing type it generates a
 * {@code <Name>PromptDescriptor} capturing each method's generic shape before erasure.
 */
@SupportedAnnotationTypes("io.github.argonizer.prooopt.annotation.PromptFunction")
public final class PromptFunctionProcessor extends AbstractProcessor {

    private final Set<String> generated = new java.util.HashSet<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        // Group annotated methods by their enclosing type for descriptor generation.
        Map<TypeElement, List<DescriptorWriter.Entry>> byType = new LinkedHashMap<>();

        for (Element el : roundEnv.getElementsAnnotatedWith(PromptFunction.class)) {
            if (el.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) el;
            PromptFunction ann = method.getAnnotation(PromptFunction.class);
            if (ann == null) {
                continue;
            }
            String prompt = ann.prompt();
            TypeElement enclosing = (TypeElement) method.getEnclosingElement();
            String location = method.getSimpleName() + "() in " + enclosing.getQualifiedName();

            TypeMirror returnType = method.getReturnType();

            // 1) Generic shape validation + capture (before erasure).
            GenericTypeValidator.AnalyzedReturn analyzed = GenericTypeValidator.analyze(returnType);
            for (String error : analyzed.errors()) {
                error(method, location, error);
            }

            // 2) Layer-1 keyword heuristics on prompt vs. return type.
            PromptHeuristics.Kind kind = classify(returnType);
            Optional<PromptHeuristics.Finding> finding =
                    PromptHeuristics.evaluate(prompt, kind, simpleReturnName(returnType));
            finding.ifPresent(f -> diagnostic(f.severity(), method, location, f.detail()));

            // 3) Placeholder binding against parameter names.
            validatePlaceholders(method, location, prompt);

            // 4) Prose-at-LOCAL advisory (Step 3, also surfaced here at Layer 1).
            if (ann.model() == ModelTier.LOCAL && PromptHeuristics.isProse(prompt)) {
                diagnostic(PromptHeuristics.Severity.WARNING, method, location, """
                          Prompt suggests prose-level output but ModelTier is LOCAL.
                          Local models (~15-20 tok/s via ONNX Runtime + Phi 3.5) may cause significant latency or timeout under load.
                          Options:
                            1. Switch to model = ModelTier.CLOUD_ADVANCED for this method
                            2. Rewrite the prompt to produce a scalar/short output if prose is not actually needed""");
            }

            byType.computeIfAbsent(enclosing, k -> new ArrayList<>())
                    .add(new DescriptorWriter.Entry(
                            method.getSimpleName().toString(),
                            analyzed.rawTypeFqn(),
                            analyzed.typeArgFqns()));
        }

        byType.forEach(this::generateDescriptor);
        return false;
    }

    // ------------------------------------------------------------------ placeholder validation

    private void validatePlaceholders(ExecutableElement method, String location, String prompt) {
        List<String> params = new ArrayList<>();
        for (VariableElement p : method.getParameters()) {
            params.add(p.getSimpleName().toString());
        }
        for (String placeholder : PlaceholderBinding.placeholders(prompt)) {
            if (params.contains(placeholder)) {
                continue;
            }
            StringBuilder detail = new StringBuilder()
                    .append("  Prompt contains '{").append(placeholder)
                    .append("}' but no parameter named '").append(placeholder).append("' exists.\n")
                    .append("  Method parameters are: ")
                    .append(params.isEmpty() ? "(none)" : String.join(", ", params));
            Optional<String> suggestion = PlaceholderBinding.suggest(placeholder, params);
            suggestion.ifPresent(s -> detail.append("\n  Did you mean '{").append(s).append("}'?"));
            // Header already carries method/class; emit detail without re-prefixing.
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "[PrOOPt ERROR] " + location + "\n" + detail, method);
        }
    }

    // ------------------------------------------------------------------ descriptor generation

    private void generateDescriptor(TypeElement type, List<DescriptorWriter.Entry> entries) {
        String pkg = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        String simple = type.getSimpleName().toString();
        String descriptorSimple = DescriptorWriter.descriptorSimpleName(simple);
        String fqn = pkg.isEmpty() ? descriptorSimple : pkg + "." + descriptorSimple;
        if (!generated.add(fqn)) {
            return;
        }
        String source = DescriptorWriter.write(pkg, simple, entries);
        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(fqn, type);
            try (Writer w = file.openWriter()) {
                w.write(source);
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "[PrOOPt ERROR] failed to generate " + fqn + ": " + e.getMessage(), type);
        }
    }

    // ------------------------------------------------------------------ return-type classification

    private PromptHeuristics.Kind classify(TypeMirror t) {
        if (t.getKind().isPrimitive()) {
            return switch (t.getKind()) {
                case INT, LONG, SHORT, BYTE -> PromptHeuristics.Kind.INTEGRAL;
                case DOUBLE, FLOAT -> PromptHeuristics.Kind.DECIMAL;
                case BOOLEAN -> PromptHeuristics.Kind.BOOLEAN;
                default -> PromptHeuristics.Kind.OTHER;
            };
        }
        if (!(t instanceof DeclaredType dt)) {
            return PromptHeuristics.Kind.OTHER;
        }
        String fqn = dt.asElement().toString();
        if (fqn.equals("java.lang.String") || fqn.equals("java.lang.CharSequence")) {
            return PromptHeuristics.Kind.TEXT;
        }
        if (fqn.equals("java.lang.Integer") || fqn.equals("java.lang.Long")
                || fqn.equals("java.lang.Short") || fqn.equals("java.lang.Byte")
                || fqn.equals("java.math.BigInteger")) {
            return PromptHeuristics.Kind.INTEGRAL;
        }
        if (fqn.equals("java.lang.Double") || fqn.equals("java.lang.Float")
                || fqn.equals("java.math.BigDecimal")) {
            return PromptHeuristics.Kind.DECIMAL;
        }
        if (fqn.equals("java.lang.Boolean")) {
            return PromptHeuristics.Kind.BOOLEAN;
        }
        if (StructuredFamilies.isMapFamily(fqn)) {
            return PromptHeuristics.Kind.MAP;
        }
        if (StructuredFamilies.isListFamily(fqn) || StructuredFamilies.isSetFamily(fqn)) {
            return PromptHeuristics.Kind.LIST;
        }
        return PromptHeuristics.Kind.OTHER;
    }

    private String simpleReturnName(TypeMirror t) {
        if (t.getKind() == TypeKind.VOID) {
            return "void";
        }
        if (t.getKind().isPrimitive()) {
            return t.toString();
        }
        return GenericTypeValidator.renderSimple(t);
    }

    // ------------------------------------------------------------------ diagnostics

    private void error(Element el, String location, String detail) {
        diagnostic(PromptHeuristics.Severity.ERROR, el, location, detail);
    }

    private void diagnostic(PromptHeuristics.Severity severity, Element el, String location,
                            String detail) {
        Diagnostic.Kind kind = severity == PromptHeuristics.Severity.ERROR
                ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
        String tag = severity == PromptHeuristics.Severity.ERROR ? "[PrOOPt ERROR] " : "[PrOOPt WARN] ";
        String body = detail.startsWith("  ") ? detail : "  " + detail;
        processingEnv.getMessager().printMessage(kind, tag + location + "\n" + body, el);
    }
}
