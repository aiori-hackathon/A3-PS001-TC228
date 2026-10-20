#!/system/bin/sh

TARGET="/sdcard/ranslab/editor_test.txt"

echo "Initial document" > "$TARGET"

i=1

while [ $i -le 20 ]
do
    echo "Normal edit $i" >> "$TARGET"

    if [ $((i % 5)) -eq 0 ]; then
        mv "$TARGET" "$TARGET.tmp"
        mv "$TARGET.tmp" "$TARGET"
    fi

    i=$((i+1))
    sleep 0.5
done