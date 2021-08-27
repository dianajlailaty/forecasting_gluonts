FROM python:3.7 
RUN pip install --upgrade pip 
RUN pip install stomp.py requests 
RUN mkdir /app 
ADD ./src /app/
WORKDIR /app/src 
CMD ["python","-u","app.py"]
 

