@echo off
echo ========================================
echo   hada serveur
echo ========================================
echo.
mvn exec:java -Dexec.mainClass="org.example.socketproject.server.ServerMain"
pause








