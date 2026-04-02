$outputDirectory = "out\production\article"

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

javac -d $outputDirectory src\Main.java src\model\*.java src\repository\*.java src\service\*.java src\storage\*.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful."
} else {
    Write-Host "Compilation failed."
    exit $LASTEXITCODE
}
