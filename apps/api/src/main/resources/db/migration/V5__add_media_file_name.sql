ALTER TABLE media_assets
    ADD COLUMN file_name VARCHAR(255);

UPDATE media_assets
SET file_name = LEFT(CASE
    WHEN cdn_url IS NOT NULL AND cdn_url <> '' THEN SUBSTRING_INDEX(cdn_url, '/', -1)
    WHEN s3_key IS NOT NULL AND s3_key <> '' THEN SUBSTRING_INDEX(s3_key, '/', -1)
    ELSE CONCAT('media-', id)
END, 255);

ALTER TABLE media_assets
    MODIFY COLUMN file_name VARCHAR(255) NOT NULL;

CREATE INDEX idx_media_assets_file_name ON media_assets (file_name);
