#!/usr/bin/env bash
# Uruchamia backend z sekretami z local.env (patrz local.env.example).
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f local.env ]; then
  echo "Brak local.env — skopiuj local.env.example do local.env i uzupełnij dane." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source local.env
set +a

if [ -z "${APP_MAIL_ENCRYPTION_KEY:-}" ]; then
  echo "Brak APP_MAIL_ENCRYPTION_KEY w local.env. Wygeneruj: openssl rand -base64 32" >&2
  exit 1
fi

# Profil dev włącza CORS dla ng serve na :4200 — bez niego frontend nie dogada się z API.
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

MVN="${MAVEN_BIN:-mvn}"
if ! command -v "$MVN" >/dev/null 2>&1; then
  MVN="$HOME/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn"
fi

exec "$MVN" spring-boot:run
