@echo off
rem
rem Update-all-plugins script for Windows
rem

set SCRIPT_DIR=%~dp0
for %%F in ("%SCRIPT_DIR%.") do set ROOT_DIR=%%~dpF
set LIB_DIR=%ROOT_DIR%lib

java -cp "%LIB_DIR%\*" ^
    is.galia.Application ^
    -update-plugins
