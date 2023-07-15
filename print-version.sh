#!/bin/bash
#Prints the current project version
#
# Created by Steve Hannah
# Creation Date March 19, 2021
# Usage: bash print-version.sh
# Output Example: 7.0.13-SNAPSHOT
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
cd $SCRIPTPATH
POM_FILE="$SCRIPTPATH/pom.xml"
VERSION=$(grep -m1 "<version>" $POM_FILE | awk -F'[<>]' '{print $3}')
echo "$VERSION"