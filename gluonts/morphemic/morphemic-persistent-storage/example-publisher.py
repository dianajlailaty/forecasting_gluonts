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
destination = sys.argv[1:2] or ["/queue/static-topic-1"]
destination = destination[0]

conn = stomp.Connection(host_and_ports = [(host, port)])
#conn.start()
conn.connect(login=user,passcode=password)
metrics = ['response_time','cpu_usage','memory']
def generateMeasurement(name):
    if name == "response_time":
        return random.randint(20,100)
    elif name == 'cpu_usage':
        return random.randint(1,100)
    else:
        return random.randint(50,250)
while True:
    data = {'timestamp': time.time(),'labels':{'hostname':'localhost','application':'my_first_app','metrics':[]}}
    for metric in metrics:
        measurement = generateMeasurement(metric)
        data['labels']['metrics'].append({metric: measurement})

    conn.send(body=json.dumps(data), destination=destination, persistent='false')
    print(data)
    time.sleep(3)
    #conn.send(body=' '.join(sys.argv[1:]), destination='/queue/test')
  
conn.send("SHUTDOWN", destination=destination, persistent='false')
conn.disconnect()