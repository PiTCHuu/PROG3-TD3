ALTER TABLE dish ADD COLUMN IF NOT EXISTS selling_price NUMERIC(10, 2);

CREATE TABLE IF NOT EXISTS dish_ingredient (
                                               id SERIAL PRIMARY KEY,
                                               id_dish INTEGER NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    id_ingredient INTEGER NOT NULL REFERENCES ingredient(id) ON DELETE CASCADE,
    quantity_required NUMERIC(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    UNIQUE(id_dish, id_ingredient)
    );

ALTER TABLE ingredient DROP COLUMN IF EXISTS id_dish;

ALTER TABLE ingredient DROP COLUMN IF EXISTS required_quantity;


INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity_required, unit) VALUES
                                                                                  (1, 1, 0.20, 'KG'),
                                                                                  (1, 2, 0.15, 'KG'),
                                                                                  (2, 3, 1.00, 'KG'),
                                                                                  (4, 4, 0.30, 'KG'),
                                                                                  (4, 5, 0.20, 'KG');

UPDATE dish SET selling_price = 3500.00 WHERE id = 1;
UPDATE dish SET selling_price = 12000.00 WHERE id = 2;
UPDATE dish SET selling_price = 8000.00 WHERE id = 4;