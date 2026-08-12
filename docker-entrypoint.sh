#!/bin/sh
set -eu

# Named Docker volumes are commonly created as root. Repair the dedicated
# upload directory before dropping privileges so chat and ticket attachments
# remain writable across deployments and volume reuse.
mkdir -p /app/uploads
chown -R appuser:appgroup /app/uploads

exec su-exec appuser:appgroup java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar
