-- SQL script to remove duplicate categories
-- Run this in your MySQL database

-- First, let's see the duplicates
SELECT name, type, COUNT(*) as count, user_id
FROM categories
GROUP BY name, type, user_id
HAVING count > 1;

-- Delete duplicates, keeping only the one with the lowest ID
DELETE c1 FROM categories c1
INNER JOIN categories c2 
WHERE c1.id > c2.id 
  AND c1.name = c2.name 
  AND c1.type = c2.type 
  AND c1.user_id = c2.user_id;

-- Verify no duplicates remain
SELECT name, type, COUNT(*) as count, user_id
FROM categories
GROUP BY name, type, user_id
HAVING count > 1;
