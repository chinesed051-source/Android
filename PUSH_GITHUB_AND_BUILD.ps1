$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$RepoUrl = 'https://github.com/chinesed051-source/Android.git'
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$TempRoot = Join-Path $env:TEMP 'JingPing-GitHub-Push'

function Find-Git {
    $cmd = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $roots = @(
        (Join-Path $env:LOCALAPPDATA 'GitHubDesktop'),
        (Join-Path $env:LOCALAPPDATA 'GitHub Desktop')
    )

    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        $git = Get-ChildItem -Path $root -Recurse -Filter git.exe -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\git\\(cmd|bin)\\git\.exe$' } |
            Sort-Object FullName -Descending |
            Select-Object -First 1
        if ($git) { return $git.FullName }
    }

    throw 'Git was not found. Install Git for Windows or GitHub Desktop first.'
}

function Run-Git([string[]]$GitArgs, [string]$ErrorMessage) {
    & $Git @GitArgs
    if ($LASTEXITCODE -ne 0) { throw $ErrorMessage }
}

$Git = Find-Git
Write-Host ('Git: ' + $Git) -ForegroundColor Cyan

if (Test-Path $TempRoot) {
    Remove-Item $TempRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $TempRoot | Out-Null

Write-Host 'Cloning repository...' -ForegroundColor Yellow
& $Git clone $RepoUrl $TempRoot
if ($LASTEXITCODE -ne 0) {
    throw 'Clone failed. Check your GitHub/network access.'
}

# Remove repository files but keep .git, then copy the Android project.
Get-ChildItem -LiteralPath $TempRoot -Force |
    Where-Object { $_.Name -ne '.git' } |
    Remove-Item -Recurse -Force

$exclude = @('.git', '.gradle', '.idea', 'build', '.tools')
Get-ChildItem -LiteralPath $ProjectRoot -Force |
    Where-Object { $exclude -notcontains $_.Name } |
    ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $TempRoot -Recurse -Force
    }

Push-Location $TempRoot
try {
    & $Git checkout -B main
    if ($LASTEXITCODE -ne 0) { throw 'Could not create/switch to main branch.' }

    & $Git config user.name 'JingPing Builder'
    & $Git config user.email 'jingping-builder@users.noreply.github.com'
    & $Git add -A

    $changes = & $Git status --porcelain
    if (-not $changes) {
        Write-Host 'Repository already contains the latest files.' -ForegroundColor Green
    } else {
        & $Git commit -m 'build: publish JingPing Android v0.2.1'
        if ($LASTEXITCODE -ne 0) { throw 'Git commit failed.' }

        Write-Host 'Pushing to GitHub...' -ForegroundColor Yellow
        & $Git push -u origin main
        if ($LASTEXITCODE -ne 0) {
            Write-Host ''
            Write-Host 'GitHub authentication blocked the push.' -ForegroundColor Red
            Write-Host 'If a browser/login window appeared, finish login and run this script again.' -ForegroundColor Yellow
            throw 'Git push failed.'
        }
    }
}
finally {
    Pop-Location
}

Write-Host ''
Write-Host 'Upload complete. GitHub Actions should start automatically.' -ForegroundColor Green
Write-Host 'Repository: https://github.com/chinesed051-source/Android' -ForegroundColor Cyan
Write-Host 'Actions:    https://github.com/chinesed051-source/Android/actions' -ForegroundColor Cyan
Start-Process 'https://github.com/chinesed051-source/Android/actions'
