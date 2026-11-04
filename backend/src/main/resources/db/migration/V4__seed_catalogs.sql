-- É2: initial catalog values (same as supabase/init/02_seed.sql). Admins can rename or deactivate them.
INSERT INTO body_segment (name) VALUES
    ('Hombro'), ('Codo'), ('Muñeca'), ('Mano'), ('Dedos')
ON CONFLICT DO NOTHING;

INSERT INTO surgery_type (name) VALUES
    ('Osteosíntesis con placa'), ('Tenorrafia'), ('Liberación del túnel carpiano'), ('Artrodesis')
ON CONFLICT DO NOTHING;

INSERT INTO scar_type (name) VALUES
    ('Normotrófica'), ('Hipertrófica'), ('Queloide'), ('Retráctil')
ON CONFLICT DO NOTHING;
