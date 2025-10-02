-- ============================================
-- SQL AI Test Database Schema & Sample Data
-- Total Records: 100+ items across 4 tables
-- ============================================

-- Drop existing tables
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;

-- ============================================
-- 1. CUSTOMERS TABLE (30 records)
-- ============================================
CREATE TABLE customers (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           name VARCHAR(100) NOT NULL,
                           email VARCHAR(100) NOT NULL UNIQUE,
                           city VARCHAR(50) NOT NULL,
                           registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           customer_grade VARCHAR(20) NOT NULL
);

INSERT INTO customers (name, email, city, registration_date, customer_grade) VALUES
                                                                                 ('Mika Virtanen', 'mika.virtanen@email.fi', 'Helsinki', '2024-03-15 10:30:00', 'Gold'),
                                                                                 ('Laura Korhonen', 'laura.korhonen@email.fi', 'Tampere', '2024-04-20 14:22:00', 'Silver'),
                                                                                 ('Jari Nieminen', 'jari.nieminen@email.fi', 'Oulu', '2024-05-10 09:15:00', 'Bronze'),
                                                                                 ('Sanna Mäkinen', 'sanna.makinen@email.fi', 'Helsinki', '2024-03-25 11:45:00', 'Platinum'),
                                                                                 ('Petri Laine', 'petri.laine@email.fi', 'Tampere', '2024-06-05 16:30:00', 'Gold'),
                                                                                 ('Anni Koskinen', 'anni.koskinen@email.fi', 'Oulu', '2024-04-12 08:20:00', 'Silver'),
                                                                                 ('Ville Heikkinen', 'ville.heikkinen@email.fi', 'Helsinki', '2024-07-01 13:10:00', 'Bronze'),
                                                                                 ('Katri Järvinen', 'katri.jarvinen@email.fi', 'Tampere', '2024-05-18 10:00:00', 'Gold'),
                                                                                 ('Mikko Lehtonen', 'mikko.lehtonen@email.fi', 'Oulu', '2024-06-22 15:40:00', 'Platinum'),
                                                                                 ('Emma Saarinen', 'emma.saarinen@email.fi', 'Helsinki', '2024-04-08 12:25:00', 'Silver'),
                                                                                 ('Juha Mattila', 'juha.mattila@email.fi', 'Tampere', '2024-07-15 09:50:00', 'Bronze'),
                                                                                 ('Liisa Rantanen', 'liisa.rantanen@email.fi', 'Oulu', '2024-03-30 14:35:00', 'Gold'),
                                                                                 ('Timo Kinnunen', 'timo.kinnunen@email.fi', 'Helsinki', '2024-05-25 11:15:00', 'Silver'),
                                                                                 ('Nina Hämäläinen', 'nina.hamalainen@email.fi', 'Tampere', '2024-06-10 16:20:00', 'Platinum'),
                                                                                 ('Antti Salo', 'antti.salo@email.fi', 'Oulu', '2024-04-28 10:40:00', 'Bronze'),
                                                                                 ('Sofia Laitinen', 'sofia.laitinen@email.fi', 'Helsinki', '2024-07-20 13:55:00', 'Gold'),
                                                                                 ('Markus Tuominen', 'markus.tuominen@email.fi', 'Tampere', '2024-05-05 09:30:00', 'Silver'),
                                                                                 ('Hanna Ahonen', 'hanna.ahonen@email.fi', 'Oulu', '2024-06-18 15:10:00', 'Bronze'),
                                                                                 ('Jukka Ojala', 'jukka.ojala@email.fi', 'Helsinki', '2024-04-15 12:00:00', 'Platinum'),
                                                                                 ('Maria Niemi', 'maria.niemi@email.fi', 'Tampere', '2024-07-08 10:25:00', 'Gold'),
                                                                                 ('Pekka Kallio', 'pekka.kallio@email.fi', 'Oulu', '2024-03-22 14:50:00', 'Silver'),
                                                                                 ('Elina Virtanen', 'elina.virtanen@email.fi', 'Helsinki', '2024-06-01 11:30:00', 'Bronze'),
                                                                                 ('Olli Mäkelä', 'olli.makela@email.fi', 'Tampere', '2024-05-12 16:45:00', 'Gold'),
                                                                                 ('Helena Peltonen', 'helena.peltonen@email.fi', 'Oulu', '2024-07-25 09:20:00', 'Platinum'),
                                                                                 ('Samuli Laakso', 'samuli.laakso@email.fi', 'Helsinki', '2024-04-05 13:40:00', 'Silver'),
                                                                                 ('Kirsi Manninen', 'kirsi.manninen@email.fi', 'Tampere', '2024-06-28 10:55:00', 'Bronze'),
                                                                                 ('Tero Savolainen', 'tero.savolainen@email.fi', 'Oulu', '2024-05-20 15:25:00', 'Gold'),
                                                                                 ('Anna Leppänen', 'anna.leppanen@email.fi', 'Helsinki', '2024-07-12 12:10:00', 'Silver'),
                                                                                 ('Risto Koivisto', 'risto.koivisto@email.fi', 'Tampere', '2024-04-18 09:45:00', 'Platinum'),
                                                                                 ('Jenni Kuusisto', 'jenni.kuusisto@email.fi', 'Oulu', '2024-06-15 14:30:00', 'Bronze');

-- ============================================
-- 2. PRODUCTS TABLE (20 records)
-- ============================================
CREATE TABLE products (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          product_name VARCHAR(100) NOT NULL,
                          category VARCHAR(50) NOT NULL,
                          price DECIMAL(10,2) NOT NULL,
                          stock_quantity INT NOT NULL,
                          manufacturer VARCHAR(100) NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO products (product_name, category, price, stock_quantity, manufacturer, created_at) VALUES
                                                                                                   ('iPhone 15 Pro', 'Electronics', 1299.99, 50, 'Apple', '2024-01-10 10:00:00'),
                                                                                                   ('Samsung Galaxy S24', 'Electronics', 999.99, 75, 'Samsung', '2024-01-15 11:30:00'),
                                                                                                   ('Sony WH-1000XM5', 'Electronics', 399.99, 120, 'Sony', '2024-02-01 09:20:00'),
                                                                                                   ('MacBook Air M3', 'Electronics', 1499.99, 30, 'Apple', '2024-01-20 14:45:00'),
                                                                                                   ('Nike Air Max', 'Fashion', 159.99, 200, 'Nike', '2024-03-05 10:15:00'),
                                                                                                   ('Adidas Ultraboost', 'Fashion', 179.99, 150, 'Adidas', '2024-03-10 11:40:00'),
                                                                                                   ('Levi''s 501 Jeans', 'Fashion', 89.99, 300, 'Levi''s', '2024-02-15 13:20:00'),
                                                                                                   ('Patagonia Jacket', 'Fashion', 249.99, 80, 'Patagonia', '2024-02-20 15:50:00'),
                                                                                                   ('Organic Coffee Beans', 'Food', 24.99, 500, 'Starbucks', '2024-04-01 08:30:00'),
                                                                                                   ('Premium Olive Oil', 'Food', 34.99, 400, 'Bertolli', '2024-04-05 09:45:00'),
                                                                                                   ('Dark Chocolate Bar', 'Food', 12.99, 600, 'Lindt', '2024-04-10 10:20:00'),
                                                                                                   ('Protein Powder', 'Food', 49.99, 250, 'Optimum Nutrition', '2024-04-15 11:35:00'),
                                                                                                   ('Clean Code Book', 'Books', 45.99, 150, 'Pearson', '2024-05-01 13:00:00'),
                                                                                                   ('The Pragmatic Programmer', 'Books', 42.99, 180, 'Addison-Wesley', '2024-05-05 14:25:00'),
                                                                                                   ('Atomic Habits', 'Books', 29.99, 220, 'Penguin', '2024-05-10 15:40:00'),
                                                                                                   ('Sapiens', 'Books', 32.99, 200, 'Harper', '2024-05-15 16:55:00'),
                                                                                                   ('Yoga Mat', 'Sports', 39.99, 180, 'Manduka', '2024-06-01 10:10:00'),
                                                                                                   ('Dumbbells Set', 'Sports', 149.99, 90, 'Bowflex', '2024-06-05 11:25:00'),
                                                                                                   ('Running Shoes', 'Sports', 129.99, 160, 'Asics', '2024-06-10 12:40:00'),
                                                                                                   ('Tennis Racket', 'Sports', 199.99, 70, 'Wilson', '2024-06-15 13:55:00');

-- ============================================
-- 3. ORDERS TABLE (40 records)
-- ============================================
CREATE TABLE orders (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        customer_id BIGINT NOT NULL,
                        order_date TIMESTAMP NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        total_amount DECIMAL(10,2) NOT NULL,
                        FOREIGN KEY (customer_id) REFERENCES customers(id)
);

INSERT INTO orders (customer_id, order_date, status, total_amount) VALUES
                                                                       (1, '2025-04-02 10:30:00', 'Delivered', 1299.99),
                                                                       (1, '2025-05-15 14:20:00', 'Delivered', 399.99),
                                                                       (2, '2025-04-10 11:45:00', 'Delivered', 249.98),
                                                                       (3, '2025-04-18 09:30:00', 'Shipped', 999.99),
                                                                       (4, '2025-05-01 16:10:00', 'Delivered', 1749.98),
                                                                       (4, '2025-06-20 13:40:00', 'Processing', 179.99),
                                                                       (5, '2025-04-25 10:20:00', 'Delivered', 159.99),
                                                                       (6, '2025-05-08 15:35:00', 'Delivered', 89.99),
                                                                       (7, '2025-06-12 12:15:00', 'Shipped', 549.97),
                                                                       (8, '2025-04-30 11:00:00', 'Delivered', 74.98),
                                                                       (9, '2025-05-22 14:50:00', 'Delivered', 1499.99),
                                                                       (10, '2025-06-05 10:40:00', 'Processing', 159.98),
                                                                       (11, '2025-07-01 13:25:00', 'Pending', 249.99),
                                                                       (12, '2025-05-18 09:55:00', 'Delivered', 129.98),
                                                                       (13, '2025-06-28 16:30:00', 'Shipped', 399.99),
                                                                       (14, '2025-04-22 11:20:00', 'Delivered', 1679.97),
                                                                       (15, '2025-07-10 14:45:00', 'Processing', 89.99),
                                                                       (16, '2025-05-12 10:10:00', 'Delivered', 199.98),
                                                                       (17, '2025-06-18 15:20:00', 'Delivered', 74.98),
                                                                       (18, '2025-07-15 12:35:00', 'Pending', 179.99),
                                                                       (19, '2025-04-08 13:50:00', 'Delivered', 2799.97),
                                                                       (20, '2025-05-28 11:15:00', 'Delivered', 249.99),
                                                                       (21, '2025-06-22 14:00:00', 'Shipped', 159.99),
                                                                       (22, '2025-07-05 10:30:00', 'Processing', 399.99),
                                                                       (23, '2025-04-15 16:40:00', 'Delivered', 269.97),
                                                                       (24, '2025-05-20 12:25:00', 'Delivered', 1499.99),
                                                                       (25, '2025-06-30 13:55:00', 'Pending', 129.99),
                                                                       (26, '2025-07-18 11:10:00', 'Processing', 89.99),
                                                                       (27, '2025-05-05 15:45:00', 'Delivered', 549.96),
                                                                       (28, '2025-06-15 10:20:00', 'Delivered', 199.99),
                                                                       (29, '2025-07-22 14:30:00', 'Shipped', 999.99),
                                                                       (1, '2025-07-25 16:15:00', 'Processing', 179.99),
                                                                       (4, '2025-08-01 11:40:00', 'Pending', 399.99),
                                                                       (9, '2025-08-10 13:20:00', 'Delivered', 249.99),
                                                                       (14, '2025-08-15 10:55:00', 'Processing', 159.99),
                                                                       (19, '2025-08-20 15:30:00', 'Shipped', 129.98),
                                                                       (5, '2025-08-25 12:10:00', 'Delivered', 89.99),
                                                                       (10, '2025-09-01 14:45:00', 'Processing', 199.99),
                                                                       (15, '2025-09-10 11:25:00', 'Pending', 74.98),
                                                                       (20, '2025-09-15 16:05:00', 'Delivered', 549.97),
                                                                       (25, '2025-09-20 13:40:00', 'Shipped', 399.99);

-- ============================================
-- 4. ORDER_ITEMS TABLE (100 records)
-- ============================================
CREATE TABLE order_items (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(10,2) NOT NULL,
                             subtotal DECIMAL(10,2) NOT NULL,
                             FOREIGN KEY (order_id) REFERENCES orders(id),
                             FOREIGN KEY (product_id) REFERENCES products(id)
);

INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) VALUES
                                                                                   (1, 1, 1, 1299.99, 1299.99),
                                                                                   (2, 3, 1, 399.99, 399.99),
                                                                                   (3, 9, 2, 24.99, 49.99),
                                                                                   (3, 10, 1, 34.99, 34.99),
                                                                                   (3, 11, 5, 12.99, 64.95),
                                                                                   (4, 2, 1, 999.99, 999.99),
                                                                                   (5, 4, 1, 1499.99, 1499.99),
                                                                                   (5, 7, 1, 89.99, 89.99),
                                                                                   (5, 5, 1, 159.99, 159.99),
                                                                                   (6, 6, 1, 179.99, 179.99),
                                                                                   (7, 5, 1, 159.99, 159.99),
                                                                                   (8, 7, 1, 89.99, 89.99),
                                                                                   (9, 9, 10, 24.99, 249.99),
                                                                                   (9, 11, 10, 12.99, 129.99),
                                                                                   (9, 10, 5, 34.99, 174.99),
                                                                                   (10, 9, 2, 24.99, 49.99),
                                                                                   (10, 11, 1, 12.99, 12.99),
                                                                                   (10, 12, 1, 49.99, 49.99),
                                                                                   (11, 4, 1, 1499.99, 1499.99),
                                                                                   (12, 5, 1, 159.99, 159.99),
                                                                                   (12, 17, 1, 39.99, 39.99),
                                                                                   (13, 3, 1, 399.99, 399.99),
                                                                                   (14, 1, 1, 1299.99, 1299.99),
                                                                                   (14, 6, 1, 179.99, 179.99),
                                                                                   (14, 19, 1, 129.99, 129.99),
                                                                                   (14, 17, 1, 39.99, 39.99),
                                                                                   (15, 7, 1, 89.99, 89.99),
                                                                                   (16, 18, 1, 149.99, 149.99),
                                                                                   (16, 12, 1, 49.99, 49.99),
                                                                                   (17, 9, 2, 24.99, 49.99),
                                                                                   (17, 11, 1, 12.99, 12.99),
                                                                                   (17, 10, 1, 34.99, 34.99),
                                                                                   (18, 6, 1, 179.99, 179.99),
                                                                                   (19, 1, 2, 1299.99, 2599.98),
                                                                                   (19, 19, 1, 129.99, 129.99),
                                                                                   (20, 8, 1, 249.99, 249.99),
                                                                                   (21, 5, 1, 159.99, 159.99),
                                                                                   (22, 3, 1, 399.99, 399.99),
                                                                                   (23, 5, 1, 159.99, 159.99),
                                                                                   (23, 17, 1, 39.99, 39.99),
                                                                                   (23, 19, 1, 129.99, 129.99),
                                                                                   (24, 4, 1, 1499.99, 1499.99),
                                                                                   (25, 19, 1, 129.99, 129.99),
                                                                                   (26, 7, 1, 89.99, 89.99),
                                                                                   (27, 13, 2, 45.99, 91.98),
                                                                                   (27, 14, 2, 42.99, 85.98),
                                                                                   (27, 15, 3, 29.99, 89.97),
                                                                                   (27, 16, 3, 32.99, 98.97),
                                                                                   (28, 20, 1, 199.99, 199.99),
                                                                                   (29, 2, 1, 999.99, 999.99),
                                                                                   (30, 6, 1, 179.99, 179.99),
                                                                                   (31, 3, 1, 399.99, 399.99),
                                                                                   (32, 8, 1, 249.99, 249.99),
                                                                                   (33, 5, 1, 159.99, 159.99),
                                                                                   (34, 5, 1, 159.99, 159.99),
                                                                                   (35, 9, 2, 24.99, 49.99),
                                                                                   (35, 11, 2, 12.99, 25.99),
                                                                                   (35, 12, 1, 49.99, 49.99),
                                                                                   (36, 7, 1, 89.99, 89.99),
                                                                                   (37, 20, 1, 199.99, 199.99),
                                                                                   (38, 9, 2, 24.99, 49.99),
                                                                                   (38, 11, 1, 12.99, 12.99),
                                                                                   (38, 10, 1, 34.99, 34.99),
                                                                                   (39, 1, 1, 1299.99, 1299.99),
                                                                                   (39, 5, 1, 159.99, 159.99),
                                                                                   (39, 6, 1, 179.99, 179.99),
                                                                                   (40, 3, 1, 399.99, 399.99),
                                                                                   (1, 13, 1, 45.99, 45.99),
                                                                                   (2, 15, 1, 29.99, 29.99),
                                                                                   (3, 16, 1, 32.99, 32.99),
                                                                                   (4, 17, 1, 39.99, 39.99),
                                                                                   (5, 18, 1, 149.99, 149.99),
                                                                                   (6, 19, 1, 129.99, 129.99),
                                                                                   (7, 20, 1, 199.99, 199.99),
                                                                                   (8, 13, 1, 45.99, 45.99),
                                                                                   (9, 14, 1, 42.99, 42.99),
                                                                                   (10, 15, 1, 29.99, 29.99),
                                                                                   (11, 16, 1, 32.99, 32.99),
                                                                                   (12, 17, 2, 39.99, 79.98),
                                                                                   (13, 18, 1, 149.99, 149.99),
                                                                                   (14, 20, 1, 199.99, 199.99),
                                                                                   (15, 13, 1, 45.99, 45.99),
                                                                                   (16, 14, 1, 42.99, 42.99),
                                                                                   (17, 15, 2, 29.99, 59.98),
                                                                                   (18, 16, 1, 32.99, 32.99),
                                                                                   (19, 17, 1, 39.99, 39.99),
                                                                                   (20, 18, 1, 149.99, 149.99),
                                                                                   (21, 19, 2, 129.99, 259.98),
                                                                                   (22, 20, 1, 199.99, 199.99),
                                                                                   (23, 13, 1, 45.99, 45.99),
                                                                                   (24, 14, 1, 42.99, 42.99),
                                                                                   (25, 15, 1, 29.99, 29.99),
                                                                                   (26, 16, 1, 32.99, 32.99),
                                                                                   (27, 17, 1, 39.99, 39.99),
                                                                                   (28, 18, 1, 149.99, 149.99),
                                                                                   (29, 19, 1, 129.99, 129.99),
                                                                                   (30, 20, 1, 199.99, 199.99);

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check total counts
SELECT 'customers' AS table_name, COUNT(*) AS total_records FROM customers
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;

-- Check data distribution by city
SELECT city, COUNT(*) AS customer_count,
       ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM customers), 2) AS percentage
FROM customers
GROUP BY city
ORDER BY customer_count DESC;