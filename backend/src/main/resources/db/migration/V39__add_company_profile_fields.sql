ALTER TABLE tenants ADD COLUMN trade_name VARCHAR(180);
ALTER TABLE tenants ADD COLUMN legal_name VARCHAR(180);
ALTER TABLE tenants ADD COLUMN tax_id VARCHAR(40);
ALTER TABLE tenants ADD COLUMN corporate_email VARCHAR(180);
ALTER TABLE tenants ADD COLUMN phone VARCHAR(40);
ALTER TABLE tenants ADD COLUMN website VARCHAR(240);

UPDATE tenants
SET trade_name = name
WHERE trade_name IS NULL;
