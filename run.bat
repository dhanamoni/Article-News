@echo off
if not exist out\production\article (
  echo Compiled classes not found. Run compile.bat first.
  exit /b 1
)
java -cp out\production\article Main %*
