FROM openjdk:8-jdk-alpine
LABEL maintainer "Jecstar Innovation <info@jecstar.com>"

ENV PATH /usr/share/etm/bin:$PATH
ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk

RUN addgroup -g 1000 etm && adduser -u 1000 -G etm -D -h /usr/share/etm etm

WORKDIR /usr/share/etm

RUN apk update && \                                                                                                                                                                                                                        
    apk add ca-certificates wget && \                                                                                                                                                                                                      
    update-ca-certificates && \
    wget https://www.jecstar.com/downloads/etm/etm-2.1.0.tgz && \
    wget https://www.jecstar.com/downloads/etm/etm-2.1.0.tgz.sha512 && \
    test "etm-2.1.0.tgz: OK" == "$(cat etm-2.1.0.tgz.sha512 | sha512sum -c)" && \
    tar zxf etm-2.1.0.tgz && \
    chown -R etm:etm etm-2.1.0 && \
    mv etm-2.1.0/* . && \
    rmdir etm-2.1.0 && \
    rm etm-2.1.0.tgz*

USER etm

COPY etm.yml config/

USER root
RUN chown -R etm:etm *

USER etm
CMD ["/bin/sh", "bin/etm", "console", "--quit"]

EXPOSE 8080
