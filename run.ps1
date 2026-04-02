param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$outputDirectory = "out\production\article"

if (-not (Test-Path $outputDirectory)) {
    Write-Host "Compiled classes not found. Run .\compile.ps1 first."
    exit 1
}

java -cp $outputDirectory Main @Arguments
