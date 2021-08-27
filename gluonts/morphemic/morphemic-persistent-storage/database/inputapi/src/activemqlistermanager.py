import stomp, os, json, time
from threading import Thread

data_format = os.environ.get("DATA_FORMAT", "json")


class Listener(object):
    def __init__(self, conn, handler):
        self.conn = conn
        self.count = 0
        self.handler = handler
        self.start = time.time()

    def on_error(self, headers, message):
        print("received an error %s" % message)

    def on_message(self, headers, message):
        self.handler(message)


class Worker(Thread):
    def __init__(
        self, hostname, port, username, password, topic, handler, sleeping, index
    ):
        self.hostname = hostname
        self.port = port
        self.topic = topic
        self.handler = handler
        self.sleeping = sleeping
        self.index = index
        self.username = username
        self.password = password
        self.status = None
        self.normal_stop = False
        super(Worker, self).__init__()

    def getStatus(self):
        return self.status

    def getIndex(self):
        return self.index

    def stop(self):
        self.normal_stop = True

    def run(self):
        print("Worker {0} started".format(self.index))
        print(
            "Hostname : {0}\nPort: {1}\nTopic: {2}".format(
                self.hostname, self.port, self.topic
            )
        )
        while True:
            if self.normal_stop:
                break
            print("Trying to connect ...")
            try:
                conn = stomp.Connection(host_and_ports=[(self.hostname, self.port)])
                conn.set_listener("", Listener(conn, self.handler))
                conn.connect(login=self.username, passcode=self.password)
                conn.subscribe(destination=self.topic, id=1, ack="auto")
                self.status = "started"
                print("Waiting for messages...")
                while 1:
                    if self.normal_stop:
                        break
                    time.sleep(self.sleeping)
            except Exception as e:
                print("Could not connect to ActiveMQ broker")
                self.status = "error"
                print(e)
                time.sleep(5)
        print("End process")
        self.status = "stopped"


class ActiveMQManager:
    def __init__(self, handler):
        self.all_threads = []
        self.handler = handler
        thread_controller = Thread(target=self.workerController)
        thread_controller.start()

    def getData(self, data):
        if data_format == "json":
            _data = None
            try:
                _data = json.loads(data)
            except Exception as e:
                print("Could not decode json content")
                print("data content", data)
                return None
            self.handler(_data)

    def workerController(self):
        print("Worker controller started")
        while True:
            for w in self.all_threads:
                if w.getStatus() == "stopped" or w.getStatus() == "error":
                    w.stop()
                    print("Worker {0} will restart in 5 seconds".format(w.getIndex()))
                    time.sleep(5)
                    w.start()
            time.sleep(20)

    def startWorker(self, hostname, port, username, password, topic, key):
        for w in self.all_threads:
            if w.getIndex() == key:
                print("Connection already registered")
                return None
        sleeping = 5  # 5 seconds
        worker = Worker(
            hostname, port, username, password, topic, self.handler, sleeping, key
        )
        self.all_threads.append(worker)
        worker.start()
