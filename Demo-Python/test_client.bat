@echo off
REM Test Demo-Python gRPC Client
REM Usage: test_client.bat [chunk_size]

echo ========================================
echo Testing Demo-Python gRPC Client
echo ========================================

cd src

if "%1"=="" (
    echo Using default chunk size: 300
    python grpc_client.py
) else (
    echo Using chunk size: %1
    python grpc_client.py --chunk-size %1
)

pause
