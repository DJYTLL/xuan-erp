#!/usr/bin/env sh
set -eu

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <module> [service-name] [oap-grpc]" >&2
  exit 2
fi

MODULE="$1"
SERVICE_NAME="${2:-$MODULE}"
OAP_GRPC="${3:-duaoyunxuan.top:9050}"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKEND_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
AGENT_PATH="./skywalking-agent/skywalking-agent.jar"
JVM_ARGUMENTS="-javaagent:$AGENT_PATH -Dskywalking.agent.service_name=$SERVICE_NAME -Dskywalking.collector.backend_service=$OAP_GRPC"

cd "$BACKEND_DIR"
exec mvn -pl "$MODULE" spring-boot:run "-Dspring-boot.run.jvmArguments=$JVM_ARGUMENTS"
