@echo off
cd /d "%~dp0"
java -cp "out;lib/*" com.irul.trading.util.TradeHttpServer
pause