#!/bin/sh
#
# Galia startup script for Unix
#

SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/$(basename "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "${SCRIPT_PATH}")"
ROOT_DIR="$(dirname "${SCRIPT_DIR}")"
LIB_DIR="${ROOT_DIR}/lib"
CONFIG_DIR="${ROOT_DIR}/config"
JVM_OPTIONS_FILE="${CONFIG_DIR}/jvm.options"

java @"${JVM_OPTIONS_FILE}" \
    -cp "${LIB_DIR}/*" \
    is.galia.Application \
    -config "${ROOT_DIR}/config/config.yml"
