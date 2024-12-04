#!/bin/sh
#
# Plugin installation script for Unix
#

if [ $# -lt 1 ]
then
    echo "Usage: $(basename "$0") <plugin name>"
    exit 1
fi

SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/$(basename "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "${SCRIPT_PATH}")"
ROOT_DIR="$(dirname "${SCRIPT_DIR}")"
LIB_DIR="${ROOT_DIR}/lib"
GALIA_LOG_APPLICATION_LEVEL=debug
GALIA_LOG_APPLICATION_CONSOLEAPPENDER_ENABLED=true

java -cp "${LIB_DIR}/*" \
    is.galia.Application \
    -install-plugin $1
