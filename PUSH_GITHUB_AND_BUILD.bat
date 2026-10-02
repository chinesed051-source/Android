@echo off
setlocal
cd /d "%~dp0"
echo JingPing GitHub uploader v0.2.1
echo.
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0PUSH_GITHUB_AND_BUILD.ps1"
set ERR=%ERRORLEVEL%
echo.
if not "%ERR%"=="0" (
  echo Upload script failed with exit code %ERR%.
) else (
  echo Done.
)
pause
exit /b %ERR%
