#!/system/bin/sh
TARGET=/sdcard/ranslab
echo "Initial document" > "$TARGET/editor_test.txt"
i=1
while [ $i -le 20 ]
do
echo "Normal edit $i" >> "$TARGET/editor_test.txt"
i=`expr $i + 1`
sleep 0.5
done
echo "Editor workload completed."
