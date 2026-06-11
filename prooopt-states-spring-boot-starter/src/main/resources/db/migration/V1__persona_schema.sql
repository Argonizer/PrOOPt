-- PrOOPt persona state schema
-- Applied by Flyway when prooopt.persona.auto-ddl=true

CREATE TABLE IF NOT EXISTS prooopt_persona_state (
    persona_id        VARCHAR(255)  NOT NULL,
    persona_type      VARCHAR(255)  NOT NULL,
    state             TEXT          NOT NULL,
    retired           BOOLEAN       NOT NULL DEFAULT FALSE,
    retired_at        TIMESTAMP,
    retirement_reason VARCHAR(1000),
    seed              TEXT,
    origination_date  TIMESTAMP     NOT NULL,
    last_updated      TIMESTAMP     NOT NULL,
    last_evolved      TIMESTAMP,
    current_phase     VARCHAR(50),
    state_version     BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (persona_id, persona_type)
);

CREATE TABLE IF NOT EXISTS prooopt_persona_index (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id   VARCHAR(255) NOT NULL,
    persona_type VARCHAR(255) NOT NULL,
    trait_name   VARCHAR(255) NOT NULL,
    trait_value  VARCHAR(1000),
    trait_type   VARCHAR(50)  NOT NULL,
    UNIQUE (persona_id, persona_type, trait_name)
);

CREATE INDEX IF NOT EXISTS idx_persona_index_type_trait
    ON prooopt_persona_index (persona_type, trait_name, trait_value);

CREATE TABLE IF NOT EXISTS prooopt_persona_history (
    history_id        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id        VARCHAR(255) NOT NULL,
    persona_type      VARCHAR(255) NOT NULL,
    changed_at        TIMESTAMP    NOT NULL,
    state_version     BIGINT       NOT NULL,
    prompt_input      TEXT,
    fields_changed    VARCHAR(2000),
    full_state_after  TEXT,
    update_source     VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_persona_history_lookup
    ON prooopt_persona_history (persona_id, persona_type, changed_at);

CREATE TABLE IF NOT EXISTS prooopt_persona_metrics (
    persona_id   VARCHAR(255)   NOT NULL,
    persona_type VARCHAR(255)   NOT NULL,
    metric_name  VARCHAR(255)   NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    computed_at  TIMESTAMP      NOT NULL,
    PRIMARY KEY (persona_id, persona_type, metric_name)
);

CREATE TABLE IF NOT EXISTS prooopt_persona_metrics_history (
    id           BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id   VARCHAR(255)   NOT NULL,
    persona_type VARCHAR(255)   NOT NULL,
    metric_name  VARCHAR(255)   NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    computed_at  TIMESTAMP      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_metrics_history_lookup
    ON prooopt_persona_metrics_history (persona_id, persona_type, metric_name, computed_at);
