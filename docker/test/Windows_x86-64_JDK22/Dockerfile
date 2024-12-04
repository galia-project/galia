FROM mcr.microsoft.com/windows/servercore:ltsc2022-amd64

ENV JAVA_HOME="c:\jdk"
ENV MAVEN_HOME="c:\maven"

RUN setx path "%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%path%"

# Install a JDK & Maven
RUN powershell Invoke-WebRequest -Uri "https://github.com/adoptium/temurin22-binaries/releases/download/jdk-22.0.2%2B9/OpenJDK22U-jdk_x64_windows_hotspot_22.0.2_9.zip" -OutFile jdk.zip; \
    powershell Expand-Archive jdk.zip -DestinationPath . ; \
    mv jdk-22.0.2+9 %JAVA_HOME%; \
    powershell Invoke-WebRequest -Uri "https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile maven.zip; \
    powershell Expand-Archive maven.zip -DestinationPath . ; \
    mv apache-maven-3.9.6 %MAVEN_HOME%

# Copy application code
COPY --chown=galia . .

RUN mvn --quiet dependency:resolve

ENTRYPOINT mvn --batch-mode verify && mvn javadoc:javadoc
