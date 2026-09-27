#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

PULL_CODE=false
case "${1:-}" in
  "") ;;
  --pull) PULL_CODE=true ;;
  *) fail "未知参数：$1（仅支持 --pull）" ;;
esac

PREVIOUS_VERSION="$(stable_version 2>/dev/null || true)"
on_error() {
  printf '[ai-career] 更新失败；MySQL 与 pgvector 数据卷均未被删除。\n' >&2
  if [[ -n "${PREVIOUS_VERSION}" ]]; then
    printf '[ai-career] 可执行 bash deploy/rollback.sh %s 回到更新前的稳定镜像。\n' "${PREVIOUS_VERSION}" >&2
  fi
}
trap on_error ERR

cd "${PROJECT_ROOT}"
check_docker
validate_environment
check_host_resources

if [[ "${PULL_CODE}" == "true" ]]; then
  log "使用 git pull --ff-only 获取代码"
  git pull --ff-only
else
  log "使用当前工作区代码；如需拉取远端请运行 bash deploy/update.sh --pull"
fi

export APP_VERSION="$(resolve_build_version)"
validate_compose

log "顺序构建新的后端镜像"
compose build backend
log "顺序构建新的前端镜像"
compose build web
verify_runtime_images

compose up -d mysql
wait_for_health mysql 240
compose up -d pgvector
wait_for_health pgvector 180

log "滚动替换后端（普通更新不会执行 RAG 导入）"
compose up -d --no-deps backend
wait_for_health backend 300

log "替换 Web/Nginx"
compose up -d --no-deps web
wait_for_health web 120

record_stable_version
compose ps
log "更新完成，稳定镜像版本：${APP_VERSION}"
[[ -z "${PREVIOUS_VERSION}" ]] || log "旧镜像 ${PREVIOUS_VERSION} 已保留，可显式回滚"
