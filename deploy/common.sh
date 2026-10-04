#!/usr/bin/env bash

set -Eeuo pipefail

DEPLOY_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${DEPLOY_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"
STATE_DIR="${PROJECT_ROOT}/.deploy"

log() {
  printf '[ai-career] %s\n' "$*"
}

fail() {
  printf '[ai-career] 错误：%s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "缺少命令：$1"
}

env_value() {
  local key="$1"
  local value
  value="$(sed -n "s/^${key}=//p" "${ENV_FILE}" | tail -n 1 | tr -d '\r')"
  if [[ "${value}" == \"*\" && "${value}" == *\" ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "${value}" == \'*\' && "${value}" == *\' ]]; then
    value="${value:1:${#value}-2}"
  fi
  printf '%s' "${value}"
}

validate_environment() {
  [[ -f "${ENV_FILE}" ]] || fail "未找到 ${ENV_FILE}，请先复制 .env.production.example 为 .env"
  local required=(
    SPRING_PROFILES_ACTIVE
    MYSQL_DATABASE MYSQL_ROOT_PASSWORD DB_URL DB_USERNAME DB_PASSWORD
    POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
    RAG_PG_URL RAG_PG_USERNAME RAG_PG_PASSWORD RAG_KNOWLEDGE_DIR
    AI_BASE_URL AI_API_KEY AI_MODEL AI_TIMEOUT AI_MAX_RETRIES
    RAG_EMBEDDING_BASE_URL RAG_EMBEDDING_API_KEY RAG_EMBEDDING_MODEL
    JWT_SECRET JWT_EXPIRATION_MILLIS
    AI_RAG_ENABLED AI_RAG_IMPORT AI_CAREER_TOOLS_ENABLED AI_TOOL_MAX_CALLS_PER_TOOL
    OSS_ACCESS_KEY_ID OSS_ACCESS_KEY_SECRET OSS_ENDPOINT OSS_BUCKET_NAME
  )
  local key value
  for key in "${required[@]}"; do
    value="$(env_value "${key}")"
    [[ -n "${value}" ]] || fail ".env 中 ${key} 不能为空"
    [[ "${value}" != *"__REQUIRED__"* && "${value}" != *"CHANGE_ME"* ]] \
      || fail ".env 中 ${key} 仍是占位符"
  done
  [[ "$(env_value SPRING_PROFILES_ACTIVE)" == "prod" ]] \
    || fail "SPRING_PROFILES_ACTIVE 必须为 prod"
  [[ "$(env_value MYSQL_DATABASE)" == "ai_career" ]] \
    || fail "当前 Compose 固定使用 ai_career，请设置 MYSQL_DATABASE=ai_career"
  [[ "$(env_value DB_URL)" == "jdbc:mysql://mysql:3306/ai_career?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true" ]] \
    || fail "DB_URL 必须指向 Compose 内的 mysql 服务"
  [[ "$(env_value DB_USERNAME)" != "root" ]] \
    || fail "后端不能使用 MySQL root 账户，请配置独立 DB_USERNAME"
  [[ "$(env_value POSTGRES_DB)" == "ai_career_rag" ]] \
    || fail "当前 Compose 固定使用 ai_career_rag，请设置 POSTGRES_DB=ai_career_rag"
  [[ "$(env_value POSTGRES_USER)" == "$(env_value RAG_PG_USERNAME)" ]] \
    || fail "容器内 RAG 用户必须与 POSTGRES_USER 一致"
  [[ "$(env_value POSTGRES_PASSWORD)" == "$(env_value RAG_PG_PASSWORD)" ]] \
    || fail "RAG_PG_PASSWORD 必须与 POSTGRES_PASSWORD 一致"
  [[ "$(env_value RAG_PG_URL)" == "jdbc:postgresql://pgvector:5432/ai_career_rag" ]] \
    || fail "RAG_PG_URL 必须指向 Compose 内的 pgvector 服务"
  local jwt_secret mysql_root_password
  jwt_secret="$(env_value JWT_SECRET)"
  mysql_root_password="$(env_value MYSQL_ROOT_PASSWORD)"
  (( ${#jwt_secret} >= 32 )) || fail "JWT_SECRET 至少需要 32 个字符"
  (( ${#mysql_root_password} >= 16 )) || fail "MYSQL_ROOT_PASSWORD 至少需要 16 个字符"
}

check_host_resources() {
  if [[ "${SKIP_RESOURCE_CHECK:-0}" == "1" ]]; then
    log "已跳过主机资源检查（仅建议本地验证使用）"
    return
  fi
  [[ -r /proc/meminfo ]] || fail "部署脚本仅支持可读取 /proc/meminfo 的 Linux 主机"
  local total_kb available_kb swap_kb
  total_kb="$(awk '/^MemTotal:/ {print $2}' /proc/meminfo)"
  available_kb="$(awk '/^MemAvailable:/ {print $2}' /proc/meminfo)"
  swap_kb="$(awk '/^SwapTotal:/ {print $2}' /proc/meminfo)"
  (( total_kb >= 1740800 )) || fail "物理内存不足 1.7 GiB；生产部署要求 2 GB 主机"
  (( available_kb >= 524288 )) || fail "当前可用内存不足 512 MiB，请释放内存后重试"
  (( swap_kb >= 1740800 )) || fail "Swap 不足 1.7 GiB；请先配置至少 2 GB Swap"
}

check_docker() {
  require_command docker
  docker info >/dev/null 2>&1 || fail "Docker Engine 未运行或当前用户无权限访问"
  docker compose version >/dev/null 2>&1 || fail "未安装 Docker Compose Plugin"
}

compose() {
  docker compose --project-directory "${PROJECT_ROOT}" --env-file "${ENV_FILE}" "$@"
}

validate_compose() {
  log "校验 Compose 配置"
  compose config --quiet
}

wait_for_health() {
  local service="$1"
  local timeout_seconds="${2:-240}"
  local container_id status elapsed=0
  container_id="$(compose ps -q "${service}")"
  [[ -n "${container_id}" ]] || fail "服务 ${service} 没有创建容器"
  while (( elapsed < timeout_seconds )); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
    case "${status}" in
      healthy)
        log "${service} 已健康"
        return 0
        ;;
      unhealthy|exited|dead)
        compose logs --tail=120 "${service}" >&2
        fail "${service} 状态为 ${status}"
        ;;
    esac
    sleep 5
    elapsed=$((elapsed + 5))
  done
  compose logs --tail=120 "${service}" >&2
  fail "等待 ${service} 健康超时（${timeout_seconds} 秒）"
}

resolve_build_version() {
  local revision timestamp
  revision="$(git -C "${PROJECT_ROOT}" rev-parse --short=12 HEAD 2>/dev/null || printf 'source')"
  timestamp="$(date -u +%Y%m%d%H%M%S)"
  printf '%s-%s' "${revision}" "${timestamp}"
}

verify_runtime_images() {
  local backend_image="ai-career-backend:${APP_VERSION}"
  local web_image="ai-career-web:${APP_VERSION}"
  log "检查运行镜像未包含本地配置、构建目录或源码依赖"
  docker run --rm --entrypoint sh "${backend_image}" -c \
    'test -z "$(find /app -xdev \( -name application-local.yml -o -name .env -o -name .git -o -name target -o -name node_modules -o -name dist \) -print -quit 2>/dev/null)"'
  docker run --rm --entrypoint sh "${web_image}" -c \
    'test -z "$(find /usr/share/nginx/html -xdev \( -name .env -o -name .git -o -name node_modules -o -name src \) -print -quit 2>/dev/null)"'
  # 以镜像默认非 root 用户检查配置可读性与语法，在替换线上 Web 前发现权限问题。
  docker run --rm --entrypoint nginx --add-host backend:127.0.0.1 "${web_image}" -t
}

record_stable_version() {
  mkdir -p "${STATE_DIR}"
  if [[ -f "${STATE_DIR}/current-version" ]]; then
    local current_version
    current_version="$(tr -d '\r\n' < "${STATE_DIR}/current-version")"
    if [[ -n "${current_version}" && "${current_version}" != "${APP_VERSION}" ]]; then
      printf '%s\n' "${current_version}" > "${STATE_DIR}/previous-version"
    fi
  fi
  printf '%s\n' "${APP_VERSION}" > "${STATE_DIR}/current-version"
}

stable_version() {
  [[ -f "${STATE_DIR}/current-version" ]] || return 1
  tr -d '\r\n' < "${STATE_DIR}/current-version"
}

previous_stable_version() {
  [[ -f "${STATE_DIR}/previous-version" ]] || return 1
  tr -d '\r\n' < "${STATE_DIR}/previous-version"
}
