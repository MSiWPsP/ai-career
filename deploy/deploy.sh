#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

IMPORT_RAG=false
PREBUILT=false
for argument in "$@"; do
  case "${argument}" in
    --import-rag) IMPORT_RAG=true ;;
    --prebuilt) PREBUILT=true ;;
    *) fail "未知参数：${argument}（支持 --import-rag、--prebuilt）" ;;
  esac
done

trap 'printf "[ai-career] 部署失败，请查看上方错误；未执行卷删除或系统清理。\n" >&2' ERR

cd "${PROJECT_ROOT}"
check_docker
validate_environment
check_host_resources

if [[ "${PREBUILT}" == "true" ]]; then
  export APP_VERSION="$(env_value APP_VERSION)"
  [[ -n "${APP_VERSION}" ]] || fail "--prebuilt 模式要求 .env 配置 APP_VERSION"
else
  export APP_VERSION="$(resolve_build_version)"
fi
validate_compose

if [[ "${PREBUILT}" == "true" ]]; then
  docker image inspect "ai-career-backend:${APP_VERSION}" >/dev/null 2>&1 \
    || fail "缺少预构建镜像 ai-career-backend:${APP_VERSION}"
  docker image inspect "ai-career-web:${APP_VERSION}" >/dev/null 2>&1 \
    || fail "缺少预构建镜像 ai-career-web:${APP_VERSION}"
  log "使用本机已有的预构建镜像 ${APP_VERSION}"
else
  log "顺序构建后端镜像（是否执行测试由 DOCKER_RUN_TESTS 控制，并始终执行 JAR 安全检查）"
  compose build backend
  log "顺序构建前端镜像"
  compose build web
fi
verify_runtime_images

log "启动 MySQL"
compose up -d mysql
wait_for_health mysql 240

log "启动 pgvector"
compose up -d pgvector
wait_for_health pgvector 180

if [[ "${IMPORT_RAG}" == "true" ]]; then
  log "执行一次性 RAG 导入；失败将立即终止部署"
  compose --profile tools run --rm rag-import
else
  log "未传入 --import-rag，跳过 Embedding 与知识导入"
fi

log "启动后端"
compose up -d --no-deps backend
wait_for_health backend 300

log "启动 Web/Nginx"
compose up -d --no-deps web
wait_for_health web 120

record_stable_version
compose ps
WEB_PORT_VALUE="$(env_value WEB_PORT)"
log "部署完成，访问地址：http://服务器IP:${WEB_PORT_VALUE:-80}"
log "当前镜像版本：${APP_VERSION}"
