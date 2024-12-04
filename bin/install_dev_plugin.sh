#!/bin/sh
#
# This development script builds a plugin (located in a parallel directory
# outside of this one), copies it into the ./plugins directory, and unzips it,
# thereby installing it. If the plugin does not exist in a parallel directory,
# it is cloned from its remote git repository.
#

PLUGIN=$1
SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/$(basename "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "${SCRIPT_PATH}")"
GALIA_DIR="$(dirname "${SCRIPT_DIR}")"
SRC_PLUGIN_DIR="${GALIA_DIR}/../${PLUGIN}"
GALIA_PLUGIN_DIR="${GALIA_DIR}/plugins"
GITHUB_ORGANIZATION="galia-project"

if [ "${PLUGIN}" = "" ]
then
  echo "Usage: $0 <plugin>"
  exit 1
fi

if [ ! -d "$SRC_PLUGIN_DIR" ]
then
  echo "Plugin directory does not exist: ${SRC_PLUGIN_DIR}"
  echo "Trying to clone from GitHub..."
  cd "${GALIA_DIR}/.." \
      && git clone --recursive \
          git@github.com:${GITHUB_ORGANIZATION}/${PLUGIN}.git \
          ${PLUGIN}
fi

mvn install -DskipTests -Ddependency-check.skip=true

mkdir -p "${GALIA_PLUGIN_DIR}"

cd "${SRC_PLUGIN_DIR}" \
    && mvn clean package -DskipTests -Ddependency-check.skip=true \
    && rm -rf "${GALIA_PLUGIN_DIR}"/"${PLUGIN}"-* \
    && cp target/${PLUGIN}-*.zip "${GALIA_PLUGIN_DIR}" \
    && cd "${GALIA_PLUGIN_DIR}" \
    && unzip -q "${PLUGIN}"-*.zip \
    && rm "${PLUGIN}"-*.zip \
    && cd "${GALIA_DIR}"
