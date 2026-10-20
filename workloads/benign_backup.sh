#!/system/bin/sh

TARGET="/sdcard/ranslab"

i=1

while [ $i -le 50 ]
do
    echo "Backup copy $i" > "$TARGET/backup_$i.txt"
    i=$((i+1))
    sleep 0.05
done