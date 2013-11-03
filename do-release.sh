#!/bin/bash
cd "$(dirname $0)"

echo "AndroidManifest.xml:"
cat AndroidManifest.xml  | grep 'android:version\(Code\|Name\)' | tr -d '>' | sed -e 's/^ */    /g'
echo "pom.xml:"
cat pom.xml  | grep '<version>' | head -n1
echo "DEBUG:"
cat src/fi/iki/dezgeg/matkakorttiwidget/gui/MatkakorttiWidgetApp.java  | grep 'public static.*DEBUG'
echo "assets/about.html:"
cat assets/about.html | grep 'id=.version' | sed -e 's/^/    /'
echo "git describe:"
git describe --dirty --tags | sed -e 's/^/    /'

echo
echo "Press Return to build release APK."
read unused

if [ -z "$1" ]; then
    echo "Usage: $0 <path-to-keystore>"
    exit 1
fi

set -e
echo "Compiling..."
mvn clean install -Prelease

echo
echo "Signing..."
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore "$1" target/matkakortti-widget.apk dezgeg
$ANDROID_HOME/tools/zipalign -f 4 target/matkakortti-widget.apk target/matkakortti-widget-release.apk

echo
echo "Signature:"
jarsigner -verify -certs -verbose target/matkakortti-widget-release.apk | grep 'CN=' | sort -u

echo
echo "Generated target/matkakortti-widget-release.apk"
