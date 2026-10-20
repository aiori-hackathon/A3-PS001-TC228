
# Create 100 harmless dummy text files

$folder = Join-Path $PSScriptRoot "test_files"

# Create the folder if it does not exist
New-Item -ItemType Directory -Path $folder -Force | Out-Null

# Generate 100 dummy files
for ($i = 1; $i -le 100; $i++) {

    $fileName = "file_{0:D3}.txt" -f $i
    $filePath = Join-Path $folder $fileName

    $content = "Dummy laboratory file number $i"

    Set-Content -Path $filePath -Value $content
}

Write-Host "Successfully created 100 dummy files."
Write-Host "Location: $folder"
