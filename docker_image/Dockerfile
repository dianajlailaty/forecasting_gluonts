FROM python:3.8.10
WORKDIR /morphemic_project
RUN apt-get clean && apt-get update -y -qq
RUN apt-get install -y --no-install-recommends apt-utils
RUN python -m pip install --upgrade pip
RUN apt-get install -y \
        build-essential \
        cmake \
        pkg-config \
        wget \
        swig \
        git \
        curl \
        unzip \
        libaio1 \
        nano \
        freetds-dev \
        unixodbc \
        unixodbc-dev \
        libjpeg-dev \
        libtiff5-dev \
        libpng-dev \
        libgtk2.0-dev \
        libavcodec-dev \
        libavformat-dev \
        libswscale-dev \
        libv4l-dev \
        libatlas-base-dev \
        gfortran \
        libhdf5-dev \
        libtbb2 \
        libtbb-dev \
        libgl1-mesa-glx
        
COPY forecasting_gluonts/docker_image/requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

ADD https://gitlab.ow2.org/melodic/morphemic-preprocessor/-/archive/morphemic-rc1.5/morphemic-preprocessor-morphemic-rc1.5.tar.gz /var/lib/morphemic/

COPY forecasting_gluonts/ ./

RUN cd /var/lib/morphemic/ \
    && tar -zxf morphemic-preprocessor-morphemic-rc1.5.tar.gz \
    && cd morphemic-preprocessor-morphemic-rc1.5 \
    && cd morphemic-datasetmaker && python3 setup.py install \ 
    && cd ../.. \
    && cp -R /var/lib/morphemic/morphemic-preprocessor-morphemic-rc1.5/amq-message-python-library /morphemic_project/messaging \
    && rm -rf /var/lib/morphemic \
    && mkdir -p /logs && mkdir /morphemic_project/models

 CMD python /morphemic_project/main.py > /logs/gluonts.log 2>&1
