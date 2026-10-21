#!/system/bin/sh
TARGET=/sdcard/ranslab
i=1
while [ $i -le 50 ]
do
echo "Backup copy $i" > "$TARGET/backup_$i.txt"
i=`expr $i + 1`
sleep 0.05
done
echo "Backup workload completed."
