@echo off
rem
rem Plugin removal script for Windows
rem

if "%~1" == "" (
    echo Usage: %0 ^<plugin name^>
    exit /B 1
)

set PLUGIN=%~1
set SCRIPT_DIR=%~dp0
for %%F in ("%SCRIPT_DIR%.") do set ROOT_DIR=%%~dpF
set LIB_DIR=%ROOT_DIR%lib
set GALIA_LOG_APPLICATION_LEVEL=debug
set GALIA_LOG_APPLICATION_CONSOLEAPPENDER_ENABLED=true

java -cp "%LIB_DIR%\*" ^
    is.galia.Application ^
    -remove-plugin %PLUGIN%
