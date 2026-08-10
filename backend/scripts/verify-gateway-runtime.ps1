param(
    [string] $GatewayBaseUrl = "http://127.0.0.1:8100",
    [string] $FrontendOrigin = "http://127.0.0.1:5173",
    [string] $AccessToken = $env:XUAN_GATEWAY_VERIFY_ACCESS_TOKEN,
    [string] $ForbiddenAccessToken = $env:XUAN_GATEWAY_VERIFY_FORBIDDEN_TOKEN,
    [int] $SentinelTransportPort = $(if ($env:XUAN_SENTINEL_PORT) { [int] $env:XUAN_SENTINEL_PORT } else { 8719 })
)

$ErrorActionPreference = "Stop"

function Invoke-GatewayRequest {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Method,

        [Parameter(Mandatory = $true)]
        [string] $Path,

        [hashtable] $Headers = @{},

        [string] $Body
    )

    $uri = "$GatewayBaseUrl$Path"
    $request = [System.Net.HttpWebRequest] [System.Net.WebRequest]::Create($uri)
    $request.Method = $Method
    $request.Accept = "application/json,text/plain,*/*"
    foreach ($headerName in $Headers.Keys) {
        if ($headerName -eq "Accept") {
            $request.Accept = $Headers[$headerName]
        } elseif ($headerName -eq "Content-Type") {
            $request.ContentType = $Headers[$headerName]
        } else {
            $request.Headers[$headerName] = $Headers[$headerName]
        }
    }
    if ($Body) {
        $request.ContentType = "application/json"
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
        $request.ContentLength = $bytes.Length
        $stream = $request.GetRequestStream()
        try {
            $stream.Write($bytes, 0, $bytes.Length)
        } finally {
            $stream.Dispose()
        }
    }

    try {
        $response = $request.GetResponse()
        $responseHeaders = [ordered] @{}
        foreach ($key in $response.Headers.AllKeys) {
            $responseHeaders[$key] = @($response.Headers.GetValues($key))
        }
        $statusCode = [int] $response.StatusCode

        $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
        try {
            $body = $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
            $response.Dispose()
        }

        return [pscustomobject] @{
            StatusCode = $statusCode
            Body = $body
            Headers = $responseHeaders
            Path = $Path
        }
    } catch {
        $exceptionResponse = $_.Exception.Response
        if ($exceptionResponse -eq $null -and $_.Exception.InnerException -ne $null) {
            $exceptionResponse = $_.Exception.InnerException.Response
        }
        if ($exceptionResponse -eq $null) {
            throw "请求 Gateway 失败：$Method $uri。请先确认 xuan-gateway 已经重启并监听该地址。原始错误：$($_.Exception.Message)"
        }

        $errorResponse = $exceptionResponse
        $errorHeaders = [ordered] @{}
        foreach ($key in $errorResponse.Headers.AllKeys) {
            $errorHeaders[$key] = @($errorResponse.Headers.GetValues($key))
        }

        $stream = $errorResponse.GetResponseStream()
        $body = ""
        if ($stream -ne $null) {
            $reader = New-Object System.IO.StreamReader($stream)
            try {
                $body = $reader.ReadToEnd()
            } finally {
                $reader.Dispose()
            }
        }

        return [pscustomobject] @{
            StatusCode = [int] $errorResponse.StatusCode
            Body = $body
            Headers = $errorHeaders
            Path = $Path
        }
    }
}

function Assert-Status {
    param(
        [pscustomobject] $Response,
        [int[]] $Expected,
        [string] $Message
    )

    if ($Response.StatusCode -notin $Expected) {
        throw "$Message，期望状态码 $($Expected -join '/')，实际 $($Response.StatusCode)，路径 $($Response.Path)，响应 $($Response.Body)"
    }
}

function Assert-BodyContains {
    param(
        [pscustomobject] $Response,
        [string] $Needle,
        [string] $Message
    )

    if ($Response.Body -notlike "*$Needle*") {
        throw "$Message，路径 $($Response.Path)，响应 $($Response.Body)"
    }
}

function Assert-HeaderContains {
    param(
        [pscustomobject] $Response,
        [string] $Name,
        [string] $Needle,
        [string] $Message
    )

    $actual = $Response.Headers[$Name]
    if (-not $actual -or (($actual -join ",") -notlike "*$Needle*")) {
        throw "$Message，响应头 $Name 实际值：$actual"
    }
}

function Assert-TcpPortListening {
    param(
        [int] $Port,
        [string] $Message
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connect = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
        if (-not $connect.AsyncWaitHandle.WaitOne(1000, $false)) {
            throw "$Message，127.0.0.1:$Port 未监听"
        }
        $client.EndConnect($connect)
    } finally {
        $client.Dispose()
    }
}

Write-Host "验证 Gateway 运行进程：$GatewayBaseUrl"

$health = Invoke-GatewayRequest -Method "GET" -Path "/actuator/health"
Assert-Status $health @(200) "健康检查未通过，可能进程未启动或端口不是最新 Gateway"
Assert-TcpPortListening $SentinelTransportPort "Sentinel transport 端口未启动，说明 Gateway Sentinel 配置可能没有被运行进程加载"

$publicPaths = @(
    "/actuator/info",
    "/v3/api-docs/iam",
    "/v3/api-docs/tenant",
    "/.well-known/jwks.json"
)

foreach ($path in $publicPaths) {
    $response = Invoke-GatewayRequest -Method "GET" -Path $path
    Assert-Status $response @(200, 503) "公开路径没有穿过认证放行；如果是 503 表示已放行但下游服务暂不可用"
}

$refresh = Invoke-GatewayRequest -Method "POST" -Path "/api/iam/auth/refresh"
Assert-Status $refresh @(200, 400, 401, 503) "refresh 入口没有进入公开放行链路"
if ($refresh.StatusCode -eq 401 -and $refresh.Body -like "*SECURITY_AUTHENTICATION_MISSING*") {
    throw "refresh 被 Gateway 当成未登录请求拦截，说明公开路径配置没有被运行进程加载"
}

$cors = Invoke-GatewayRequest `
    -Method "OPTIONS" `
    -Path "/api/iam/auth/login" `
    -Headers @{
        Origin = $FrontendOrigin
        "Access-Control-Request-Method" = "POST"
        "Access-Control-Request-Headers" = "content-type,authorization"
    }
Assert-Status $cors @(200, 204) "CORS 预检失败"
Assert-HeaderContains $cors "Access-Control-Allow-Origin" $FrontendOrigin "CORS 没有放行当前前端 Origin"

$unauthorized = Invoke-GatewayRequest -Method "GET" -Path "/api/tenant-plans"
Assert-Status $unauthorized @(401) "受保护 Tenant 接口未携带 token 时没有返回 401"
Assert-BodyContains $unauthorized "SECURITY_AUTHENTICATION_MISSING" "401 响应体不是统一 JSON"
Assert-HeaderContains $unauthorized "X-Trace-Id" "xuan-" "Gateway 没有生成或透传 TraceId"

$tenantRoutePaths = @(
    "/api/tenants",
    "/api/tenant-configs",
    "/api/tenant-domains",
    "/api/tenant-contacts",
    "/api/tenant-resources/tenant-configs",
    "/api/tenant-plans",
    "/api/tenant-plan-assignments",
    "/api/tenant-provision-tasks/1/retry",
    "/api/tenant-outbox-events/1/retry"
)

foreach ($path in $tenantRoutePaths) {
    $response = Invoke-GatewayRequest -Method "GET" -Path $path
    if ($response.StatusCode -eq 404) {
        throw "Tenant 路由未被当前 Gateway 进程加载，路径返回 404：$path"
    }
    Assert-Status $response @(401, 405, 503) "Tenant 路由检查出现非预期状态"
}

$auditRoute = Invoke-GatewayRequest -Method "GET" -Path "/api/audit/logs"
if ($auditRoute.StatusCode -eq 404) {
    throw "Audit 路由未被当前 Gateway 进程加载，路径返回 404：/api/audit/logs"
}
Assert-Status $auditRoute @(401, 405, 503) "Audit 路由检查出现非预期状态"

$spoofed = Invoke-GatewayRequest `
    -Method "GET" `
    -Path "/api/tenant-plans" `
    -Headers @{
        "X-User-Id" = "999"
        "X-Tenant-Id" = "888"
        "X-Username" = "fake-admin"
        "X-Roles" = "super_admin"
        "X-Permissions" = "*"
    }
Assert-Status $spoofed @(401) "伪造身份 Header 的匿名请求没有被 Gateway 拒绝"
Assert-BodyContains $spoofed "SECURITY_AUTHENTICATION_MISSING" "伪造身份 Header 可能影响了 Gateway 认证判断"

if ($AccessToken) {
    $authenticated = Invoke-GatewayRequest -Method "GET" -Path "/api/tenant-plans" -Headers @{ Authorization = "Bearer $AccessToken" }
    Assert-Status $authenticated @(200, 403, 503) "携带真实 token 访问 Tenant 接口没有进入已认证链路"
    if ($authenticated.StatusCode -eq 200) {
        Write-Host "已验证真实 token 的 Tenant 受保护接口 200。"
    }
}

if ($ForbiddenAccessToken) {
    $forbidden = Invoke-GatewayRequest -Method "GET" -Path "/api/tenant-plans" -Headers @{ Authorization = "Bearer $ForbiddenAccessToken" }
    Assert-Status $forbidden @(403) "低权限 token 没有返回 403"
    Assert-BodyContains $forbidden "SECURITY_PERMISSION_DENIED" "403 响应体不是统一 JSON"
}

Write-Host "OK: Gateway 重启后的路由、JWKS、Sentinel、审计、CORS、公开路径、TraceId、身份 Header 覆盖和 401/403 运行态检查通过。"
