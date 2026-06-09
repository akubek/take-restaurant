-- CUSTOMERS
INSERT INTO customer (id, first_name, last_name, phone_number, email)
VALUES
(1, 'Jan', 'Kowalski', '123456789', 'jan@example.com'),
(2, 'Anna', 'Nowak', '987654321', 'anna@example.com');

-- DISHES
INSERT INTO dish (id, name, description, price_in_cents, calories, spiciness)
VALUES
(1, 'Pizza Margherita', 'Classic pizza with cheese', 3200, 850, 'MILD'),
(2, 'Spicy Ramen', 'Hot ramen soup', 4200, 650, 'HOT');

-- INGREDIENTS
INSERT INTO ingredients (id, name, is_vegan, unit)
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
INSERT INTO orders (id, customer_id, order_date_time, table_number, status)
VALUES
(1, 1, CURRENT_TIMESTAMP(), 5, 'OPEN');

-- ORDER ITEMS
INSERT INTO order_item
(id, order_id, dish_id, dish_price_at_order_time, quantity, seat_number, notes, status)
VALUES
(1, 1, 1, 3200, 2, 1, 'Extra cheese', 'NEW');