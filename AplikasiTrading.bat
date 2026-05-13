@echo off
start "Server" cmd /k run_server.bat
timeout /t 2
start "App" cmd /k run_app.bat
exit