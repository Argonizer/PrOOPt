/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.store;

import io.github.argonizer.states.meta.PersonaMetaReader;
import io.github.argonizer.states.meta.PersonaMetadata;
import io.github.argonizer.states.personas.npc.GuardNpc;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonaQueryTranslatorTest {

    private final PersonaMetadata meta = PersonaMetaReader.read(GuardNpc.class);

    @Test
    void parseSingleLessThan() {
        List<IndexCondition> conds = PersonaQueryTranslator.translate("trust_in_player < -50", meta);
        assertEquals(1, conds.size());
        IndexCondition c = conds.get(0);
        assertEquals("trust_in_player", c.traitName());
        assertEquals("<", c.operator());
        assertEquals("AND", c.logicalOp());
    }

    @Test
    void parseAndCondition() {
        List<IndexCondition> conds = PersonaQueryTranslator.translate(
                "trust_in_player < -50 AND suspicion > 70", meta);
        assertEquals(2, conds.size());
        assertEquals("trust_in_player", conds.get(0).traitName());
        assertEquals("suspicion", conds.get(1).traitName());
    }

    @Test
    void parseStringEquality() {
        List<IndexCondition> conds = PersonaQueryTranslator.translate("alert_state = 'HIGH'", meta);
        assertEquals(1, conds.size());
        assertEquals("HIGH", conds.get(0).parameter());
    }

    @Test
    void parseInClause() {
        List<IndexCondition> conds = PersonaQueryTranslator.translate("alert_state IN ('HIGH', 'LOCKDOWN')", meta);
        assertEquals(1, conds.size());
        assertEquals("IN", conds.get(0).operator());
    }

    @Test
    void emptyConditionReturnsEmpty() {
        assertTrue(PersonaQueryTranslator.translate("", meta).isEmpty());
        assertTrue(PersonaQueryTranslator.translate(null, meta).isEmpty());
    }
}
