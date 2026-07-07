$PauseOnExit = $args -contains '-PauseOnExit'
$ErrorActionPreference = 'Stop'

function Exit-WithOptionalPause {
  param(
    [int] $Code = 0
  )

  if ($PauseOnExit) {
    Write-Output ''
    Write-Output 'Press Enter to close this window...'
    Read-Host | Out-Null
  }

  exit $Code
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$logDir = Join-Path $root '.logs'
$pidFile = Join-Path $logDir 'docs-site.pid'
$buildOutLog = Join-Path $logDir 'docs-site-build.out.log'
$buildErrLog = Join-Path $logDir 'docs-site-build.err.log'
$outLog = Join-Path $logDir 'docs-site.out.log'
$errLog = Join-Path $logDir 'docs-site.err.log'

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$processPath = [Environment]::GetEnvironmentVariable('Path', 'Process')
if (-not $processPath) {
  $processPath = [Environment]::GetEnvironmentVariable('PATH', 'Process')
}
if ($processPath) {
  [Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
  [Environment]::SetEnvironmentVariable('Path', $processPath, 'Process')
}

if (Test-Path $pidFile) {
  $existingPid = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue
  if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
    Write-Output "Docs site is already running. PID: $existingPid"
    Write-Output 'URL: http://localhost:3000'
    Exit-WithOptionalPause 0
  }
}

$npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue)
if (-not $npm) {
  throw 'npm.cmd was not found in PATH.'
}

Write-Output 'Building docs site and Pagefind search index...'
Write-Output "Build log: $buildOutLog"
Write-Output "Build error log: $buildErrLog"

$buildProcess = Start-Process `
  -FilePath $npm.Source `
  -ArgumentList @('run', 'docs:build') `
  -WorkingDirectory $root `
  -RedirectStandardOutput $buildOutLog `
  -RedirectStandardError $buildErrLog `
  -WindowStyle Hidden `
  -Wait `
  -PassThru

if ($buildProcess.ExitCode -ne 0) {
  Write-Output "Docs site build failed. See logs:"
  Write-Output $buildOutLog
  Write-Output $buildErrLog
  Exit-WithOptionalPause $buildProcess.ExitCode
}

Write-Output 'Build completed. Starting preview server...'

$process = Start-Process `
  -FilePath $npm.Source `
  -ArgumentList @('run', 'docs:preview') `
  -WorkingDirectory $root `
  -RedirectStandardOutput $outLog `
  -RedirectStandardError $errLog `
  -WindowStyle Hidden `
  -PassThru

Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ASCII
Write-Output "Docs site preview started with full search. PID: $($process.Id)"
Write-Output 'URL: http://localhost:3000'
Write-Output "Preview log: $outLog"
Write-Output "Preview error log: $errLog"
Exit-WithOptionalPause 0
