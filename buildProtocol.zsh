#!/bin/zsh
# Create jar from protobuf source files
#
# @file Build script
# @author Christoph Kappel <kappel@powerfolder.com>
# @version $Id$
#

# Config
VERSION="0.1"
PROTO_IN="../PF-PROTOCOL"
JAR_NAME="powerfolder-protobuf"
ANY_MESSAGE_PROTO="AnyMessageProto.proto"

JAR_SOURCE_OUT="lib/${JAR_NAME}-source-${VERSION}.jar"
JAR_CLASS_OUT="lib/${JAR_NAME}-${VERSION}.jar"

HTML_OUT="html"

function usage
{
  echo "Usage: $1 [-d|-g|-s|-j|-w|-h]"
  echo
  echo "  -d  Enable debug (set -x)"
  echo "  -g  Generate AnyMessageProto file (${ANY_MESSAGE_PROTO})"
  echo "  -s  Create source jar (${JAR_SOURCE_OUT})"
  echo "  -j  Create class jar (${JAR_CLASS_OUT})"
  echo "  -w  Build HTML web view of files (${HTML_OUT})"
  echo "  -h  Show this help."
  echo
}

# Check env
for PROG in protoc javac jar
do
  PROGPATH="`which "$PROG" 2>"/dev/null"`"

  if [ -z "$PROGPATH" ] ; then
    echo "$PROG not found in \$PATH."
    exit -1
  fi
done

if [ 0 -eq "$#" ] ; then
  usage $0
  exit -1
fi


# Finally do stuff
while getopts ":dgsjwh" OPT; do
  case $OPT in
    d)
      set -x
      ;;
    g)
      # Generate any message proto
      cat <<-EOF > ${PROTO_IN}/${ANY_MESSAGE_PROTO}
syntax = "proto3";
option java_package = "de.dal33t.powerfolder.protocol";

EOF

      # Add imports
      for PROTO in ${PROTO_IN}/*.proto
      do
        # Check whether we found our any message proto
        [ "x${PROTO}" = "x${PROTO_IN}/${ANY_MESSAGE_PROTO}" ] && continue

        echo "import \"`basename $PROTO`\";" >> ${PROTO_IN}/${ANY_MESSAGE_PROTO}
      done

      # Continue file
      cat <<-EOF >> ${PROTO_IN}/${ANY_MESSAGE_PROTO}

message AnyMessage { // Simple any message
  string clazzName = 1;

  oneof any {
EOF

      # Add messages to oneof
      I=10;
      for PROTO in ${PROTO_IN}/*.proto
      do
        # Check whether we found our any message proto
        [ "x${PROTO}" = "x${PROTO_IN}/${ANY_MESSAGE_PROTO}" ] && continue

        PROTO_NAME=`basename ${PROTO/Proto} | cut -d '.' -f1`

        echo "    $PROTO_NAME ${PROTO_NAME:l} = $I;" >> ${PROTO_IN}/${ANY_MESSAGE_PROTO}

        I=`expr $I + 1`
      done

      # Close braces
      cat <<-EOF >> ${PROTO_IN}/${ANY_MESSAGE_PROTO}
  }
}
EOF
      echo "[INFO] Generated \`${PROTO_IN}/${ANY_MESSAGE_PROTO}\`"
      ;;
    s)
      \protoc --proto_path=$PROTO_IN --java_out=$JAR_SOURCE_OUT $PROTO_IN/*.proto

      echo "[INFO] Created \`$JAR_SOURCE_OUT\`"
      ;;
    j)
      TEMP_JAVA_OUT="./build/java"
      TEMP_CLASS_OUT="./build/class"

      mkdir -p $TEMP_JAVA_OUT
      mkdir -p $TEMP_CLASS_OUT

      \protoc --proto_path=$PROTO_IN --java_out=$TEMP_JAVA_OUT $PROTO_IN/*.proto

      echo "[INFO] Generated \`$TEMP_JAVA_OUT\`"

      \javac -cp ".:./lib/protobuf-java-3.1.0.jar" -d $TEMP_CLASS_OUT $TEMP_JAVA_OUT/**/*.java

      echo "[INFO] Compiled \`$TEMP_CLASS_OUT\´"

      \jar cf $JAR_CLASS_OUT -C $TEMP_CLASS_OUT .

      echo "[INFO] Created \`$JAR_CLASS_OUT\`"

      rm -rf `dirname $TEMP_CLASS_OUT`
      ;;
    w)
      # Create or clean web out
      if ! [ -d "$HTML_OUT" ] ; then
        mkdir "$HTML_OUT"
      else
        rm -rf "$HTML_OUT"/*
      fi

      # Open html out
      cat <<-EOF > "$HTML_OUT/_index.html"
<html>
  <head>
    <title>Index</title>
  </head>

  <body>
    <h1>Protocols</h1>

    <div>
EOF

      for PROTO in "$PROTO_IN"/*.proto; do
        HTMLFILE=`basename $PROTO | sed "s#proto#html#"`

        # Open html out
        cat <<-EOF > "$HTML_OUT/$HTMLFILE"
<html>
  <head>
    <title>${HTMLFILE}</title>
    <style>
      p { margin: 0px; passing: 0px; }
      .lang { color: #999999; }
      .para { display: inline-block; margin-top: 15px; }
      .indent { margin-left: 30px; }
      .indent2 { margin-left: 60px; }
      .special { color: #aa00aa; }
      .message { color: #ff0000; }
      .enum { display: inline-block; color: #0aaaa0; }
      .enum-value { display: inline-block; color: #000066; }
      .type { color: #00aa00;; }
      .back { display: block; padding-top: 30px; }
      .comment { color: #666666 !important; }
      .comment * { color: #666666 !important; }
      .link { color: #0000ff; }
    </style>
  </head>

  <body>
    <code>
EOF

        # Update content
        ISENUM=0
        ENUMS=()

        cat "$PROTO" | while read LINE; do

          # Chars
          LINE=${LINE/=/'<span class="special">=</span>'}
          LINE=${LINE/\{/'<span class="special para">{</span>'}
          LINE=${LINE/;/'<span class="special">;</span>'}

          # Special case: Closing curly brace
          if [ "}" = "${LINE:0:1}" ] ; then
            if [ 1 -eq $ISENUM ] ; then
              LINE=${LINE/\}/'<span class="special indent">}</span>'}

              ISENUM=0
            else
              LINE=${LINE/\}/'<span class="special">}</span>'}
            fi
          fi

          # Lang keywords
          LINE=${LINE/syntax/'<span class="lang">syntax</span>'}
          LINE=${LINE/option/'<span class="lang">option</span>'}
          LINE=${LINE/import/'<span class="lang">import</span>'}
          LINE=${LINE/repeated/'<span class="lang indent">repeated</span>'}

          LINE=${LINE/message/'<span class="message">message</span>'}

          # Enum
          if [ "${LINE:0:4}" = "enum" ] ; then
            NAME=`echo ${LINE:5} | cut -d ' ' -f1`
            ENUMS+=($NAME)

            LINE=${LINE/enum/'<span class="enum indent">enum</span>'}

            ISENUM=1
          fi

          # Enum value
          VALUE=`echo $LINE | cut -d ' ' -f 1`

          if [ "`echo $VALUE | tr '[:lower:]' '[:upper]'`" = "$VALUE" ] ; then
            LINE=`echo $LINE | sed "s#\([A-Z_]*\)#<span class=\"enum-value indent2\">\1</span>#"`
          fi

          # Types
          LINE=${LINE/string/'<span class="type indent">string</span>'}
          LINE=${LINE/int32/'<span class="type indent">int32</span>'}
          LINE=${LINE/int64/'<span class="type indent">int64</span>'}
          LINE=${LINE/bool/'<span class="type indent">bool</span>'}
          LINE=${LINE/double/'<span class="type indent">double</span>'}
          LINE=${LINE/bytes/'<span class="type indent">bytes</span>'}

          # Comments
          LINE=`echo $LINE | sed "s#\(//.*\)#<span class=\"comment\">\1</span>#"`

          # Everything unhandled must be either enum types or proto types
          if ! [ "<" = "${LINE:0:1}" ] ; then
            TYPE=`echo $LINE | cut -d ' ' -f 1`
            KLASS="link"
            HREF="${TYPE}Proto.html"

            for (( i=1; i<=${#ENUMS[@]}; i++ )); do
              if [ "$TYPE" = "${ENUMS[$i]}" ] ; then
                KLASS="enum"
                HREF=""

                break
              fi
            done

            LINE=${LINE/$TYPE/"<a class=\"$KLASS indent\" href=\"${HREF}\">$TYPE</a>"}
          fi

          echo "<p>$LINE</p>" >> "$HTML_OUT/$HTMLFILE"
        done

        echo "[INFO] Created \`$HTML_OUT/$HTMLFILE\`"

        # Close html out
        cat <<-EOF >> "$HTML_OUT/$HTMLFILE"

      </code>
    <a class="back" href="javascript:history.back()">Back</a>
  </body>
</html>
EOF

        echo "<a style=\"display: inline-block; width: 32%\" href=\"$HTMLFILE\">${HTMLFILE/.html/}</a>" >> "$HTML_OUT/_index.html"
      done

      # Close index
      cat <<-EOF >> "$HTML_OUT/_index.html"
    </div>
  </body>
</html>
EOF

      echo "[INFO] Created \`$HTML_OUT/_index.html\`"
      ;;

    h)
      usage

      exit
      ;;
    \?)
      echo "[ERROR] Unknown argument: $OPTARG"
      echo
      usage $0
      ;;
  esac
done
