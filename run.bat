@echo off
chcp 65001 > nul
echo ========================================================
echo   ஈகை — பகுத்துண்டு பல்லுயிர் ஓம்புதல்
echo   Community Food Donation ^& Rescue Network
echo ========================================================
echo Launching application...
echo.

java "-Dfile.encoding=UTF-8" -cp "bin;lib/*;." com.fooddonation.Main
pause
