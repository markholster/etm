#! /bin/sh

#-----------------------------------------------------------------------------
# These settings can be modified to fit the needs of your application

# Application
APP_NAME="${applicationName}"
APP_VERSION="${applicationVersion}"
APP_LONG_NAME="Enterprise Telemetry Monitor"

# discover BASEDIR
BASEDIR=`dirname "$0"`/..
BASEDIR=`(cd "$BASEDIR"; pwd)`
ls -l "$0" | grep -e '->' > /dev/null 2>&1
if [ $? = 0 ]; then
  #this is softlink
  _PWD=`pwd`
  _EXEDIR=`dirname "$0"`
  cd "$_EXEDIR"
  _BASENAME=`basename "$0"`
  _REALFILE=`ls -l "$_BASENAME" | sed 's/.*->\ //g'`
   BASEDIR=`dirname "$_REALFILE"`/..
   BASEDIR=`(cd "$BASEDIR"; pwd)`
   cd "$_PWD"
fi


# Location of the pid file.
PIDDIR="$BASEDIR"
CONFIGDIR="$BASEDIR/config"

# The following two lines are used by the chkconfig command. Change as is
#  appropriate for your application.  They should remain commented.
# chkconfig: 2345 20 80
# description: Enterprise Telemetry Monitor

# Do not modify anything beyond this point
#-----------------------------------------------------------------------------

# Get the fully qualified path to the script
case $0 in
    /*)
        SCRIPT="$0"
        ;;
    *)
        PWD=`pwd`
        SCRIPT="$PWD/$0"
        ;;
esac

# Resolve the true real path without any sym links.
CHANGED=true
while [ "X$CHANGED" != "X" ]
do
    # Change spaces to ":" so the tokens can be parsed.
    SAFESCRIPT=`echo $SCRIPT | sed -e 's; ;:;g'`
    # Get the real path to this script, resolving any symbolic links
    TOKENS=`echo $SAFESCRIPT | sed -e 's;/; ;g'`
    REALPATH=
    for C in $TOKENS; do
        # Change any ":" in the token back to a space.
        C=`echo $C | sed -e 's;:; ;g'`
        REALPATH="$REALPATH/$C"
        # If REALPATH is a sym link, resolve it.  Loop for nested links.
        while [ -h "$REALPATH" ] ; do
            LS="`ls -ld "$REALPATH"`"
            LINK="`expr "$LS" : '.*-> \(.*\)$'`"
            if expr "$LINK" : '/.*' > /dev/null; then
                # LINK is absolute.
                REALPATH="$LINK"
            else
                # LINK is relative.
                REALPATH="`dirname "$REALPATH"`""/$LINK"
            fi
        done
    done

    if [ "$REALPATH" = "$SCRIPT" ]
    then
        CHANGED=""
    else
        SCRIPT="$REALPATH"
    fi
done

# Change the current directory to the location of the script
cd "`dirname "$REALPATH"`"
REALDIR=`pwd`

# If the PIDDIR is relative, set its value relative to the full REALPATH to avoid problems if
#  the working directory is later changed.
FIRST_CHAR=`echo $PIDDIR | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    PIDDIR=$REALDIR/$PIDDIR
fi

# Process ID
PIDFILE="$PIDDIR/$APP_NAME.pid"
LOCKDIR="/var/lock/subsys"
LOCKFILE="$LOCKDIR/$APP_NAME"
pid=""

# Resolve the location of the 'ps' command
PSEXE="/usr/bin/ps"
if [ ! -x "$PSEXE" ]
then
    PSEXE="/bin/ps"
    if [ ! -x "$PSEXE" ]
    then
        echo "Unable to locate 'ps'."
        echo "Please report this message along with the location of the command on your system."
        exit 1
    fi
fi

# Resolve the os
DIST_OS=`uname -s | tr "[A-Z]" "[a-z]" | tr -d ' '`
case "$DIST_OS" in
    'sunos')
        DIST_OS="solaris"
        ;;
    'hp-ux' | 'hp-ux64')
        DIST_OS="hpux"
        ;;
    'darwin')
        DIST_OS="macosx"
        ;;
    'unix_sv')
        DIST_OS="unixware"
        ;;
    cygwin_nt*)
        DIST_OS="cygwin"
        ;;
esac

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if [ "$DIST_OS" = "cygwin" ]; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# If a specific java binary isn't specified search for the standard 'java' binary
if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java`
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." 1>&2
  echo "  We cannot execute $JAVACMD" 1>&2
  exit 1
fi

if [ -z "$REPO" ]
then
  REPO="$BASEDIR"/lib
fi

CLASSPATH="$REPO"/*:"$REPO"/ext/*

getpid() {
    if [ -f "$PIDFILE" ]
    then
        if [ -r "$PIDFILE" ]
        then
            pid=`cat "$PIDFILE"`
            if [ "X$pid" != "X" ]
            then
                # It is possible that 'a' process with the pid exists but that it is not the
                #  correct process.  This can happen in a number of cases, but the most
                #  common is during system startup after an unclean shutdown.
                # The ps statement below looks for the specific wrapper command running as
                #  the pid.  If it is not found then the pid file is considered to be stale.
                if [ "$DIST_OS" = "macosx" ]; then
                  pidtest=`$PSEXE -p $pid -o command -ww | grep "$JAVACMD" | tail -1`
                else
                  pidtest=`$PSEXE -o args,pid | grep "$JAVACMD" | grep $pid | tail -1`
                fi
                if [ "X$pidtest" = "X" ]
                then
                    # This is a stale pid file.
                    rm -f "$PIDFILE"
                    echo "Removed stale pid file: $PIDFILE"
                    pid=""
                fi
            fi
        else
            echo "Cannot read $PIDFILE."
            exit 1
        fi
    fi
}

testpid() {
    if [ "$DIST_OS" = "macosx" ]; then
      pidtest=`$PSEXE -p $pid -o command -ww | grep "$JAVACMD" | tail -1`
    else
      pidtest=`$PSEXE -o args,pid | grep "$JAVACMD" | grep $pid | tail -1`
    fi
    if [ "X$pidtest" = "X" ]
    then
        # Process is gone so remove the pid file.
        rm -f "$PIDFILE"
        pid=""
    fi
}

console() {
    getpid
    if [ "X$pid" = "X" ]
    then
        # The string passed to eval must handles spaces in paths correctly.
        COMMAND_LINE="$CMDNICE \"$JAVACMD\" $JAVA_OPTS -Xms256m -Xmx1024m -Xss256m -classpath \"$CLASSPATH\" -Djava.net.useSystemProxies=true -Dapp.name=\"$APP_NAME\" -Dapp.version=\"$APP_VERSION\" -Dapp.repo=\"$REPO\" -Dapp.home=\"$BASEDIR\" ${mainClassName} --config-dir=\"$CONFIGDIR\" $@"
        eval $COMMAND_LINE
    else
        echo "$APP_LONG_NAME is already running."
        exit 1
    fi
}

start() {
    getpid
    if [ "X$pid" = "X" ]
    then
        nohup $JAVACMD $JAVA_OPTS -Xms256m -Xmx1024m -Xss256m -classpath "$CLASSPATH" -Djava.net.useSystemProxies=true -Dapp.name="$APP_NAME" -Dapp.version="$APP_VERSION" -Dapp.repo="$REPO" -Dapp.home="$BASEDIR" ${mainClassName} --config-dir="$CONFIGDIR" $@ </dev/null >/dev/null 2>&1 &
        echo $! > $PIDFILE
    else
        echo "$APP_LONG_NAME is already running."
        exit 1
    fi
}

stopit() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."
    else
        if [ "X$IGNORE_SIGNALS" = "X" ]
        then
            # Running so try to stop it.
            kill $pid
            if [ $? -ne 0 ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        else
            rm -f "$ANCHORFILE"
            if [ -f "$ANCHORFILE" ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        fi

        # We can not predict how long it will take for the wrapper to
        #  actually stop as it depends on settings in wrapper.conf.
        #  Loop until it does.
        savepid=$pid
        CNT=0
        TOTCNT=0
        while [ "X$pid" != "X" ]
        do
            # Show a waiting message every 5 seconds.
            if [ "$CNT" -lt "5" ]
            then
                CNT=`expr $CNT + 1`
            else
                echo "Waiting for $APP_LONG_NAME to exit..."
                CNT=0
            fi
            TOTCNT=`expr $TOTCNT + 1`

            sleep 1

            testpid
        done

        pid=$savepid
        testpid
        if [ "X$pid" != "X" ]
        then
            echo "Failed to stop $APP_LONG_NAME."
            exit 1
        else
            echo "Stopped $APP_LONG_NAME."
        fi
    fi
}

status() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME is not running."
        exit 1
    else
        echo "$APP_LONG_NAME is running ($pid)."
        exit 0
    fi
}

dump() {
    echo "Dumping $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."

    else
        kill -3 $pid

        if [ $? -ne 0 ]
        then
            echo "Failed to dump $APP_LONG_NAME."
            exit 1
        else
            echo "Dumped $APP_LONG_NAME."
        fi
    fi
}

reinitialize() {
    # The string passed to eval must handles spaces in paths correctly.
    COMMAND_LINE="$CMDNICE \"$JAVACMD\" $JAVA_OPTS -Xms256m -Xmx1024m -Xss256m -classpath \"$CLASSPATH\" -Djava.net.useSystemProxies=true -Dapp.name=\"$APP_NAME\" -Dapp.version=\"$APP_VERSION\" -Dapp.repo=\"$REPO\" -Dapp.home=\"$BASEDIR\" ${mainClassName} --config-dir=\"$CONFIGDIR\" --reinitialize"
    eval $COMMAND_LINE
}

tail() {
    # The string passed to eval must handles spaces in paths correctly.
    COMMAND_LINE="$CMDNICE \"$JAVACMD\" $JAVA_OPTS -Xms256m -Xmx1024m -Xss256m -classpath \"$CLASSPATH\" -Djava.net.useSystemProxies=true -Dapp.name=\"$APP_NAME\" -Dapp.version=\"$APP_VERSION\" -Dapp.repo=\"$REPO\" -Dapp.home=\"$BASEDIR\" ${mainClassName} --config-dir=\"$CONFIGDIR\" --tail"
    eval $COMMAND_LINE

}

case "$1" in

    'console')
        echo "Running $APP_LONG_NAME..."
        console $2 $3 $4 $5 $6
        ;;

    'docker')
        console '--quiet'
        ;;

    'start')
        echo "Starting $APP_LONG_NAME..."
        start $2 $3 $4 $5 $6
        ;;

    'stop')
        echo "Stopping $APP_LONG_NAME..."
        stopit
        ;;

    'restart')
        stopit
        start
        ;;

    'status')
        status
        ;;

    'dump')
        dump
        ;;

    'reinitialize')
        reinitialize
        ;;

    'tail')
        tail
        ;;
    *)
        echo "Usage: $0 { console | start | stop | restart | status | dump | reinitialize | tail}"
        exit 1
        ;;
esac

exit 0
