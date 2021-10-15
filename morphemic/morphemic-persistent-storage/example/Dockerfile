FROM python:3.7
RUN pip install --upgrade pip 
RUN pip install stomp.py 
RUN mkdir /app 
COPY ./publisher.py /app 
WORKDIR /app
CMD ["python","-u","publisher.py"]