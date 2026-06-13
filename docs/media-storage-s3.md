# S3 media storage

Cinema Memory keeps the public media URL contract as `/uploads/media/{fileName}`.
The API stores new upload bytes in S3 and stores only metadata in MySQL.

## Runtime environment variables

Required for ECS/Fargate production:

```text
AWS_REGION=ap-northeast-2
MEDIA_S3_BUCKET=<bucket-name>
MEDIA_S3_PREFIX=uploads/media
MEDIA_STORAGE_MODE=s3
MEDIA_MAX_UPLOAD_BYTES=52428800
```

Compatibility:

- `S3_BUCKET` is still accepted as a fallback when `MEDIA_S3_BUCKET` is absent.
- `MEDIA_STORAGE_MODE=local` can be used for local development without S3. Do not use it in production.
- AWS access keys must not be committed. On ECS/Fargate, prefer the task role and the AWS SDK default credential chain.

## S3 bucket checks

Create or select a private bucket in `AWS_REGION`.

Expected object path:

```text
s3://<MEDIA_S3_BUCKET>/<MEDIA_S3_PREFIX>/users/{userId}/{uuid}-{safe-file-name}
```

Example:

```text
s3://cinema-memory-prod/uploads/media/users/3/3dcc5cbe-c488-4bc7-850a-ab4e62c0cd52-photo.jpg
```

The bucket does not need public read access because the backend streams media through `/uploads/media/{fileName}`.

## ECS task role IAM policy

Attach permissions like this to the ECS task role, scoped to the selected bucket and prefix:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::<MEDIA_S3_BUCKET>/<MEDIA_S3_PREFIX>/*"
    },
    {
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::<MEDIA_S3_BUCKET>",
      "Condition": {
        "StringLike": {
          "s3:prefix": "<MEDIA_S3_PREFIX>/*"
        }
      }
    }
  ]
}
```

`DeleteObject` is included for future cleanup workflows. Current media deletion removes DB metadata.

## MySQL migration

Flyway migration:

```text
apps/api/src/main/resources/db/migration/V5__add_media_file_name.sql
```

It adds `media_assets.file_name`, backfills it from `cdn_url` or `s3_key`, marks it `NOT NULL`, and creates an index for `/uploads/media/{fileName}` lookup.

## Legacy media_data backfill

Rows that still have `media_assets.media_data` and a missing/local `s3_key` can be uploaded to S3 through the admin API:

```powershell
$env:API = "https://<api-host>"
$env:TOKEN = "<admin-jwt>"

curl.exe -X POST "$env:API/admin/media/migrate-s3?limit=100&clearLegacyData=false" `
  -H "Authorization: Bearer $env:TOKEN"
```

Run repeatedly until `scanned` is `0`.

After verifying S3 reads in production, run with `clearLegacyData=true` to clear migrated DB blobs. For rows that already have an S3 key, the API checks the S3 object before clearing `media_data`:

```powershell
curl.exe -X POST "$env:API/admin/media/migrate-s3?limit=100&clearLegacyData=true" `
  -H "Authorization: Bearer $env:TOKEN"
```

Run repeatedly until `scanned` is `0`. The response includes `uploaded`, `cleared`, `skipped`, and `failed` counts.

The legacy fallback remains available for rows whose `media_data` has not been cleared.

## Deployment verification

Upload a media file through the authenticated API:

```powershell
$env:API = "https://<api-host>"
$env:TOKEN = "<user-jwt>"
$env:SCENE_ID = "<scene-id>"

curl.exe -X POST "$env:API/media/upload?sceneId=$env:SCENE_ID" `
  -H "Authorization: Bearer $env:TOKEN" `
  -F "file=@C:\path\to\sample.jpg;type=image/jpeg"
```

Confirm MySQL metadata:

```sql
SELECT id, file_name, s3_key, cdn_url, content_type, byte_size, media_data IS NULL AS media_data_is_null
FROM media_assets
ORDER BY id DESC
LIMIT 5;
```

Fetch through the stable backend URL:

```powershell
curl.exe -I "$env:API/uploads/media/<file-name>"
curl.exe -L "$env:API/uploads/media/<file-name>" --output downloaded-media
```

Confirm the object exists in S3:

```powershell
aws s3 ls "s3://<MEDIA_S3_BUCKET>/<MEDIA_S3_PREFIX>/users/"
```
