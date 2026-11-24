@echo off
chcp 65001 > nul
echo ========================================================
echo   Compiling: ஈகை — பகுத்துண்டு பல்லுயிர் ஓம்புதல்
echo   Community Food Donation ^& Rescue Network
echo ========================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -cp ".;lib/*" -d bin src/com/fooddonation/exception/*.java src/com/fooddonation/model/*.java src/com/fooddonation/dao/*.java src/com/fooddonation/service/*.java src/com/fooddonation/util/*.java src/com/fooddonation/ui/*.java src/com/fooddonation/*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Compilation Successful! Output classes in bin/
) else (
    echo.
    echo [ERROR] Compilation Failed with error code %ERRORLEVEL%
)
pause
