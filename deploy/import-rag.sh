#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

trap 'printf "[ai-career] RAG 导入失败，请查看上方错误。\n" >&2' ERR

cd "${PROJECT_ROOT}"
check_docker
validate_environment
check_host_resources

export APP_VERSION="$(stable_version 2>/dev/null || true)"
[[ -n "${APP_VERSION}" ]] || fail "尚未记录稳定镜像，请先完成首次部署"
docker image inspect "ai-career-backend:${APP_VERSION}" >/dev/null 2>&1 \
  || fail "本机不存在稳定后端镜像 ai-career-backend:${APP_VERSION}"

validate_compose
compose up -d mysql
wait_for_health mysql 240
compose up -d pgvector
wait_for_health pgvector 180

log "使用稳定后端镜像 ${APP_VERSION} 执行一次性 RAG 导入"
compose --profile tools run --rm rag-import
log "RAG 导入完成；Web 服务未重启"
