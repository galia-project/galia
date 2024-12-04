#!/bin/sh
#
# List fonts script for Unix
#

SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/$(basename "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "${SCRIPT_PATH}")"
ROOT_DIR="$(dirname "${SCRIPT_DIR}")"
LIB_DIR="${ROOT_DIR}/lib"

java -cp "${LIB_DIR}/*" \
    is.galia.Application \
    -list-fonts
