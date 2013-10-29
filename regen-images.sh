#!/bin/bash -e
cd $(dirname $0)/res/drawable

convert ok.png -colorspace gray ok_gray.png

for f in euro_sign watch; do
    # inkscape svg/$f.svg --export-png=$f.png --export-width=64
    convert svg/$f.svg -background none -resize 128x $f.png
    convert -transparent white +level-colors yellow, $f.png $f.png
done

# Someone might say this is a gross hack. And they'd be right.
convert -background none svg/icon_half.svg -crop 256x512+0x0 temp-left.png
cat svg/icon_half.svg | sed -e 's/#ff0000/#00ff00/g' > temp.svg
convert -background none temp.svg -crop 256x512+0x0 -flop temp-right.png
convert +append temp-left.png temp-right.png icon.png

rm temp*

make_icon() {
    kind=$1
    resolution=$2
    file=$3

    convert $file -resize ${resolution}x ../drawable-$kind/$file
}
make_icon "mdpi" 48 "icon.png"
make_icon "hdpi" 72 "icon.png"
make_icon "xhdpi" 96 "icon.png"
make_icon "xxhdpi" 144 "icon.png"
