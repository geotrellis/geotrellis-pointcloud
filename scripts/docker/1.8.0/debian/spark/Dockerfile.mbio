FROM quay.io/geodocker/pdal-mbio:1.8.0
MAINTAINER Grigory Pomadchin <daunnc@gmail.com>

RUN set -ex && \
    apt update -y && \
    apt install \
      openjdk-8-jdk \
      ca-certificates-java -y

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-amd64

# Install PDAL and Python deps https://tracker.debian.org/pkg/pdal
RUN apt-get -y install bash gcc g++ cmake
RUN apt-get -y install wget curl python-pip python3 python3-pip

# Install GDAL
ENV ROOTDIR /usr/local
ENV LD_LIBRARY_PATH /usr/local/lib
ENV GDAL_VERSION 2.4.0
ENV OPENJPEG_VERSION 2.3.0

# Load assets
WORKDIR $ROOTDIR/
RUN mkdir -p $ROOTDIR/src

RUN wget -qO- \
    http://download.osgeo.org/gdal/${GDAL_VERSION}/gdal-${GDAL_VERSION}.tar.gz | \
    tar -xzC $ROOTDIR/src/
RUN wget -qO- \
    https://github.com/uclouvain/openjpeg/archive/v${OPENJPEG_VERSION}.tar.gz | \
    tar -xzC $ROOTDIR/src/

RUN set -ex \
    # Runtime dependencies
    && deps=" \
       python-dev \
       python3-dev \
       python-numpy \
       python3-numpy \
       bash-completion \
       libspatialite-dev \
       libpq-dev \
       libcurl4-gnutls-dev \
       libproj-dev \
       libxml2-dev \
       libgeos-dev \
       libnetcdf-dev \
       libpoppler-dev \
       libhdf4-alt-dev \
       libhdf5-serial-dev \
    " \
    # Build dependencies
    && buildDeps=" \
       build-essential \
       cmake \
       swig \
       ant \
       pkg-config \
    "\
    && apt-get update && apt-get install -y $buildDeps $deps --no-install-recommends \
    # Compile and install OpenJPEG
    && cd src/openjpeg-${OPENJPEG_VERSION} \
    && mkdir build && cd build \
    && cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=$ROOTDIR \
    && make -j3 && make -j3 install && make -j3 clean \
    && cd $ROOTDIR && rm -Rf src/openjpeg* \
    # Compile and install GDAL
    && cd src/gdal-${GDAL_VERSION} \
    && ./configure --with-python --with-spatialite --with-pg --with-curl --with-java \
                   --with-poppler --with-openjpeg=$ROOTDIR \
    && make -j3 && make -j3 install && ldconfig \
    # Compile Python and Java bindings for GDAL
    && cd $ROOTDIR/src/gdal-${GDAL_VERSION}/swig/java && make -j3 && make -j3 install \
    && cd $ROOTDIR/src/gdal-${GDAL_VERSION}/swig/python \
    && python3 setup.py build \
    && python3 setup.py install \
    && cd $ROOTDIR && rm -Rf src/gdal* \
    # Remove build dependencies
    && apt-get purge -y --auto-remove $buildDeps \
    && rm -rf /var/lib/apt/lists/*

# Install Spark
ENV PYSPARK_PYTHON /usr/bin/python3
ENV PYSPARK_DRIVER_PYTHON /usr/bin/python3
ENV SPARK_HOME /opt/spark
ENV SPARK_CONF_DIR $SPARK_HOME/conf
ENV PATH $PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin

RUN set -x \
    && mkdir -p $SPARK_HOME $SPARK_CONF_DIR \
    && curl -sS -# http://mirror.metrocast.net/apache/spark/spark-2.4.1/spark-2.4.1-bin-hadoop2.7.tgz \
    | tar -xz -C ${SPARK_HOME} --strip-components=1

COPY ./fs /

RUN mkdir -p /data/spark

VOLUME [ "/data/spark" ]

WORKDIR "${SPARK_HOME}"
