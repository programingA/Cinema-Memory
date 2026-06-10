ALTER TABLE users
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'USER';

CREATE INDEX idx_users_role ON users (role);
