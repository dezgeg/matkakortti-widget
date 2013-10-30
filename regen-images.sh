#!/bin/bash -e
cd $(dirname $0)/res/drawable

make_icon() {
    kind=$1
    resolution=$2
    file=$3

    convert $file -resize ${resolution}x ../drawable-$kind/$file
}

convert ok.png -colorspace gray ok_gray.png

# 12 px icons
for f in euro_sign watch; do
    convert -background none svg/$f.svg -resize 72x $f.png
    convert +level-colors "#ebcd12," $f.png $f.png

    make_icon "mdpi" 12 "$f.png"
    make_icon "hdpi" 18 "$f.png"
    make_icon "xhdpi" 24 "$f.png"
    make_icon "xxhdpi" 36 "$f.png"
done

# Someone might say this is a gross hack. And they'd be right.
convert -background none svg/icon_half.svg -crop 256x512+0x0 temp-left.png
cat svg/icon_half.svg | sed -e 's/#ff0000/#00ff00/g' > temp.svg
convert -background none temp.svg -crop 256x512+0x0 -flop temp-right.png
convert +append temp-left.png temp-right.png icon.png

rm temp*

make_icon "mdpi" 48 "icon.png"
make_icon "hdpi" 72 "icon.png"
make_icon "xhdpi" 96 "icon.png"
make_icon "xxhdpi" 144 "icon.png"
