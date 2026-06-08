$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$logDir = Join-Path $root '.logs'
$pidFile = Join-Path $logDir 'docs-site.pid'
$outLog = Join-Path $logDir 'docs-site.out.log'
$errLog = Join-Path $logDir 'docs-site.err.log'

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (Test-Path $pidFile) {
  $existingPid = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue
  if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
    Write-Output "Docs site is already running. PID: $existingPid"
    exit 0
  }
}

$npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue)
if (-not $npm) {
  throw 'npm.cmd was not found in PATH.'
}

$process = Start-Process `
  -FilePath $npm.Source `
  -ArgumentList @('run', 'docs:dev') `
  -WorkingDirectory $root `
  -RedirectStandardOutput $outLog `
  -RedirectStandardError $errLog `
  -WindowStyle Hidden `
  -PassThru

Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ASCII
Write-Output "Docs site started. PID: $($process.Id)"
Write-Output 'URL: http://localhost:3000'
