@echo off
cd /d "%~dp0"
java -cp "out;lib/*" com.irul.trading.MainFrame
pause