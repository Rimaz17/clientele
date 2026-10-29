-- Every existing account becomes a regular user; admins are promoted explicitly.
ALTER TABLE customer ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE customer ADD CONSTRAINT customer_role_check CHECK (role IN ('USER', 'ADMIN'));
