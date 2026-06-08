$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$pidFile = Join-Path $root '.logs\docs-site.pid'

if (Test-Path $pidFile) {
  $processId = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue
  if ($processId -and (Get-Process -Id $processId -ErrorAction SilentlyContinue)) {
    Stop-Process -Id $processId -Force
    Write-Output "Docs site stopped. PID: $processId"
  }
  Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

Get-CimInstance Win32_Process -Filter "name = 'node.exe'" | Where-Object {
  $_.CommandLine -and $_.CommandLine -like "*$root*" -and $_.CommandLine -like '*astro*'
} | ForEach-Object {
  Stop-Process -Id $_.ProcessId -Force
  Write-Output "Stopped remaining docs node process. PID: $($_.ProcessId)"
}
