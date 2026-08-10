param(
    [string] $GatewayBaseUrl = "http://127.0.0.1:8100",
    [string] $FrontendOrigin = "http://127.0.0.1:5173",
    [string] $AccessToken = $env:XUAN_GATEWAY_VERIFY_ACCESS_TOKEN,
    [string] $TenantCode = $env:XUAN_GATEWAY_VERIFY_TENANT_CODE,
    [string] $Username = $env:XUAN_GATEWAY_VERIFY_USERNAME,
    [string] $Password = $env:XUAN_GATEWAY_VERIFY_PASSWORD,
    [switch] $RequireTenantColumnRules
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
            throw "请求 Gateway 失败：$Method $uri。请确认 Gateway 已启动。原始错误：$($_.Exception.Message)"
        }

        $errorHeaders = [ordered] @{}
        foreach ($key in $exceptionResponse.Headers.AllKeys) {
            $errorHeaders[$key] = @($exceptionResponse.Headers.GetValues($key))
        }

        $stream = $exceptionResponse.GetResponseStream()
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
            StatusCode = [int] $exceptionResponse.StatusCode
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

function Convert-ResponseJson {
    param(
        [pscustomobject] $Response,
        [string] $Message
    )

    try {
        return $Response.Body | ConvertFrom-Json
    } catch {
        throw "$Message，响应不是合法 JSON，路径 $($Response.Path)，响应 $($Response.Body)"
    }
}

function Unwrap-ApiData {
    param([object] $Json)

    if ($Json.PSObject.Properties.Name -contains "data") {
        return $Json.data
    }
    return $Json
}

function Assert-JsonProperty {
    param(
        [object] $Json,
        [string] $Name,
        [string] $Message
    )

    if ($Json -eq $null -or -not ($Json.PSObject.Properties.Name -contains $Name)) {
        throw $Message
    }
}

function Assert-ArrayProperty {
    param(
        [object] $Json,
        [string] $Name,
        [string] $Message
    )

    Assert-JsonProperty $Json $Name $Message
    if ($Json.$Name -eq $null -or -not ($Json.$Name -is [System.Array])) {
        throw $Message
    }
}

function Assert-TraceHeader {
    param([pscustomobject] $Response)

    $traceId = $Response.Headers["X-Trace-Id"]
    if (-not $traceId -or (($traceId -join ",") -notlike "xuan-*")) {
        throw "Gateway 没有为 $($Response.Path) 生成或透传 X-Trace-Id，实际值：$traceId"
    }
}

function AuthorizationHeader {
    param([string] $Token)

    return @{
        Authorization = "Bearer $Token"
        Origin = $FrontendOrigin
    }
}

function Login-ThroughGateway {
    if (-not $TenantCode -or -not $Username -or -not $Password) {
        throw "真实链路验证需要 access token。请设置 XUAN_GATEWAY_VERIFY_ACCESS_TOKEN，或设置 XUAN_GATEWAY_VERIFY_TENANT_CODE / XUAN_GATEWAY_VERIFY_USERNAME / XUAN_GATEWAY_VERIFY_PASSWORD 让脚本先通过 Gateway 登录。"
    }

    $body = @{
        tenantCode = $TenantCode
        username = $Username
        password = $Password
    } | ConvertTo-Json -Compress
    $response = Invoke-GatewayRequest `
        -Method "POST" `
        -Path "/api/iam/auth/login" `
        -Headers @{ Origin = $FrontendOrigin } `
        -Body $body
    Assert-Status $response @(200) "前端 Origin 通过 Gateway 登录 IAM 失败"
    Assert-TraceHeader $response

    $json = Convert-ResponseJson $response "登录响应解析失败"
    $data = Unwrap-ApiData $json
    Assert-JsonProperty $data "accessToken" "登录响应缺少 accessToken"
    Assert-JsonProperty $data "refreshToken" "登录响应缺少 refreshToken"
    Assert-JsonProperty $data "currentUser" "登录响应缺少 currentUser"
    return $data
}

function Refresh-ThroughGateway {
    param([string] $RefreshToken)

    $body = @{
        refreshToken = $RefreshToken
    } | ConvertTo-Json -Compress
    $response = Invoke-GatewayRequest `
        -Method "POST" `
        -Path "/api/iam/auth/refresh" `
        -Headers @{ Origin = $FrontendOrigin } `
        -Body $body
    Assert-Status $response @(200) "前端 Origin 通过 Gateway refresh IAM token 失败"
    Assert-TraceHeader $response

    $json = Convert-ResponseJson $response "refresh 响应解析失败"
    $data = Unwrap-ApiData $json
    Assert-JsonProperty $data "accessToken" "refresh 响应缺少 accessToken"
    Assert-JsonProperty $data "refreshToken" "refresh 响应缺少 refreshToken"
    return $data
}

function Assert-ColumnPermissionApplied {
    param(
        [object] $Snapshot,
        [object] $TenantPage
    )

    Assert-JsonProperty $Snapshot "columnPermissions" "权限快照缺少 columnPermissions"
    Assert-JsonProperty $TenantPage "records" "Tenant 列权限接口响应缺少 records"
    if (-not ($TenantPage.records -is [System.Array])) {
        throw "Tenant 列权限接口 records 不是数组"
    }

    $tenantRules = $Snapshot.columnPermissions.tenant
    if ($tenantRules -eq $null) {
        if ($RequireTenantColumnRules) {
            throw "当前权限快照没有 tenant 列权限规则，无法证明裁剪生效"
        }
        Write-Host "WARN: 当前权限快照没有 tenant 列权限规则；已验证列权限裁剪接口真实链路 200，但本次无法证明隐藏/脱敏效果。"
        return
    }

    $ruleNames = @($tenantRules.PSObject.Properties.Name)
    $restrictedRules = @($tenantRules.PSObject.Properties | Where-Object { $_.Value -eq "HIDDEN" -or $_.Value -eq "MASKED" })
    if ($restrictedRules.Count -eq 0) {
        if ($RequireTenantColumnRules) {
            throw "当前 tenant 列权限规则没有 HIDDEN/MASKED，无法证明裁剪效果。规则字段：$($ruleNames -join ',')"
        }
        Write-Host "WARN: 当前 tenant 列权限规则没有 HIDDEN/MASKED；已验证列权限裁剪接口真实链路 200。"
        return
    }

    if ($TenantPage.records.Count -eq 0) {
        throw "Tenant 列权限接口 records 为空，无法验证裁剪后的字段值"
    }

    $first = $TenantPage.records[0]
    foreach ($rule in $restrictedRules) {
        $field = $rule.Name
        if (-not ($first.PSObject.Properties.Name -contains $field)) {
            continue
        }
        $value = $first.$field
        if ($rule.Value -eq "HIDDEN" -and $value -ne $null) {
            throw "Tenant 列权限 HIDDEN 未生效，字段 $field 仍返回值：$value"
        }
        if ($rule.Value -eq "MASKED" -and $value -is [string] -and $value -notlike "*`**") {
            throw "Tenant 列权限 MASKED 疑似未生效，字段 $field 返回值未脱敏：$value"
        }
    }
}

if (-not $AccessToken) {
    $loginResult = Login-ThroughGateway
    $refreshResult = Refresh-ThroughGateway $loginResult.refreshToken
    $AccessToken = $refreshResult.accessToken
}

Write-Host "验证前端 Origin -> Gateway -> IAM 登录 / refresh / 当前用户 / 当前菜单 / 当前权限快照 / Tenant 列权限裁剪接口：$GatewayBaseUrl"

$authHeaders = AuthorizationHeader $AccessToken

$currentUserResponse = Invoke-GatewayRequest -Method "GET" -Path "/api/iam/auth/current-user" -Headers $authHeaders
Assert-Status $currentUserResponse @(200) "前端通过 Gateway 查询 IAM 当前用户失败"
Assert-TraceHeader $currentUserResponse
$currentUser = Unwrap-ApiData (Convert-ResponseJson $currentUserResponse "当前用户响应解析失败")
Assert-JsonProperty $currentUser "userId" "当前用户响应缺少 userId"
Assert-JsonProperty $currentUser "username" "当前用户响应缺少 username"
Assert-ArrayProperty $currentUser "permissions" "当前用户响应缺少 permissions 数组"

$menusResponse = Invoke-GatewayRequest -Method "GET" -Path "/api/iam/menus/current" -Headers $authHeaders
Assert-Status $menusResponse @(200) "前端通过 Gateway 查询 IAM 当前菜单失败"
Assert-TraceHeader $menusResponse
$menus = Unwrap-ApiData (Convert-ResponseJson $menusResponse "当前菜单响应解析失败")
if ($menus -eq $null -or -not ($menus -is [System.Array])) {
    throw "当前菜单响应 data 不是数组"
}
if ($menus.Count -gt 0) {
    Assert-JsonProperty $menus[0] "code" "当前菜单节点缺少 code"
    Assert-JsonProperty $menus[0] "title" "当前菜单节点缺少 title"
    Assert-JsonProperty $menus[0] "children" "当前菜单节点缺少 children"
}

$snapshotResponse = Invoke-GatewayRequest -Method "GET" -Path "/api/iam/permissions/current" -Headers $authHeaders
Assert-Status $snapshotResponse @(200) "前端通过 Gateway 查询 IAM 当前权限快照失败"
Assert-TraceHeader $snapshotResponse
$snapshot = Unwrap-ApiData (Convert-ResponseJson $snapshotResponse "当前权限快照响应解析失败")
Assert-ArrayProperty $snapshot "menus" "权限快照缺少 menus 数组"
Assert-ArrayProperty $snapshot "routePermissions" "权限快照缺少 routePermissions 数组"
Assert-ArrayProperty $snapshot "buttonPermissions" "权限快照缺少 buttonPermissions 数组"
Assert-JsonProperty $snapshot "columnPermissions" "权限快照缺少 columnPermissions"
Assert-JsonProperty $snapshot "authVersion" "权限快照缺少 authVersion"

$tenantOptionsResponse = Invoke-GatewayRequest -Method "GET" -Path "/api/tenants/column-permission-options?pageNum=1&pageSize=20" -Headers $authHeaders
if ($tenantOptionsResponse.StatusCode -eq 403) {
    throw "前端通过 Gateway 查询 Tenant 列权限裁剪接口返回 403。真实链路已到达 Tenant 权限边界，但当前 token 缺少 tenant:view / iam-column-permission:view / iam-role-column-permission:view 之一，路径 $($tenantOptionsResponse.Path)，响应 $($tenantOptionsResponse.Body)"
}
Assert-Status $tenantOptionsResponse @(200) "前端通过 Gateway 查询 Tenant 列权限裁剪接口失败"
Assert-TraceHeader $tenantOptionsResponse
$tenantPage = Unwrap-ApiData (Convert-ResponseJson $tenantOptionsResponse "Tenant 列权限接口响应解析失败")
Assert-JsonProperty $tenantPage "records" "Tenant 列权限接口缺少 records"
Assert-JsonProperty $tenantPage "total" "Tenant 列权限接口缺少 total"
Assert-JsonProperty $tenantPage "pageNum" "Tenant 列权限接口缺少 pageNum"
Assert-JsonProperty $tenantPage "pageSize" "Tenant 列权限接口缺少 pageSize"
Assert-ColumnPermissionApplied $snapshot $tenantPage

Write-Host "OK: 前端 Origin -> Gateway -> IAM 当前菜单/权限快照 -> Tenant 列权限裁剪真实链路验证通过。"
