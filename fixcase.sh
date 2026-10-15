#!/bin/bash
cd "Jni source/jni"
grep -rhoE '#include "[^"]+"' . | sed 's/#include "//;s/"$//' | sort -u | while read inc; do
  base=$(basename "$inc")
  if [ -z "$(find . -name "$base" -print -quit)" ]; then
    real=$(find . -iname "$base" -print -quit)
    [ -n "$real" ] && cp "$real" "$(dirname "$real")/$base" && echo "fix: $base <- $real"
  fi
done
