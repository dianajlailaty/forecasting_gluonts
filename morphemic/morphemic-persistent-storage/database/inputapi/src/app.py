import json, time, os, requests, stomp
from flask import Flask, request, Response
from activemqlistermanager import ActiveMQManager
from influxdb import InfluxDBClient
from threading import Thread

url_api_ems = os.environ.get("URL_API_EMS", "localhost:8080")
influxdb_url = os.environ.get("INFLUXDB_URL", "http://localhost:8086")

metric_name_field_name = os.environ.get("METRIC_NAME_FIELD_NAME", "metricName")
metric_name_field_value = os.environ.get("METRIC_NAME_FIELD_VALUE", "metricValue")
metric_name_field_application = os.environ.get(
    "METRIC_NAME_FIELD_APPLICATION", "application"
)
metric_name_field_timestamp = os.environ.get("METRIC_NAME_FIELD_TIMESTAMP", "timestamp")
metric_name_field_label = os.environ.get("METRIC_NAME_FIELD_LABEL", "labels")

max_waiting_time = int(os.environ.get("MAX_WAITING_TIME", "20"))  # minutes
max_point_list = int(os.environ.get("MAX_POINT_LIST", "1000"))

influxdb_hostname = os.environ.get("INFLUXDB_HOSTNAME", "localhost")
influxdb_port = int(os.environ.get("INFLUXDB_PORT", "8086"))
influxdb_username = os.environ.get("INFLUXDB_USERNAME", "morphemic")
influxdb_password = os.environ.get("INFLUXDB_PASSWORD", "password")
influxdb_dbname = os.environ.get("INFLUXDB_DBNAME", "morphemic")
ps_management_queue = os.environ.get("PS_MANAGEMENT_QUEUE", "persistent_storage")

# "hostname": "localhost", "port": 61610, "topic": "static-topic-1", "metric": "somekey","username":"aaa","password": "111"
activemq_hostname = os.environ.get("ACTIVEMQ_HOSTNAME", "localhost")
activemq_port = int(os.environ.get("ACTIVEMQ_PORT", "61613"))
activemq_topic = os.environ.get("ACTIVEMQ_TOPIC", "static-topic-1")
activemq_subs_key = os.environ.get("ACTIVEMQ_SUBS_KEY", "subs-1")
activemq_username = os.environ.get("ACTIVEMQ_USERNAME", "aaa")
activemq_password = os.environ.get("ACTIVEMQ_PASSWORD", "111")


class Publisher(Thread):
    def __init__(self):
        self.host = activemq_hostname
        self.port = activemq_port
        self.username = activemq_username
        self.password = activemq_password
        self.queue = []
        self.conn = None
        super(Publisher, self).__init__()

    def connect(self):
        try:
            self.conn = stomp.Connection(host_and_ports=[(self.host, self.port)])
            self.conn.connect(login=self.username, passcode=self.password)
            print("Publisher is connected to ActiveMQ")

        except Exception as e:
            print("Publisher coudn't connect to ActiveMQ")
            print(e)

    def addInQueue(self, data, queue):
        self.queue.append((data, queue))

    def run(self):
        while True:
            if len(self.queue) > 0:
                try:
                    data, destination = self.queue.pop(0)
                    self.conn.send(
                        body=json.dumps(data),
                        destination=destination,
                        persistent="false",
                    )
                except Exception as e:
                    print("An exception occured while publishing")
                    print(e)
            else:
                time.sleep(1)


class Consumer:
    def __init__(self, queue, application, metrics, name):
        self.queue = queue
        self.application = application
        self.metrics = metrics  # list of metrics
        self.name = name

    def getName(self):
        return self.name

    def getQueue(self):
        return self.queue

    def getApplication(self):
        return self.application

    def getMetrics(self):
        return self.metrics

    def match(self, data):
        if self.application == "":
            if type(self.metrics) == type([]):
                if data[metric_name_field_name] in self.metrics:
                    return True
        else:
            if self.application != data[metric_name_field_application]:
                return False
            else:
                if self.metrics == []:
                    return True
                else:
                    return data[metric_name_field_name] in self.metrics


class ConsumerManager:
    def __init__(self):
        self.consumers = {}

    def addConsumer(self, queue, application, metrics, name):
        consumer = Consumer(queue, application, metrics, name)
        self.consumers[name] = consumer
        print("Consumer {0} added successfuly".format(name))

    def getConsumersByQueue(self, queue):
        result = []
        for name, consumer in self.consumers.items():
            if consumer.getQueue() == queue:
                result.append(consumer)
        return result

    def getConsumer(self, name):
        if name in self.consumers:
            return self.consumers[name]
        return None

    def getConsumersByApplication(self, application):
        result = []
        for queue, consumer in self.consumers.items():
            if consumer.getApplication() == application:
                result.append(consumer)
        return result

    def getAllConsumers(self):
        for name, consumer in self.consumers.items():
            yield consumer

    def getConsumersByMetric(self, metric):
        result = []
        for queue, consumer in self.consumers.items():
            if metric in consumer.getMetrics():
                result.append(consumer)
        return result

    def removeConsumer(self, name):
        if name in self.consumers:
            del self.consumers[name]
            return True
        return False


class Backet:
    def __init__(self, application, tolerance, _time):
        self.application = application
        self.series = {}
        self.groups = {}
        self.tolerance = tolerance
        self.initial_time = _time
        self.ready = False
        self.labels = None

    def insert(self, name, value, _time):
        self.series[name] = value

    def canBeAdded(self, _time):
        return (_time - self.initial_time) <= self.tolerance

    def setReady(self):
        self.ready = True

    def addLabels(self, labels):
        self.labels = labels

    def getLabels(self):
        return self.labels

    def getTimestamp(self):
        return self.initial_time

    def clear(self):
        self.series.clear()

    def isReady(self):
        return self.ready

    def getBacketSeries(self):
        return self.series

    def setTolerance(self, tolerance):
        self.tolerance = tolerance


class BacketManager:
    def __init__(self):
        self.backets = {}
        self.max_limit = 10

    def addBacket(self, application, backet):
        if application in self.backets:
            self.backets[application].append(backet)
        else:
            self.backets[application] = [backet]

    def getBacketBasedOnTime(self, application, _time):
        backets = self.getBackets(application)
        if backets == None:
            return None
        for backet in backets:
            if backet.canBeAdded(_time):
                return backet
        return None

    def getBackets(self, application):
        if application in self.backets:
            return self.backets[application]
        else:
            return None

    def popBacket(self, application):
        if len(self.backets[application]) >= self.max_limit:
            return self.backets[application].pop(0)
        return None

    def getAllSeries(self):
        for application, backet in self.backets.items():
            yield backet.getBacketSeries()

    def trunkListBackets(self):
        for application in self.backets:
            while len(self.backets[application]) >= self.max_limit:
                self.backets[application].pop(0)
            print(len(self.backets[application]))


class ToleranceDetector:
    def __init__(self):
        self.tolerances = {}
        self.app_times = {}
        self.limit = 100

    def addTime(self, application, _time):
        if application in self.app_times:
            if len(self.app_times[application]) >= self.limit:
                self.app_times[application].pop(0)
            self.app_times[application].append(_time)
        else:
            self.app_times[application] = [_time]
        self.updateTolerance()

    def findMaxInterval(self, _list):
        max_interval = 0
        index = 0
        for el in _list:
            if index == len(_list) - 1:
                return max_interval
            interval = abs(el - _list[index + 1])
            if interval > max_interval:
                max_interval = interval
            index += 1
        return max_interval

    def getTolerance(self, application):
        if application in self.tolerances:
            return self.tolerances[application]
        else:
            return None

    def updateTolerance(self):
        if len(self.app_times.keys()) == 0:
            return False
        for application, _list_time in self.app_times.items():
            self.tolerances[application] = self.findMaxInterval(_list_time)


class Subscription:
    def __init__(self):
        self.hostname = None
        self.port = None
        self.topic = None
        self.metric = None
        self.username = None
        self.password = None

    def setHostname(self, hostname):
        self.hostname = hostname

    def setPort(self, port):
        self.port = port

    def setTopic(self, topic):
        self.topic = topic

    def setMetric(self, metric):
        self.metric = metric

    def setUsername(self, username):
        self.username = username

    def setPassword(self, password):
        self.password = password

    def getHostname(self):
        return self.hostname

    def getPort(self):
        return self.port

    def getTopic(self):
        return self.topic

    def getMetric(self):
        return self.metric

    def getPassword(self):
        return self.password

    def getUsername(self):
        return self.username

    def load(self, _json):
        try:
            self.setHostname(_json["hostname"])
            self.setPort(_json["port"])
            self.setTopic(_json["topic"])
            self.setMetric(_json["key"])
            self.setUsername(_json["username"])
            self.setPassword(_json["password"])
        except Exception as e:
            print("Could not load json content")
            print(e)
            print("json content", _json)


class Ingestor(Thread):
    def __init__(self, hostname, port, username, password, database):
        self.influxdb = (
            None  # InfluxDBClient(url=influxdb_url, token="my-token", org="morphemic")
        )
        self.hostname = hostname
        self.port = port
        self.username = username
        self.password = password
        self.database = database
        self.list_content = []
        self.last_insertion = time.time()
        self.insert_interval = 20
        self.evaluation_time = time.time()
        self.data_points = 0
        self.data_points_inserted = 0
        self.data_points_error = 0
        self.rate = 0
        self.evaluation_interval = 10
        self.scrape_interval_by_application = {}
        self.backer_manager = BacketManager()
        self.last_update_list_backet = time.time()
        self.interval_update_list_backets = 60 * 5
        # self.tolerance_manager = ToleranceDetector()
        super(Ingestor, self).__init__()

    def detectScrapeInterval(self, _json):
        # {'timestamp': time.time(),'name':metric,'labels':{'hostname':'localhost','application':'application-1'},'value': measurement}
        pass

    def addToList(self, content):
        self.list_content.append(content)

    def run(self):
        self.connect()
        while True:
            size = len(self.list_content)
            for content in self.list_content:
                if self.insert(content):
                    self.data_points_inserted += 1
                else:
                    self.data_points_error += 1
                self.data_points += 1
            del self.list_content[:size]
            time.sleep(1)

    def connect(self):
        while True:
            print("Try to connect to InfluxDB")
            try:
                self.influxdb = InfluxDBClient(
                    host=self.hostname,
                    port=self.port,
                    username=self.username,
                    password=self.password,
                    database=self.database,
                )
                databases = self.influxdb.get_list_database()
                print(databases)
                db_exist = False
                for db in databases:
                    if db["name"] == self.database:
                        print("Database {0} found".format(self.database))
                        db_exist = True
                if not db_exist:
                    print("Database {0} will be created".format(self.database))
                    self.influxdb.create_database(self.database)
                    self.influxdb.switch_database(self.database)
                    print(self.influxdb.get_list_database())
                print("Connection to InfluxDB established")
                break
            except Exception as e:
                print("Could not connect to InfluxDB")
                print(e)
                time.sleep(5)

    def insert(self, content):
        # {'timestamp': time.time(),'name':metric,'labels':{'hostname':'localhost','application':'application-1'},'value': measurement}
        # {“metricName”: “name”, “metricValue”: value, “timestamp”: time,  “application”: “applicationName”, “level”: 1…}
        fields = None
        try:
            fields = json.loads(content)
        except Exception as e:
            print("Cannot decode json")
            print("content", content)
            return False
        # self.tolerance_manager.addTime(fields["application"], fields["timestamp"])
        application = fields[metric_name_field_application]
        timestamp = fields[metric_name_field_timestamp]
        metric = fields[metric_name_field_name]
        value = fields[metric_name_field_value]

        backet = self.backer_manager.getBacketBasedOnTime(application, timestamp)

        if backet != None:
            backet.insert(metric, value, timestamp)
            backet.addLabels({"application": application, "level": fields["level"]})
            # tolerance = self.tolerance_manager.getTolerance(fields["application"])
            # if tolerance != None:
            #    backet.setTolerance(tolerance)
        else:
            backet = Backet(application, 2, timestamp)
            self.backer_manager.addBacket(application, backet)
            backet.insert(metric, value, timestamp)

        backet = self.backer_manager.popBacket(application)
        if backet != None:
            metrics = backet.getBacketSeries()
            timestamp = backet.getTimestamp()
            labels = backet.getLabels()
            self.insert2({"metrics": metrics, "labels": labels, "timestamp": timestamp})

    def insert2(self, data):
        # {
        #   "timestamp": 1608125957.8976443,
        #   "labels":
        #       {"hostname": "localhost", "application": "my_first_app"},
        #   "metrics": { "response_time": 42, "cpu_usage": 3 , "memory": 74 }
        # }
        point = {
            "measurement": data["labels"]["application"],
            "fields": data["metrics"],
            "tags": data["labels"],
            "timestamp": data["timestamp"],
        }
        try:
            return self.influxdb.write(
                {"points": [point]}, {"db": self.database}, 204, "json"
            )
        except Exception as e:
            print("An Error occur while inserting data point")
            print(e)
            print("content", point)
            return False


class InputApi:
    def __init__(
        self,
        influxdb_hostname,
        influxdb_port,
        influxdb_username,
        influxdb_password,
        influxdb_database,
    ):
        self.consumer_manager = None
        self.subscriptions = {}
        self.consumers = {}
        self.ingestor = Ingestor(
            influxdb_hostname,
            influxdb_port,
            influxdb_username,
            influxdb_password,
            influxdb_database,
        )
        self.data_points = 0
        self.last_evaluation = time.time()
        self.consumer_controller = ConsumerManager()
        self.evaluation_interval = 5
        self.publisher = Publisher()

    def getActiveMQParameters(self):
        conns = [
            {
                "hostname": activemq_hostname,
                "port": activemq_port,
                "topic": activemq_topic,
                "key": activemq_subs_key,
                "username": activemq_username,
                "password": activemq_password,
            },
            {
                "hostname": activemq_hostname,
                "port": activemq_port,
                "topic": activemq_topic,
                "key": activemq_subs_key + "-2",
                "username": activemq_username,
                "password": activemq_password,
            },
            {
                "hostname": activemq_hostname,
                "port": activemq_port,
                "topic": activemq_topic,
                "key": activemq_subs_key + "-3",
                "username": activemq_username,
                "password": activemq_password,
            },
            {
                "hostname": activemq_hostname,
                "port": activemq_port,
                "topic": ps_management_queue,
                "key": "management",
                "username": activemq_username,
                "password": activemq_password,
            },
        ]
        return conns

    def saveSubscriptions(self, data):
        if type(data) != type([]):
            print("Error data type")
            print(type(data))
            return None
        for _json in data:
            subs = Subscription()
            subs.load(_json)
            print(
                "Subscription hostname : {0}, port : {1}, topic: {2}, metric: {3} added".format(
                    _json["hostname"], _json["port"], _json["topic"], _json["key"]
                )
            )
            self.subscriptions[subs.getMetric()] = subs

    def prepareDatasetRequest(self, _json):
        pass

    def getSubscriptions(self):
        return self.subscriptions

    def startConsuming(self):
        for key in self.subscriptions.keys():
            if not key in self.consumers:
                subs = self.subscriptions[key]
                self.consumer_manager.startWorker(
                    subs.getHostname(),
                    subs.getPort(),
                    subs.getUsername(),
                    subs.getPassword(),
                    subs.getTopic(),
                    key,
                )

    def handleSubscriptions(self, data):
        try:
            _json = json.loads(data)
            consumers = self.consumer_controller.getAllConsumers()
            for consumer in consumers:
                if consumer.match(_json):
                    self.publisher.addInQueue(data, consumer.getQueue())
        except Exception as e:
            print("Error in handle subscription")
            print(e)

    def handleRequest(self, _json):
        if _json["request"] == "subscribe":
            if not "name" in _json:
                print("Subscription name must be provided")
                return False
            if not "queue" in _json:
                print("Missing queue field")
                return False
            if not "application" in _json and not "metrics" in _json:
                print("Field application or metrics must be provided")
                return False
            self.consumer_controller.addConsumer(
                _json["queue"], _json["application"], _json["metrics"], _json["name"]
            )
        if _json["request"] == "unsubscribe":
            if not "name" in _json:
                print("Subscription name must be provided")
                return False
            self.consumer_controller.removeConsumer(_json["name"])

        if _json["request"] == "make_dataset":
            if not "application" in _json:
                print("Application name missing")
                return False
            if not "queue" in _json:
                print("Queue missing in the request")
                return False
            self.prepareDatasetRequest(self, _json)

    def start(self):
        # ws = FlaskAppWrapper(__name__)
        # ws.add_endpoint(endpoint='/home', endpoint_name='home', handler=home)
        # ws.start()
        self.publisher.connect()
        self.publisher.start()
        self.ingestor.start()
        self.consumer_manager = ActiveMQManager(self.getData)
        self.saveSubscriptions(self.getActiveMQParameters())
        self.startConsuming()

    def getSubscriberSize(self):
        return len(self.subscriptions.keys())

    def getData(self, data):
        try:
            _json = json.loads(data)
            if "request" in _json:
                print(_json)
                self.handleRequest(_json)
                return True
        except Exception as e:
            print(e)
            print("Non JSON content received")
            return None
        self.ingestor.addToList(data)
        self.data_points += 1
        if time.time() - self.last_evaluation > self.evaluation_interval:
            rate = int(self.data_points / self.evaluation_interval)
            print("DP rate: {0} dp/sec, total dp: {1}".format(rate, self.data_points))
            self.data_points = 0
            self.last_evaluation = time.time()
        self.handleSubscriptions(data)


class EndpointAction(object):
    def __init__(self, action):
        self.action = action

    def __call__(self, *args):
        response = self.action()
        return Response(response, status=200, mimetype="application/json")


class FlaskAppWrapper(Thread):
    app = None

    def __init__(self, name):
        self.app = Flask(name)
        super(FlaskAppWrapper, self).__init__()

    def run(self):
        self.app.run()

    def add_endpoint(self, endpoint=None, endpoint_name=None, handler=None):
        self.app.add_url_rule(endpoint, endpoint_name, EndpointAction(handler))


def home():
    return "Welcome to Input API WS"


if __name__ == "__main__":
    api = InputApi(
        influxdb_hostname,
        influxdb_port,
        influxdb_username,
        influxdb_password,
        influxdb_dbname,
    )
    api.start()