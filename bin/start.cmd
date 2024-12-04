@echo off
rem
rem Galia startup script for Windows
rem

set SCRIPT_DIR=%~dp0
for %%F in ("%SCRIPT_DIR%.") do set ROOT_DIR=%%~dpF
set LIB_DIR=%ROOT_DIR%lib
set JVM_OPTIONS_FILE=%ROOT_DIR%config\jvm.options

java @"%JVM_OPTIONS_FILE%" ^
    -cp "%LIB_DIR%\*" ^
    is.galia.Application ^
    -config "%ROOT_DIR%config\config.yml"
