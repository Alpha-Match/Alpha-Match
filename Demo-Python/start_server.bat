@echo off
REM Start Demo-Python gRPC Server
REM Usage: start_server.bat

echo ========================================
echo Starting Demo-Python gRPC Server
echo ========================================

cd src
python grpc_server.py

pause
