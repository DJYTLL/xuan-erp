param(
    [Parameter(Mandatory = $true)]
    [string] $Module,

    [string] $ServiceName = $Module,

    [string] $OapGrpc = "duaoyunxuan.top:9050"
)

$ErrorActionPreference = "Stop"

$BackendDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$AgentPath = "./skywalking-agent/skywalking-agent.jar"
$JvmArguments = "-javaagent:$AgentPath -Dskywalking.agent.service_name=$ServiceName -Dskywalking.collector.backend_service=$OapGrpc"

Push-Location $BackendDir
try {
    & mvn -pl $Module spring-boot:run "-Dspring-boot.run.jvmArguments=$JvmArguments"
} finally {
    Pop-Location
}
