#
# Release Dockerfile.
#
# Build using build.sh.
#
FROM photon:5.0

ARG galia_version
ARG TARGETARCH
ENV JAVA_HOME=/opt/jdk
ENV PATH=$JAVA_HOME/bin:$PATH

RUN tdnf install -y \
    dejavu-fonts \
    fontconfig \
    freetype2 \
    unzip

# Install a JRE
RUN if [ "${TARGETARCH}" = "arm64" ]; then \
        curl -OL https://github.com/adoptium/temurin22-binaries/releases/download/jdk-22.0.2%2B9/OpenJDK22U-jre_aarch64_linux_hotspot_22.0.2_9.tar.gz; \
    else \
        curl -OL https://github.com/adoptium/temurin22-binaries/releases/download/jdk-22.0.2%2B9/OpenJDK22U-jre_x64_linux_hotspot_22.0.2_9.tar.gz; \
    fi

RUN tar xfz OpenJDK*.tar.gz \
    && mkdir /opt \
    && mv jdk-22.0.2+9-jre $JAVA_HOME \
    && rm OpenJDK*.tar.gz

COPY ./target/galia-$galia_version.zip .

RUN unzip galia-$galia_version.zip \
    && mv galia-$galia_version app \
    && rm galia-$galia_version.zip

WORKDIR /app

EXPOSE 8182

CMD ["bin/start.sh"]
