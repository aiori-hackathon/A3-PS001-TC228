#!/system/bin/sh

TARGET="/sdcard/ranslab"

for file in "$TARGET"/file_*.txt
do

    echo "SIMULATED ENCRYPTED DATA" > "$file"

    mv "$file" "$file.locked"

    sleep 0.5

done