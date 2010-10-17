#!/bin/sh

GUICE_DIR=$1

svn export --force $GUICE_DIR/src core/src/main/java.orig
svn export --force $GUICE_DIR/test core/src/test/java.orig

for d in `(cd $GUICE_DIR; find extensions -type d -name src)`
do
  MAIN=$d/main
  svn export --force $GUICE_DIR/$d $MAIN/java
  if [ -d $MAIN/java/META-INF ]
  then
    mkdir -p $MAIN/resources/META-INF
    mv -f $MAIN/java/META-INF/* $MAIN/resources/META-INF/
    rmdir $MAIN/java/META-INF
  fi
done

for d in `(cd $GUICE_DIR; find extensions -type d -name test)`
do
  TEST=`dirname $d`/src/test
  svn export --force $GUICE_DIR/$d $TEST/java
  if [ -d $TEST/java/META-INF ]
  then
    mkdir -p $TEST/resources/META-INF
    mv -f $TEST/java/META-INF/* $TEST/resources/META-INF/
    rmdir $TEST/java/META-INF
  fi
done

mv extensions/persist/src/main/java/log4j.properties extensions/persist/src/main/resources
mv extensions/struts2/src/main/java/struts-plugin.xml extensions/struts2/src/main/resources

svn export --force $GUICE_DIR/extensions/struts2/example/root extensions/struts2/example/src/main/resources
svn export --force $GUICE_DIR/examples/src examples/src/main/java

