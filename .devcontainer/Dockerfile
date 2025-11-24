FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# Instala dependências e Git LFS
RUN apt-get update && \
    apt-get install -y curl zip unzip git-lfs && \
    git lfs install

# Instala Node.js 24.x fixo e habilita pnpm via Corepack
RUN curl -fsSL https://deb.nodesource.com/setup_24.x | bash - && \
    apt-get install -y nodejs && \
    corepack enable

# Instala Docker Engine, CLI, Buildx e Compose dentro do container
RUN apt-get update && \
    apt-get install -y ca-certificates gnupg lsb-release && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc && \
    chmod a+r /etc/apt/keyrings/docker.asc && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo \"${UBUNTU_CODENAME:-$VERSION_CODENAME}\") stable" \
      > /etc/apt/sources.list.d/docker.list && \
    apt-get update && \
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin && \
    usermod -aG docker vscode

RUN apt-get update && apt-get install -y \
    libfreetype6 \
    libfreetype6-dev \
    libfontconfig1 \
    graphviz

RUN wget -O /usr/local/bin/plantuml.jar https://github.com/plantuml/plantuml/releases/download/v1.2025.10/plantuml-1.2025.10.jar

# Instala SDKMAN e configura no .bashrc
USER vscode

ENV SDKMAN_DIR="/home/vscode/.sdkman"

RUN curl -s "https://get.sdkman.io" | bash && \
    echo "source $HOME/.sdkman/bin/sdkman-init.sh" >> $HOME/.bashrc

RUN echo 'alias plantuml="java -jar /usr/local/bin/plantuml.jar"' >> $HOME/.bashrc

# Shell como bash para comandos com SDKMAN
SHELL ["bash", "-c"]

# Instala Java 21 via SDKMAN
RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    sdk install java 21.0.2-open && \
    sdk install maven && \
    sdk flush archives
    
CMD ["bash"]
