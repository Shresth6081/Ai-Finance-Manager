-- Remove duplicate categories, keeping the row with the lowest id
-- for each unique (name, user_id, type) combination.
DELETE FROM categories
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id
        FROM categories
        GROUP BY name, user_id, type
    ) AS keeper
);
