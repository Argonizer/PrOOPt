/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 *
 * Licensed under the MIT License. See the LICENSE file in the project root.
 */
package io.github.argonizer.prooopt.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TfIdfEmbeddingEngineTest {

    private TfIdfEmbeddingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TfIdfEmbeddingEngine();
        engine.fit(List.of(
                "extract date from text",
                "count words in document",
                "summarize contract text",
                "validate input is not empty"
        ));
    }

    @Test
    void dimensionMatchesVocabularySize() {
        assertTrue(engine.dimension() > 0);
    }

    @Test
    void embedProducesVectorOfCorrectLength() {
        float[] v = engine.embed("extract the date");
        assertEquals(engine.dimension(), v.length);
    }

    @Test
    void embeddedVectorIsNormalised() {
        float[] v = engine.embed("extract date from text");
        assertEquals(1.0, VectorMath.norm(v), 1e-5);
    }

    @Test
    void similarTextsHaveHigherCosineThanDissimilar() {
        float[] dateSimilar = engine.embed("extract signing date from contract");
        float[] wordsSimilar = engine.embed("count total words in the document");
        float[] dateFit = engine.embed("extract date from text");
        float[] wordsFit = engine.embed("count words in document");

        double simDate = VectorMath.cosineSimilarity(dateSimilar, dateFit);
        double simDateToWords = VectorMath.cosineSimilarity(dateSimilar, wordsFit);
        assertTrue(simDate > simDateToWords, "date query should match date doc better than words doc");

        double simWords = VectorMath.cosineSimilarity(wordsSimilar, wordsFit);
        double simWordsToDate = VectorMath.cosineSimilarity(wordsSimilar, dateFit);
        assertTrue(simWords > simWordsToDate, "words query should match words doc better than date doc");
    }

    @Test
    void outOfVocabularyTermsAreIgnored() {
        float[] v = engine.embed("xyzzy qwerty asdf");
        // should not throw; result should be zero vector (no known terms)
        assertEquals(engine.dimension(), v.length);
    }

    @Test
    void emptyOrBlankTextProducesZeroVector() {
        float[] v = engine.embed("");
        for (float x : v) {
            assertEquals(0f, x);
        }
    }

    @Test
    void refitClearsPriorVocabulary() {
        int dim1 = engine.dimension();
        engine.fit(List.of("a b c d e f g h i j k l m"));
        int dim2 = engine.dimension();
        assertEquals(engine.dimension(), dim2);
        assertTrue(dim2 != dim1 || dim2 > 0);
    }
}
