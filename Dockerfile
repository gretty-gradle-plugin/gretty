FROM ubuntu as base
RUN apt-get update

FROM base as base-utils
ENV _BASH_UTILS_DIR=/root/.bashrc.d
COPY <<-EOF $_BASH_UTILS_DIR/0_on_bash_ready.bash
shopt -s expand_aliases
export _on_bash_ready_i=\$(find $_BASH_UTILS_DIR -type f | wc -l)
function on_bash_ready (){
  ((_on_bash_ready_i++))
  local file="$_BASH_UTILS_DIR/\${_on_bash_ready_i}.bash"
  echo "\$@" >> \$file && \
  sed -i 's/\r\$//' \$file && \
  source \$file
}
EOF
RUN sed -i 's/\r$//' $_BASH_UTILS_DIR/0_on_bash_ready.bash
RUN echo "while read -r FILE; do source \$FILE; done < <( find $_BASH_UTILS_DIR -name '*.bash' | sort)" >> ~/.profile
SHELL ["/bin/bash", "-l", "-c"]


FROM base-utils as firefox
RUN apt-get install -y wget
RUN install -d -m 0755 /etc/apt/keyrings
RUN wget -q https://packages.mozilla.org/apt/repo-signing-key.gpg -O- | tee /etc/apt/keyrings/packages.mozilla.org.asc > /dev/null
RUN echo 'deb [signed-by=/etc/apt/keyrings/packages.mozilla.org.asc] https://packages.mozilla.org/apt mozilla main' | tee -a /etc/apt/sources.list.d/mozilla.list > /dev/null
RUN apt-get update && apt-get install -y firefox-devedition-l10n-eu
RUN ln -s /usr/bin/firefox-devedition /usr/bin/firefox


FROM firefox as firefox-sdkman
RUN apt-get install -y curl unzip zip findutils
RUN curl -s "https://get.sdkman.io?rcupdate=false" | bash
RUN on_bash_ready source /root/.sdkman/bin/sdkman-init.sh

FROM firefox-sdkman as firefox-jdk
ARG JAVA_VERSIONS="8.0.412-amzn"
ENV JAVA_VERSIONS="$JAVA_VERSIONS"
RUN on_bash_ready 'alias install_jdk="sdk install java $1"'
RUN for version in ${JAVA_VERSIONS//,/ } ; do install_jdk $version ; done


FROM firefox-jdk as firefox-jdk-gradle
ARG GRADLE_VERSION="6.9.4"
RUN sdk install gradle $GRADLE_VERSION
