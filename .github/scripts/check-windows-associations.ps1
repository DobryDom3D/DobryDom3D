# This file is part of DobryDom3D, a fork of Sweet Home 3D 7.5, modified by DobryDom3D contributors since August 2026.
# Installs the DobryDom3D installer silently and checks its file association registry entries:
# .dd3d and .dd3x must be owned by DobryDom3D, while .sh3d and .sh3x must only list it in OpenWithProgids.
param([Parameter(Mandatory = $true)][string]$Installer)

$ErrorActionPreference = 'Stop'
$classes = 'Registry::HKEY_LOCAL_MACHINE\Software\Classes'
$progId = 'DobryDom3D.SweetHome3DHome'

$process = Start-Process -FilePath $Installer -ArgumentList '/quiet', '/norestart' -Wait -PassThru
if ($process.ExitCode -ne 0) {
  throw "Installer exited with code $($process.ExitCode)"
}

function Get-DefaultValue($path) {
  if (-not (Test-Path $path)) { return $null }
  return (Get-Item $path).GetValue('')
}

$errors = @()
foreach ($extension in '.dd3d', '.dd3x') {
  if (-not (Get-DefaultValue "$classes\$extension")) {
    $errors += "$extension has no default application"
  }
}
foreach ($extension in '.sh3d', '.sh3x') {
  $default = Get-DefaultValue "$classes\$extension"
  if ($default) {
    $errors += "$extension default application is '$default', expected none"
  }
  $key = "$classes\$extension\OpenWithProgids"
  if (-not (Test-Path $key) -or $null -eq (Get-Item $key).GetValue($progId)) {
    $errors += "$extension doesn't list $progId in OpenWithProgids"
  }
}
$command = Get-DefaultValue "$classes\$progId\shell\open\command"
if (-not $command -or -not $command.Contains('DobryDom3D.exe')) {
  $errors += "$progId has no open command running DobryDom3D.exe (found '$command')"
}
if (-not $command -or -not (Test-Path ($command -replace '^"([^"]+)".*$', '$1'))) {
  $errors += "The program of the open command of $progId doesn't exist"
}

foreach ($message in $errors) { Write-Host "::error::$message" }
if ($errors.Count -gt 0) { exit 1 }
Write-Host 'File associations are registered as expected'
