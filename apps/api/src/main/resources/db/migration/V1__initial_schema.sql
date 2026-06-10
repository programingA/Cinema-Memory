CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    display_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE oauth_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE films (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    cover_image_url VARCHAR(700),
    mood VARCHAR(80),
    visibility VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_films_user_created (user_id, created_at),
    CONSTRAINT fk_films_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE memory_scenes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    film_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    memory_date DATE,
    location VARCHAR(255),
    mood VARCHAR(80),
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_scenes_film_order (film_id, sort_order),
    CONSTRAINT fk_scenes_film FOREIGN KEY (film_id) REFERENCES films (id)
);

CREATE TABLE media_assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scene_id BIGINT,
    user_id BIGINT NOT NULL,
    s3_key VARCHAR(700) NOT NULL,
    cdn_url VARCHAR(700) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    byte_size BIGINT NOT NULL,
    thumbnail_url VARCHAR(700),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_media_scene (scene_id),
    CONSTRAINT fk_media_scene FOREIGN KEY (scene_id) REFERENCES memory_scenes (id),
    CONSTRAINT fk_media_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tags_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE film_tags (
    film_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (film_id, tag_id),
    CONSTRAINT fk_film_tags_film FOREIGN KEY (film_id) REFERENCES films (id),
    CONSTRAINT fk_film_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);
