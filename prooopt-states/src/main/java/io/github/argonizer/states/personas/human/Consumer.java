/*
 * PrOOPt — Object-Oriented Prompt Engineering for Java.
 *
 * Copyright (c) 2026 Akshay Rawal
 * SPDX-License-Identifier: MIT
 */
package io.github.argonizer.states.personas.human;

import io.github.argonizer.states.annotation.Persona;
import io.github.argonizer.states.annotation.PersonaId;
import io.github.argonizer.states.annotation.Trait;
import io.github.argonizer.prooopt.annotation.PromptFunction;
import io.github.argonizer.prooopt.annotation.CodeFunction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Consumer persona extending {@link Person} with purchasing behaviour traits.
 */
@Persona(
    value = "A retail consumer with purchasing psychology, brand relationships, and spending behaviour.",
    trackHistory = true,
    internalLoop = true,
    evolutionSchedule = "@weekly"
)
public class Consumer extends Person {

    // --- Consumer identity ---

    @Trait("[FIXED] Preferred currency code, e.g. USD.")
    private String currency;

    @Trait("[FIXED] Primary language for communication, e.g. en-GB.")
    private String preferredLanguage;

    // --- Consumer psychology ---

    @Trait(value = "Price sensitivity: 0 (price-agnostic) to 100 (extremely price-sensitive).", index = true)
    private int priceSensitivity;

    @Trait(value = "Brand loyalty propensity: 0 (switches freely) to 100 (intensely loyal).", index = true)
    private int brandLoyaltyPropensity;

    @Trait(value = "Susceptibility to social proof and reviews: 0–100.", index = true)
    private int socialProofSusceptibility;

    @Trait(value = "Impulse purchase tendency: 0–100.", index = true)
    private int impulseTendency;

    @Trait(value = "Post-purchase dissonance likelihood: 0–100.", index = true)
    private int postPurchaseDissonance;

    @Trait(value = "Current churn risk: 0 (loyal) to 100 (about to churn).", index = true)
    private int churnRisk;

    @Trait(value = "Net Promoter Score proxy: -100 to 100.", index = true)
    private int npsProxy;

    @Trait(value = "Dominant category of spend, e.g. ELECTRONICS, FOOD, FASHION.", index = true)
    private String dominantCategory;

    @Trait(value = "Approximate average monthly spend in primary currency.", index = true)
    private double avgMonthlySpend;

    @Trait(value = "Narrative of current brand relationships: which brands are trusted/avoided.", index = false)
    private String brandRelationshipSummary;

    @Trait(value = "Summary of inferred purchase drivers: convenience, price, status, etc.", index = false)
    private String purchaseDriverSummary;

    // --- Transaction history (seed data, not LLM-modified) ---
    private List<PurchaseRecord> purchaseHistory = new ArrayList<>();

    public Consumer() { super(); }

    public Consumer(String id, String name) { super(id, name); }

    // Getters / setters

    public String getCurrency()             { return currency; }
    public void setCurrency(String v)       { this.currency = v; }
    public String getPreferredLanguage()    { return preferredLanguage; }
    public void setPreferredLanguage(String v) { this.preferredLanguage = v; }
    public int getPriceSensitivity()        { return priceSensitivity; }
    public void setPriceSensitivity(int v)  { this.priceSensitivity = v; }
    public int getBrandLoyaltyPropensity()  { return brandLoyaltyPropensity; }
    public void setBrandLoyaltyPropensity(int v) { this.brandLoyaltyPropensity = v; }
    public int getSocialProofSusceptibility()    { return socialProofSusceptibility; }
    public void setSocialProofSusceptibility(int v) { this.socialProofSusceptibility = v; }
    public int getImpulseTendency()         { return impulseTendency; }
    public void setImpulseTendency(int v)   { this.impulseTendency = v; }
    public int getPostPurchaseDissonance()  { return postPurchaseDissonance; }
    public void setPostPurchaseDissonance(int v) { this.postPurchaseDissonance = v; }
    public int getChurnRisk()               { return churnRisk; }
    public void setChurnRisk(int v)         { this.churnRisk = v; }
    public int getNpsProxy()                { return npsProxy; }
    public void setNpsProxy(int v)          { this.npsProxy = v; }
    public String getDominantCategory()     { return dominantCategory; }
    public void setDominantCategory(String v){ this.dominantCategory = v; }
    public double getAvgMonthlySpend()      { return avgMonthlySpend; }
    public void setAvgMonthlySpend(double v){ this.avgMonthlySpend = v; }
    public String getBrandRelationshipSummary() { return brandRelationshipSummary; }
    public void setBrandRelationshipSummary(String v) { this.brandRelationshipSummary = v; }
    public String getPurchaseDriverSummary(){ return purchaseDriverSummary; }
    public void setPurchaseDriverSummary(String v) { this.purchaseDriverSummary = v; }
    public List<PurchaseRecord> getPurchaseHistory() { return purchaseHistory; }
    public void setPurchaseHistory(List<PurchaseRecord> v) { this.purchaseHistory = v; }

    // --- Intrinsic rules ---

    @PromptFunction(prompt = "Derive updated churn risk from emotional valence, NPS proxy, and brand relationships.")
    public String deriveChurnRisk() {
        return "churnRisk should reflect combined dissatisfaction signals: "
             + "low emotionalValence (<30), low npsProxy (<-20), and negative brandRelationshipSummary "
             + "each add to churn risk. High brandLoyaltyPropensity mitigates it.";
    }

    @PromptFunction(prompt = "Classify and update brand relationships based on recent purchase experiences.")
    public String classifyBrandRelationships() {
        return "Update brandRelationshipSummary to reflect brands encountered recently. "
             + "Trust is built by positive experiences; distrust by negative ones. "
             + "High brandLoyaltyPropensity means existing relationships change slowly.";
    }

    @PromptFunction(prompt = "Infer purchase drivers from psychological and behavioural traits.")
    public String inferPurchaseDrivers() {
        return "purchaseDriverSummary should list the top 2–3 motivators: "
             + "price-driven if priceSensitivity > 70, status-driven if selfEsteem > 70 and priceSensitivity < 40, "
             + "convenience-driven if conscientiousness > 60 and arousal < 40.";
    }

    // --- Code functions ---

    @CodeFunction(description = "Average spend per category from purchase history.")
    public Map<String, Double> avgSpendByCategory() {
        Map<String, Double> totals = new java.util.LinkedHashMap<>();
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (PurchaseRecord p : purchaseHistory) {
            totals.merge(p.category(), p.amount(), Double::sum);
            counts.merge(p.category(), 1, Integer::sum);
        }
        Map<String, Double> result = new java.util.LinkedHashMap<>();
        totals.forEach((k, v) -> result.put(k, v / counts.get(k)));
        return result;
    }

    @CodeFunction(description = "Number of transactions in the history.")
    public int transactionFrequency() {
        return purchaseHistory.size();
    }

    @CodeFunction(description = "Days since the most recent purchase.")
    public long daysSinceLastPurchase() {
        return purchaseHistory.stream()
                .map(PurchaseRecord::purchasedAt)
                .max(Instant::compareTo)
                .map(t -> java.time.temporal.ChronoUnit.DAYS.between(t, Instant.now()))
                .orElse(Long.MAX_VALUE);
    }

    @CodeFunction(description = "The brand with the most purchases in history.")
    public String dominantBrand() {
        return purchaseHistory.stream()
                .collect(java.util.stream.Collectors.groupingBy(PurchaseRecord::brand,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
    }

    public record PurchaseRecord(String brand, String category, double amount, Instant purchasedAt) {}
}
