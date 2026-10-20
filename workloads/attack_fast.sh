#!/system/bin/sh

TARGET="/sdcard/ranslab"

for file in "$TARGET"/file_*.txt
do

    echo "SIMULATED ENCRYPTED CONTENT" > "$file"

    mv "$file" "$file.locked"

done