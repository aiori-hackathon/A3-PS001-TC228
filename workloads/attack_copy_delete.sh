#!/system/bin/sh

TARGET="/sdcard/ranslab"

for file in "$TARGET"/file_*.txt
do

    encrypted="$file.encrypted"

    echo "SIMULATED RANDOM ENCRYPTED DATA" > "$encrypted"

    rm "$file"

done#!/system/bin/sh

TARGET="/sdcard/ranslab"

for file in "$TARGET"/file_*.txt
do

    encrypted="$file.encrypted"

    echo "SIMULATED RANDOM ENCRYPTED DATA" > "$encrypted"

    rm "$file"

done