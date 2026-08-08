#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*->\(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVEDPWD=`pwd`
cd "`dirname "$PRG"`/" >/dev/null
APP_HOME=`pwd -P`
cd "$SAVEDPWD" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='" -Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != maximum.
MAX_FD="maximum"

warn () {
    echo "$*" >&2
}

die () {
    echo
    echo "$*" >&2
    exit 1
}

# OS specific support (must be 'true' or 'false').
darwin=false
msys=false
cygwin=false
mingw=false
case "`uname`" in
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    mingw=true
    ;;
  MSYS* )
    msys=true
    ;;
  CYGWIN* )
    cygwin=true
    ;;
esac

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/bin/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/bin/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$mingw" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --windows "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 3 -type d -name gradle-wrapper.jar 2>/dev/null | head -n 1`
    SEP=""
    for dir in $ROOTDIRSRAW
    do
        ROOTDIR="$dir/.."
        SEP=":"
    done
    CLASSPATH="$SEP$CLASSPATH"

    # Determine the Java command to use to start the JVM
    if [ -n "$JAVA_HOME" ] ; then
        if [ -x "$JAVA_HOME/jre/bin/java" ] ; then
            # IBM's JDK on AIX uses strange locations for the executables
            JAVACMD="$JAVA_HOME/jre/bin/java"
        else
            JAVACMD="$JAVA_HOME/bin/java"
        fi
    fi

    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi

    # Start the Gradle daemon in the background.
    mkdir -p "$GRADLE_USER_HOME/daemon" 2>/dev/null
    touch "$GRADLE_USER_HOME/daemon/.lock"
fi

# For Msys, switch paths to Windows format before running java
if "$msys" = true ; then
    APP_HOME=`cmd //c echo $APP_HOME | sed 's|\\\\|/|g'`
    CLASSPATH=`cmd //c echo $CLASSPATH | sed 's|\\\\|/|g'`
    JAVACMD=`cmd //c echo $JAVACMD | sed 's|\\\\|/|g'`
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...sysprop settings
#   * else a system property defining a property file location.

while [ $# -gt 0 ]
do
    case "$1" in
      --no-jvmargs)                          # disabling jvmargs taking precedence over config file
           NO_JVM_ARGS="true"
           ;;
      -*)  handle_option
           ;;
      *)   break
           ;;
    esac
    shift
done

handle_option() {
  case "$1" in
    --classpath | -cp )
      shift
      [ $# -gt 0 ] || die "ERROR: \'$1\' requires an argument"
      CLASSPATH="$1"
      ;;
    --modulepath | -p )
      shift
      [ $# -gt 0 ] || die "ERROR: \'$1\' requires an argument"
      MODULE_PATH="$1"
      ;;
    --add-modules )
      shift
      [ $# -gt 0 ] || die "ERROR: \'$1\' requires an argument"
      ADD_MODULES="$1"
      ;;
  esac
}

eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS
jvm_args=""
while [ $# -gt 0 ]
do
    jvm_args="\"$jvm_args\" \"$1\""
    shift
done

# Split up the JVM_OPTS And GRADLE_OPTS values into an array, following the shell quoting and substitution rules
function splitJvmOpts() {
    jvmopts=("$@")
}
eval splitJvmOpts $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS

exec "$JAVACMD" "${JVM_ARGS[@]}" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
