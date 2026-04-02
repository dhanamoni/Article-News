@echo off
if not exist out\production\article mkdir out\production\article
javac -d out\production\article src\Main.java src\model\*.java src\repository\*.java src\service\*.java src\storage\*.java
if errorlevel 1 (
  echo Compilation failed.
  exit /b 1
)
echo Compilation successful.
