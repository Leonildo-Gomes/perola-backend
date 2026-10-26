#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Start Up Batch script for POSIX systems
# ----------------------------------------------------------------------------

# Resolve modern JDK if JAVA_HOME is unset or pointing to legacy Java (e.g., 1.8)
if [ -z "$JAVA_HOME" ] || echo "$JAVA_HOME" | grep -q "1.8"; then
  if [ -x "/usr/libexec/java_home" ]; then
    DETECTED_JAVA=$(/usr/libexec/java_home -v 26 2>/dev/null || /usr/libexec/java_home -v 21 2>/dev/null)
    if [ -n "$DETECTED_JAVA" ]; then
      export JAVA_HOME="$DETECTED_JAVA"
    fi
  fi
fi

# Fallback to system Maven if available
if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

echo "Error: mvn command not found and wrapper bootstrap requires mvn." >&2
exit 1
