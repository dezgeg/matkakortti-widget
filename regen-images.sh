#!/bin/bash
cd $(dirname $0)/res/drawable

convert ok.png -colorspace gray ok_gray.png
