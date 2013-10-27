#!/bin/bash
cd $(dirname $0)/res/drawable

convert ok.png -colorspace gray ok_gray.png

for f in euro_sign watch; do
    rm $f.png 2>/dev/null
    # inkscape svg/$f.svg --export-png=$f.png --export-width=64
    convert svg/$f.svg -background none -resize 128x $f.png
    convert -transparent white +level-colors yellow, $f.png $f.png
done
