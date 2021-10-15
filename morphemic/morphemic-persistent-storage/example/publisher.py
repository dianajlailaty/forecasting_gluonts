import sys
import os
import stomp
import json 
import random
import time 

user = os.getenv("ACTIVEMQ_USER") or "aaa"
password = os.getenv("ACTIVEMQ_PASSWORD") or "111"
host = os.getenv("ACTIVEMQ_HOST") or "localhost"
port = os.getenv("ACTIVEMQ_PORT") or 61613
application_name = os.environ.get("APPLICATION_NAME","demo")
destination = sys.argv[1:2] or ["/queue/static-topic-1"]
destination = destination[0]

connected = False 
conn = None 
while not connected:
    print("Trying to connect to ActiveMQ Broker")
    try:
        conn = stomp.Connection(host_and_ports = [(host, port)])
        conn.connect(login=user,passcode=password)
        print("Connexion established")
        connected = True 
    except:
        print("Could not establish connection to ActiveMQ Broker")
        time.sleep(10)

metrics = ['response_time','cpu_usage','memory','latency']
global_cpu_usage, global_latency, global_memory = None, None, None 

def generateMeasurement(name):
    global global_cpu_usage, global_latency, global_memory
    if name == "response_time":
        _value = random.randint(100,400)
        global_cpu_usage = 1000/_value + random.randint(1,10)
        global_latency = _value*0.29
        global_memory = 3000/_value + random.randint(1,10)
        return _value 
    elif name == 'cpu_usage':
        return global_cpu_usage
    elif name == 'latency':
        return global_latency
    else:
        return global_memory
        
while True:
    for metric in metrics:
        measurement = generateMeasurement(metric)
        #{“metricName”: “name”, “metricValue”: value, “timestamp”: time,  “application”: “applicationName”, “level”: 1} 
        data = {'timestamp': time.time(),'metricName':metric, "application":application_name,"level":1,'metricValue': measurement}
        conn.send(body=json.dumps(data), destination=destination, persistent='false')
        print(data)
    time.sleep(5)
    #conn.send(body=' '.join(sys.argv[1:]), destination='/queue/test')
  
conn.send("SHUTDOWN", destination=destination, persistent='false')
conn.disconnect()

#curl -X POST -d '{"application":"application_test","start":"10m"}' -H 'Content-type:application/json' http://localhost:8767/api/v1/make