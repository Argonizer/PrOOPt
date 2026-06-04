/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.plugin;

import io.github.argonizer.prooopt.exception.PromptSemanticValidationException;
import io.github.argonizer.prooopt.plugin.internal.KeywordSemanticClassifier;
import io.github.argonizer.prooopt.plugin.internal.PromptMethod;
import io.github.argonizer.prooopt.plugin.internal.PromptMethodScanner;
import io.github.argonizer.prooopt.plugin.internal.SemanticClassifier;
import io.github.argonizer.prooopt.plugin.internal.ValidationEngine;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code prooopt:validate} goal — semantic build-time validation of {@code @PromptFunction}
 * methods, bound by default to the {@code verify} phase. Scans compiled classes, diffs the SHA-256
 * validation cache, classifies uncached methods, fails the build on INVALID (and on UNCERTAIN under
 * the {@code FAIL} policy), advises on prose at the LOCAL tier, and writes a JSON validation report.
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public final class ValidateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** Master switch for semantic validation. */
    @Parameter(property = "prooopt.semanticValidation", defaultValue = "true")
    private boolean semanticValidation;

    /** Validation model identifier (advisory; the model-free classifier is used when unavailable). */
    @Parameter(property = "prooopt.validationModel", defaultValue = "phi")
    private String validationModel;

    /** How an UNCERTAIN classification is treated: {@code FAIL} (default) or {@code WARN}. */
    @Parameter(property = "prooopt.uncertaintyPolicy", defaultValue = "FAIL")
    private String uncertaintyPolicy;

    /** Injectable classifier seam — production defaults to {@link KeywordSemanticClassifier}. */
    private SemanticClassifier classifier;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!semanticValidation) {
            getLog().info("[PrOOPt] semanticValidation=false — skipping prooopt:validate.");
            return;
        }
        Path classesDir = Path.of(project.getBuild().getOutputDirectory());
        List<PromptMethod> methods = PromptMethodScanner.scan(classesDir, buildClassLoader());
        if (methods.isEmpty()) {
            getLog().info("[PrOOPt] No @PromptFunction methods found — nothing to validate.");
            return;
        }
        getLog().info("[PrOOPt] Validating " + methods.size()
                + " @PromptFunction method(s) [model=" + validationModel + "].");

        ValidationEngine.UncertaintyPolicy policy = "WARN".equalsIgnoreCase(uncertaintyPolicy)
                ? ValidationEngine.UncertaintyPolicy.WARN
                : ValidationEngine.UncertaintyPolicy.FAIL;
        SemanticClassifier active = classifier != null ? classifier : new KeywordSemanticClassifier();
        ValidationEngine engine = new ValidationEngine(active, policy, getLog()::warn);

        Path target = Path.of(project.getBuild().getDirectory());
        Path cacheFile = target.resolve("prooopt-validation-cache.json");
        Path reportFile = target.resolve("prooopt-validation-report.json");
        try {
            engine.run(methods, cacheFile, reportFile);
            getLog().info("[PrOOPt] Semantic validation passed. Report: " + reportFile);
        } catch (PromptSemanticValidationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (Exception e) {
            throw new MojoExecutionException("[PrOOPt] validation failed to run: " + e.getMessage(), e);
        }
    }

    /** Classloader over the project's compile classpath, parented to the plugin for annotation identity. */
    private ClassLoader buildClassLoader() throws MojoExecutionException {
        try {
            List<String> elements = project.getCompileClasspathElements();
            List<URL> urls = new ArrayList<>();
            for (String element : elements) {
                urls.add(Path.of(element).toUri().toURL());
            }
            return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("[PrOOPt] could not build project classloader", e);
        }
    }

    /** Test seam: inject a deterministic classifier. */
    void setClassifier(SemanticClassifier classifier) {
        this.classifier = classifier;
    }
}
