/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.boot.jpa;

import io.github.argonizer.states.store.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of {@link PersonaStore}. Owns the transaction boundary.
 */
@Transactional
public class JpaPersonaStore implements PersonaStore {

    private final PersonaStateJpaRepository stateRepo;
    private final PersonaIndexJpaRepository indexRepo;
    private final PersonaHistoryJpaRepository historyRepo;
    private final PersonaMetricJpaRepository metricRepo;
    private final PersonaMetricHistoryJpaRepository metricHistoryRepo;
    private final EntityManager em;

    public JpaPersonaStore(PersonaStateJpaRepository stateRepo,
                           PersonaIndexJpaRepository indexRepo,
                           PersonaHistoryJpaRepository historyRepo,
                           PersonaMetricJpaRepository metricRepo,
                           PersonaMetricHistoryJpaRepository metricHistoryRepo,
                           EntityManager em) {
        this.stateRepo = stateRepo;
        this.indexRepo = indexRepo;
        this.historyRepo = historyRepo;
        this.metricRepo = metricRepo;
        this.metricHistoryRepo = metricHistoryRepo;
        this.em = em;
    }

    @Override
    public void persist(PersonaWriteUnit unit) {
        // State
        PersonaStateEntity state = toStateEntity(unit.stateRecord());
        stateRepo.save(state);

        // Index: delete-then-insert
        indexRepo.deleteByPersonaIdAndPersonaType(
                unit.stateRecord().getPersonaId(), unit.stateRecord().getPersonaType());
        for (PersonaIndexRecord ir : unit.indexRecords()) {
            indexRepo.save(toIndexEntity(ir));
        }

        // History
        historyRepo.save(toHistoryEntity(unit.historyRecord()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaStateRecord> findById(String personaId, String personaType) {
        return stateRepo.findByPersonaIdAndPersonaType(personaId, personaType)
                .map(this::toStateRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaStateRecord> findAllActive(String personaType) {
        return stateRepo.findByPersonaTypeAndRetiredFalse(personaType)
                .stream().map(this::toStateRecord).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaStateRecord> findAllRetired(String personaType) {
        return stateRepo.findByPersonaTypeAndRetiredTrue(personaType)
                .stream().map(this::toStateRecord).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaStateRecord> findAll(String personaType) {
        return stateRepo.findByPersonaType(personaType)
                .stream().map(this::toStateRecord).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaStateRecord> findWhere(String personaType,
                                              List<IndexCondition> conditions,
                                              boolean includeRetired) {
        if (conditions.isEmpty()) {
            return includeRetired ? findAll(personaType) : findAllActive(personaType);
        }

        // Build the SQL manually with parameterised queries against the index table
        StringBuilder jpql = new StringBuilder(
            "SELECT DISTINCT s FROM PersonaStateEntity s " +
            "WHERE s.personaType = :personaType "
        );
        if (!includeRetired) jpql.append("AND s.retired = false ");

        List<Object> params = new ArrayList<>();
        int paramIdx = 0;
        String pendingLogical = "AND";

        for (IndexCondition cond : conditions) {
            String logOp = cond.logicalOp() != null ? cond.logicalOp() : "AND";
            if (cond.operator().equalsIgnoreCase("IN")) {
                @SuppressWarnings("unchecked")
                List<Object> inList = (List<Object>) cond.parameter();
                List<String> placeholders = new ArrayList<>();
                for (Object v : inList) {
                    placeholders.add(":p" + paramIdx);
                    params.add(v.toString());
                    paramIdx++;
                }
                jpql.append(logOp).append(" s.personaId IN (")
                    .append("SELECT idx.personaId FROM PersonaIndexEntity idx ")
                    .append("WHERE idx.personaType = :personaType ")
                    .append("AND idx.traitName = :tn").append(paramIdx).append(" ")
                    .append("AND idx.traitValue IN (").append(String.join(",", placeholders)).append(")) ");
                params.add(cond.traitName());
                paramIdx++;
            } else {
                String compOp = toJpqlOp(cond.operator());
                jpql.append(logOp).append(" s.personaId IN (")
                    .append("SELECT idx.personaId FROM PersonaIndexEntity idx ")
                    .append("WHERE idx.personaType = :personaType ")
                    .append("AND idx.traitName = :tn").append(paramIdx).append(" ")
                    .append("AND CAST(idx.traitValue AS java.lang.Double) ").append(compOp)
                    .append(" :pv").append(paramIdx).append(") ");
                params.add(cond.traitName()); // tn{idx}
                params.add(cond.parameter()); // pv{idx}
                paramIdx++;
            }
        }

        // Fall back to simpler approach: load all active + filter in-memory using a sub-query join
        // For robustness, use the criteria API path below instead of dynamic JPQL
        return findWhereViaCriteria(personaType, conditions, includeRetired);
    }

    private List<PersonaStateRecord> findWhereViaCriteria(String personaType,
                                                           List<IndexCondition> conditions,
                                                           boolean includeRetired) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PersonaStateEntity> cq = cb.createQuery(PersonaStateEntity.class);
        Root<PersonaStateEntity> state = cq.from(PersonaStateEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(state.get("personaType"), personaType));
        if (!includeRetired) predicates.add(cb.equal(state.get("retired"), false));

        for (IndexCondition cond : conditions) {
            Subquery<String> sub = cq.subquery(String.class);
            Root<PersonaIndexEntity> idx = sub.from(PersonaIndexEntity.class);
            sub.select(idx.get("personaId"));
            List<Predicate> subPreds = new ArrayList<>();
            subPreds.add(cb.equal(idx.get("personaType"), personaType));
            subPreds.add(cb.equal(idx.get("traitName"), cond.traitName()));

            if (cond.operator().equalsIgnoreCase("IN")) {
                @SuppressWarnings("unchecked")
                List<Object> inList = (List<Object>) cond.parameter();
                List<String> strValues = inList.stream().map(Object::toString).collect(Collectors.toList());
                subPreds.add(idx.get("traitValue").in(strValues));
            } else {
                String val = cond.parameter().toString();
                Expression<Double> cast = idx.get("traitValue").as(Double.class);
                double numVal;
                try { numVal = Double.parseDouble(val); } catch (NumberFormatException e) { numVal = 0; }
                subPreds.add(buildNumericPredicate(cb, cast, cond.operator(), numVal));
            }
            sub.where(subPreds.toArray(new Predicate[0]));
            predicates.add(state.get("personaId").in(sub));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        return em.createQuery(cq).getResultList().stream()
                .map(this::toStateRecord)
                .collect(Collectors.toList());
    }

    private Predicate buildNumericPredicate(CriteriaBuilder cb,
                                            Expression<Double> expr,
                                            String operator,
                                            double value) {
        return switch (operator) {
            case "="  -> cb.equal(expr, value);
            case "!=" -> cb.notEqual(expr, value);
            case "<"  -> cb.lessThan(expr, value);
            case "<=" -> cb.lessThanOrEqualTo(expr, value);
            case ">"  -> cb.greaterThan(expr, value);
            case ">=" -> cb.greaterThanOrEqualTo(expr, value);
            default   -> cb.equal(expr, value);
        };
    }

    @Override
    public void updateRetirement(String personaId, String personaType, boolean retired,
                                 Instant retiredAt, String retirementReason) {
        stateRepo.updateRetirement(personaId, personaType, retired, retiredAt, retirementReason);
    }

    @Override
    public void persistMetric(PersonaMetricRecord metric, PersonaMetricHistoryRecord history) {
        metricRepo.save(toMetricEntity(metric));
        metricHistoryRepo.save(toMetricHistoryEntity(history));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonaMetricRecord> findMetric(String personaId, String personaType, String metricName) {
        return metricRepo.findByPersonaIdAndPersonaTypeAndMetricName(personaId, personaType, metricName)
                .map(this::toMetricRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaHistoryRecord> findHistory(String personaId, String personaType,
                                                  Instant from, Instant to) {
        return historyRepo.findHistory(personaId, personaType, from, to)
                .stream().map(this::toHistoryRecord).collect(Collectors.toList());
    }

    // --- Mapping helpers ---

    private PersonaStateEntity toStateEntity(PersonaStateRecord r) {
        PersonaStateEntity e = new PersonaStateEntity();
        e.setPersonaId(r.getPersonaId());
        e.setPersonaType(r.getPersonaType());
        e.setState(r.getState());
        e.setRetired(r.isRetired());
        e.setRetiredAt(r.getRetiredAt());
        e.setRetirementReason(r.getRetirementReason());
        e.setSeed(r.getSeed());
        e.setOriginationDate(r.getOriginationDate());
        e.setLastUpdated(r.getLastUpdated());
        e.setLastEvolved(r.getLastEvolved());
        e.setCurrentPhase(r.getCurrentPhase());
        e.setStateVersion(r.getStateVersion());
        return e;
    }

    private PersonaStateRecord toStateRecord(PersonaStateEntity e) {
        PersonaStateRecord r = new PersonaStateRecord();
        r.setPersonaId(e.getPersonaId());
        r.setPersonaType(e.getPersonaType());
        r.setState(e.getState());
        r.setRetired(e.isRetired());
        r.setRetiredAt(e.getRetiredAt());
        r.setRetirementReason(e.getRetirementReason());
        r.setSeed(e.getSeed());
        r.setOriginationDate(e.getOriginationDate());
        r.setLastUpdated(e.getLastUpdated());
        r.setLastEvolved(e.getLastEvolved());
        r.setCurrentPhase(e.getCurrentPhase());
        r.setStateVersion(e.getStateVersion());
        return r;
    }

    private PersonaIndexEntity toIndexEntity(PersonaIndexRecord r) {
        PersonaIndexEntity e = new PersonaIndexEntity();
        e.setPersonaId(r.getPersonaId());
        e.setPersonaType(r.getPersonaType());
        e.setTraitName(r.getTraitName());
        e.setTraitValue(r.getTraitValue());
        e.setTraitType(r.getTraitType() != null ? r.getTraitType() : "STRING");
        return e;
    }

    private PersonaHistoryEntity toHistoryEntity(PersonaHistoryRecord r) {
        PersonaHistoryEntity e = new PersonaHistoryEntity();
        e.setPersonaId(r.getPersonaId());
        e.setPersonaType(r.getPersonaType());
        e.setChangedAt(r.getChangedAt());
        e.setStateVersion(r.getStateVersion());
        e.setPromptInput(r.getPromptInput());
        e.setFieldsChanged(r.getFieldsChanged());
        e.setFullStateAfter(r.getFullStateAfter());
        e.setUpdateSource(r.getUpdateSource());
        return e;
    }

    private PersonaHistoryRecord toHistoryRecord(PersonaHistoryEntity e) {
        PersonaHistoryRecord r = new PersonaHistoryRecord();
        r.setHistoryId(e.getHistoryId());
        r.setPersonaId(e.getPersonaId());
        r.setPersonaType(e.getPersonaType());
        r.setChangedAt(e.getChangedAt());
        r.setStateVersion(e.getStateVersion());
        r.setPromptInput(e.getPromptInput());
        r.setFieldsChanged(e.getFieldsChanged());
        r.setFullStateAfter(e.getFullStateAfter());
        r.setUpdateSource(e.getUpdateSource());
        return r;
    }

    private PersonaMetricEntity toMetricEntity(PersonaMetricRecord r) {
        PersonaMetricEntity e = new PersonaMetricEntity();
        e.setPersonaId(r.getPersonaId());
        e.setPersonaType(r.getPersonaType());
        e.setMetricName(r.getMetricName());
        e.setMetricValue(r.getMetricValue());
        e.setComputedAt(r.getComputedAt());
        return e;
    }

    private PersonaMetricRecord toMetricRecord(PersonaMetricEntity e) {
        PersonaMetricRecord r = new PersonaMetricRecord();
        r.setPersonaId(e.getPersonaId());
        r.setPersonaType(e.getPersonaType());
        r.setMetricName(e.getMetricName());
        r.setMetricValue(e.getMetricValue());
        r.setComputedAt(e.getComputedAt());
        return r;
    }

    private PersonaMetricHistoryEntity toMetricHistoryEntity(PersonaMetricHistoryRecord r) {
        PersonaMetricHistoryEntity e = new PersonaMetricHistoryEntity();
        e.setPersonaId(r.getPersonaId());
        e.setPersonaType(r.getPersonaType());
        e.setMetricName(r.getMetricName());
        e.setMetricValue(r.getMetricValue());
        e.setComputedAt(r.getComputedAt());
        return e;
    }

    private String toJpqlOp(String op) {
        return switch (op) {
            case "=" -> "=";
            case "!=" -> "<>";
            default -> op;
        };
    }
}
