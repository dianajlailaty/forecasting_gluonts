FROM python:3.7 
RUN pip install --upgrade pip 

COPY ./src/requirements.txt /
RUN pip install -r /requirements.txt 

ADD ./datasetlib /app/datasetlib
RUN pip install /app/datasetlib

EXPOSE 8766 8767
RUN apt-get update 

RUN mkdir -p /run/pid 
RUN mkdir -p /var/log/supervisor

RUN apt-get install -y supervisor
ADD ./src /app/
COPY ./deployment/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
WORKDIR /app 
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]