INSERT INTO customer (id, first_name, last_name, phone_number, email, is_active)
VALUES
(1, 'Jan', 'Kowalski', '123456789', 'jan@example.com', true),
(2, 'Anna', 'Nowak', '987654321', 'anna@example.com', true);

-- DISHES
INSERT INTO dish (id, dish_name, description, price_in_cents, calories, spiciness, is_active)
VALUES
(1, 'Pizza Margherita', 'Classic pizza with cheese', 3200, 850, 'MILD', true),
(2, 'Spicy Ramen', 'Hot ramen soup', 4200, 650, 'HOT', true);

-- INGREDIENTS
INSERT INTO ingredients (id, ingredient_name, is_vegan, unit)
VALUES
(1, 'Tomato Sauce', true, 'GRAM'),
(2, 'Cheese', false, 'GRAM'),
(3, 'Noodles', true, 'GRAM'),
(4, 'Chili', true, 'GRAM');

-- INGREDIENT ALLERGENS
INSERT INTO ingredient_allergens (ingredient_id, allergen)
VALUES
(2, 'LACTOSE'),
(3, 'GLUTEN');

-- RECIPE ITEMS
INSERT INTO recipe_items (id, dish_id, ingredient_id, amount)
VALUES
(1, 1, 1, 150),
(2, 1, 2, 200),
(3, 2, 3, 250),
(4, 2, 4, 20);

-- ORDERS
INSERT INTO orders (id, customer_id, order_date_time, table_number, order_status)
VALUES
(1, 1, CURRENT_TIMESTAMP(), 5, 'OPEN');

-- ORDER ITEMS
INSERT INTO order_item
(id, order_id, dish_id, dish_price_at_order_time, quantity, seat_number, notes, order_item_status, is_cancelled)
VALUES
(1, 1, 1, 3200, 2, 1, 'Extra cheese', 'NEW', false);

ALTER TABLE customer ALTER COLUMN id RESTART WITH 3;
ALTER TABLE dish ALTER COLUMN id RESTART WITH 3;
ALTER TABLE ingredients ALTER COLUMN id RESTART WITH 5;
ALTER TABLE recipe_items ALTER COLUMN id RESTART WITH 5;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 2;
ALTER TABLE order_item ALTER COLUMN id RESTART WITH 2;