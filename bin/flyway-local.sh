#!/usr/bin/env bash

DIR="$(cd "$(dirname "$(realpath "${BASH_SOURCE[0]}")")" && pwd)"
BASEDIR=$(realpath "$DIR"/..)
echo "$BASEDIR"

if [[ "$1" = "" ]]; then
  CMD="migrate"
else
  CMD=$1
  shift
fi

cd "$BASEDIR"/_database || exit
mvn -P local flyway:"$CMD" "${PARAM[@]}" "$@"
