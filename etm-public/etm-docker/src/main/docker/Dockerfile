FROM adoptopenjdk/openjdk11:alpine-slim
LABEL maintainer = "Jecstar Innovation <info@jecstar.com>"

ENV PATH /usr/share/etm/bin:$PATH

WORKDIR /usr/share/etm

RUN groupadd -g 1000 etm && \
    adduser -u 1000 -G etm -D -h /usr/share/etm etm && \
    apk update && \
    apk add ca-certificates wget && \
    update-ca-certificates && \
    wget https://www.jecstar.com/downloads/etm/etm-${etm-version}.tgz && \
    wget https://www.jecstar.com/downloads/etm/etm-${etm-version}.tgz.sha512 && \
    test "etm-${etm-version}.tgz: OK" == "$(cat etm-${etm-version}.tgz.sha512 | sha512sum -c)" && \
    tar -zxf etm-${etm-version}.tgz && \
    chown -R etm:etm etm-${etm-version} && \
    mv etm-${etm-version}/* . && \
    rmdir etm-${etm-version} && \
    rm etm-${etm-version}.tgz*

COPY etm.yml config/
RUN chown -R etm:etm *

USER etm
CMD ["/bin/sh", "bin/etm", "docker"]

EXPOSE 8080
