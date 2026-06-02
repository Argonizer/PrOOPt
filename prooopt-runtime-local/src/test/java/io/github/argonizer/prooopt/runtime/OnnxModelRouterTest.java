/*
 * PrOOPt - Object-Oriented Prompt Engineering for Java
 * Copyright (c) 2026 Akshay Rawal
 * Licensed under the MIT License
 * https://github.com/argonizer/PrOOPt
 */
package io.github.argonizer.prooopt.runtime;

import io.github.argonizer.prooopt.exception.PrOOPtConfigException;
import io.github.argonizer.prooopt.model.ModelTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnnxModelRouterTest {

    private final OnnxModelLoader loader = mock(OnnxModelLoader.class);
    private final OnnxModelRouter router = new OnnxModelRouter(loader);

    @Test
    void supports_LOCAL_returnsTrue() {
        assertTrue(router.supports(ModelTier.LOCAL));
    }

    @Test
    void supports_CLOUD_FAST_returnsFalse() {
        assertFalse(router.supports(ModelTier.CLOUD_FAST));
    }

    @Test
    void route_LOCAL_delegatesToLoader() {
        when(loader.infer("test prompt")).thenReturn("result");
        String result = router.route("test prompt", ModelTier.LOCAL);
        assertEquals("result", result);
        verify(loader).infer("test prompt");
    }

    @Test
    void route_CLOUD_FAST_throwsPrOOPtConfigException() {
        PrOOPtConfigException ex = assertThrows(PrOOPtConfigException.class,
                () -> router.route("prompt", ModelTier.CLOUD_FAST));
        assertTrue(ex.getMessage().contains("prooopt-runtime-cloud"));
    }

    @Test
    void route_CLOUD_ADVANCED_throwsPrOOPtConfigException() {
        assertThrows(PrOOPtConfigException.class,
                () -> router.route("prompt", ModelTier.CLOUD_ADVANCED));
    }
}
