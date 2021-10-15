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
destination = sys.argv[1:2] or ["/queue/persistent_storage"]
topic = 'tester'
destination = destination[0]


print("connection established")
class Listener(object):
    def __init__(self, conn):
        self.conn = conn
  
    def on_error(self, headers, message):
        print('received an error %s' % message)

    def on_message(self, headers, message):
        print(message)

def sendSubscription():
    conn = stomp.Connection(host_and_ports = [(host, port)])
    #conn.start()
    conn.connect(login=user,passcode=password)
    data = {'request':'subscribe','application':'application_test','metrics':[],'queue': topic,'name': 'tester'}
    conn.send(body=json.dumps(data), destination=destination, persistent='false')
    print("Request sent")

def listen():
    conn = stomp.Connection(host_and_ports = [(host, port)])
    #conn.start()
    conn.connect(login=user,passcode=password)
    conn.set_listener('', Listener(conn))
    conn.subscribe(destination=topic, id=1, ack='auto')
    print("Subscription made")
    while True:
        time.sleep(2)

sendSubscription()
listen()

#curl -X POST -d '{"application":"application_test","start":"10m"}' -H 'Content-type:application/json' http://localhost:8767/api/v1/make