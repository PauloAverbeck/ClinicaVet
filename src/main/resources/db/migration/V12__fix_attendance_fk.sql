-- Remove a FK antiga que referenciava "animal"
ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS attendance_animal_id_fkey;

-- Cria a FK correta, apontando para "pet"
ALTER TABLE attendance
    ADD CONSTRAINT attendance_animal_id_fkey
        FOREIGN KEY (animal_id)
        REFERENCES pet(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE;