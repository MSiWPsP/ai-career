#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

cd "${PROJECT_ROOT}"
check_docker
validate_environment

TARGET_VERSION="${1:-$(previous_stable_version 2>/dev/null || true)}"
[[ -n "${TARGET_VERSION}" ]] \
  || fail "没有记录上一稳定版本，请把目标镜像标签作为第一个参数传入"
docker image inspect "ai-career-backend:${TARGET_VERSION}" >/dev/null 2>&1 \
  || fail "本机不存在后端镜像 ai-career-backend:${TARGET_VERSION}"
docker image inspect "ai-career-web:${TARGET_VERSION}" >/dev/null 2>&1 \
  || fail "本机不存在前端镜像 ai-career-web:${TARGET_VERSION}"

export APP_VERSION="${TARGET_VERSION}"
validate_compose

compose up -d mysql
wait_for_health mysql 240
compose up -d pgvector
wait_for_health pgvector 180
log "切回后端镜像 ${TARGET_VERSION}"
compose up -d --no-deps backend
wait_for_health backend 300
log "切回 Web 镜像 ${TARGET_VERSION}"
compose up -d --no-deps web
wait_for_health web 120

record_stable_version
compose ps
log "镜像回滚完成。MySQL/Flyway 迁移和 pgvector 数据卷未回滚、未删除。"
