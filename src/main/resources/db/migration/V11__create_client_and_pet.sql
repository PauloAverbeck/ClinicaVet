-- Tabela de clientes (tutores)
CREATE TABLE client (
    id            BIGSERIAL PRIMARY KEY,
    version       INT          NOT NULL DEFAULT 0,
    creation_date TIMESTAMP    NOT NULL DEFAULT now(),
    update_date   TIMESTAMP    NOT NULL DEFAULT now(),

    company_id    BIGINT       NOT NULL REFERENCES company (id),
    name          VARCHAR(200) NOT NULL,
    email         VARCHAR(320),
    phone         VARCHAR(32),
    notes         TEXT,

    deleted_at    TIMESTAMP
);

CREATE INDEX idx_client_company_id ON client (company_id);



-- Tabela de pets
CREATE TABLE pet (
    id            BIGSERIAL PRIMARY KEY,
    version       INT          NOT NULL DEFAULT 0,
    creation_date TIMESTAMP    NOT NULL DEFAULT now(),
    update_date   TIMESTAMP    NOT NULL DEFAULT now(),

    company_id    BIGINT       NOT NULL REFERENCES company (id),
    client_id     BIGINT       NOT NULL REFERENCES client (id),

    name          VARCHAR(200) NOT NULL,
    species       VARCHAR(50),
    breed         VARCHAR(100),
    birth_date    DATE,
    notes         TEXT,

    deleted_at    TIMESTAMP
);

CREATE INDEX idx_pet_company_id ON pet (company_id);
CREATE INDEX idx_pet_client_id  ON pet (client_id);