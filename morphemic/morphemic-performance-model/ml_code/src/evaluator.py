import os, time, stomp, pickle, requests, json, math 
from os import path
from threading import Thread

path_ml_model = os.environ.get("MLMODELPATH",".")
#////////////////////////////////////////////////////////////////////////////
activemq_username = os.getenv("ACTIVEMQ_USER","aaa") 
activemq_password = os.getenv("ACTIVEMQ_PASSWORD","111") 
activemq_hostname = os.getenv("ACTIVEMQ_HOST","localhost")
activemq_port = int(os.getenv("ACTIVEMQ_PORT","61613")) 
persistence_storage_queue = "/queue/persistent_storage"
subscription_topic = 'performance_model_evaluator'
ps_management_queue = os.environ.get("PS_MANAGEMENT_QUEUE","persistent_storage")
#/////////////////////////////////////////////////////////////////////////////
tolerated_error = float(os.environ.get("TOLERATED_COMPARISON_ERROR","5"))
prediction_precision = int(os.environ.get("PREDICTION_PRECISION","90")) #90%
#/////////////////////////////////////////////////////////////////////////////
performance_model_train_url = os.environ.get("PERFORMANCE_MODEL_URL","http://localhost:8766/api/v1/train")


class EvaluationCandidate():
    def __init__(self, application, target, features, prediction,variant):
        self.application = application
        self.target = target
        self.features = features
        self.prediction = prediction
        self.variant = variant 
        self.real_value = None 
        self.time = time.time()

    def getApplication(self):
        return self.application
    def getTarget(self):
        return self.target
    def getFeatures(self):
        return self.features
    def getVariant(self):
        return self.variant 
    def getPrediction(self):
        return self.prediction
    def computeError(self):
        if self.real_value != None:
            return (abs(self.real_value - self.prediction)/self.real_value)*100
    def setRealValue(self,_value):
        self.real_value = _value
    def match(self,features):
        for key, _value in features.items():
            if int(_value) != int(features[key]):
                return False 
        return True 

class Listener(object):
    def __init__(self, conn,handler):
        self.conn = conn
        self.handler = handler 

    def on_error(self, headers, message):
        print('received an error %s' % message)

    def on_message(self, headers, message):
        self.handler(message)

class Evaluation(Thread):
    def __init__(self):
        self.candidates = []
        self.stop = False
        self.subscriptions = []
        self.max_candidates_size = 200
        self.real_measurement = []
        self.mean_squared_error_map = {}
        self.evaluation_period = 60*10
        self.last_evaluation = time.time()
        self.tolerated_error = tolerated_error
        self.readCandidatesFile()
        super(Evaluation,self).__init__()

    def createSubscription(self, application):
        conn = stomp.Connection(host_and_ports = [(activemq_hostname, activemq_port)])
        conn.connect(login=activemq_username,passcode=activemq_password)
        data = {'request':'subscribe','application':application,'metrics':[],'queue': subscription_topic,'name': 'performance_model'}
        conn.send(body=json.dumps(data), destination=persistence_storage_queue, persistent='false')
        print("Subscription request sent for application {0}".format(application))
        return True 

    def stopEvaluator(self):
        self.stop = True 
        self.saveCandidates()

    def handler(self, data):
        try:
            _json = json.loads(data)
            if type(_json) == type([]):
                for candidate in _json:
                    self.addCandidate(candidate['application'],candidate['target'],candidate['features'],candidate['prediction'], candidate['variant'])
                print("{0} predictions have been added".format(len(_json)))
            else:
                if "metrics" in _json:
                    self.real_measurement.append(_json)

            if time.time() - self.last_evaluation > self.evaluation_period:
                self.evaluatePrecision()
                self.last_evaluation = time.time()
        except Exception as e:
            print("An error occured while handling data from queue")
            print(e) 

    def getFeaturesFromRealMeasurment(self,_json):
        features = _json['metrics']
        features.update(_json['labels'])
        return features

    def isClosed(self, _value1, _value2):
        return abs(float(_value1) - float(_value2)) <= self.tolerated_error

    def equalFeatues(self, real_features, prediction_features):
        for key, value in prediction_features.items():
            if not key in real_features:
                return False 
            if not self.isClosed(real_features[key],value):
                return False 
        return True 
    def computeDistance(self,real_feature, predict):
        predict_feature = predict.getFeatures()
        real_prediction = real_feature[predict.getTarget()]
        prediction = predict.getPrediction()
        f_sum = 0
        for field, _value in real_feature.items():
            if not field in predict_feature:
                continue
            if type(predict_feature[field]) == type(""):
                continue
            f_sum += (float(_value) - float(predict_feature[field]))**2
        d_f = math.sqrt(f_sum)
        d_precision = (abs(real_prediction - float(prediction))/real_prediction)*100
        return (d_f,d_precision)
    def selectByApplicationName(self,data,application, _type):
        result = []
        if _type == "real":
            for real in self.real_measurement:
                if real['labels']['application'] == application:
                    result.append(real)
        else:
            for pred in self.candidates:
                if pred.getApplication() == application:
                    result.append(pred)
        return result

    def evaluatePrecision(self):
        if len(self.real_measurement) == 0:
            if len(self.candidates) > 0:
                del self.subscriptions[:]
                for candidate in self.candidates:
                    if not candidate.getApplication() in self.subscriptions:
                        self.createSubscription(candidate.getApplication())
                        self.subscriptions.append(candidate.getApplication())
                self.saveCandidates()
            print("No real data found")
            return False 
        for application in self.subscriptions:
            distance_map = {}
            self.mean_squared_error_map[application] = []
            list_real = self.selectByApplicationName(self.real_measurement,application,"real")
            list_pred = self.selectByApplicationName(self.candidates,application,"predict")
            for real in list_real:
                real_features = self.getFeaturesFromRealMeasurment(real)
                for predict in list_pred:
                    d_f, error = self.computeDistance(real_features,predict)
                    distance_map[d_f] = 100 - int(error) 

            distance_map = dict(sorted(distance_map.items()))
            #select the 10
            print("Best candidate")
            k = list(distance_map.keys())[0]
            print("Distance : {0}".format(k))
            print("Precision in percentage : {0}%".format(distance_map[k]))
            if k < prediction_precision:
                #retrain request 
                features = list(list_pred[0].getFeatures().keys())
                target = list_pred[0].getTarget()
                variant = list_pred[0].getVariant()
                application = list_pred[0].getApplication()
                _post = {'url_file': "", 'application': application,'target':target,'features': features, 'variant': variant}
                try:
                    response = requests.post(performance_model_train_url, data=json.dumps(_post),headers={'Content-Type':'application/json'})
                except Exception as e:
                    print("An error occured while sending retrain request")
            else:
                del self.real_measurement[:]
                del self.candidates[:]
            

    def listen(self):
        conn = None 
        status = False 
        while not status:
            try:
                print('Subscribe to the topic {0}'.format(subscription_topic))
                conn = stomp.Connection(host_and_ports = [(activemq_hostname, activemq_port)])
                conn.connect(login=activemq_username,passcode=activemq_password)
                conn.set_listener('', Listener(conn, self.handler))
                conn.subscribe(destination=subscription_topic, id=1, ack='auto')
                status = True 
            except Exception as e:
                print("Could not subscribe")
                print(e)
                status = False 
                time.sleep(30)

        if not status:
            time.sleep(10)
            self.listen()
            
        while not self.stop:
            time.sleep(5)
        conn.disconnect()
        self.stop = True 

    def getStatus(self):
        return not self.stop 

    def addCandidate(self,application, target, features, prediction, variant):
        candidate = EvaluationCandidate(application,target,features,prediction,variant)
        self.candidates.append(candidate)
        if len(self.candidates) > self.max_candidates_size:
            self.candidates.pop(0)
        if not application in self.subscriptions:
            self.createSubscription(application)
            self.subscriptions.append(application)
            self.saveCandidates()

    def readCandidatesFile(self):
        if path.exists(path_ml_model+"/subscriptions.obj"):
            self.subscriptions = pickle.load(open(path_ml_model+"/subscriptions.obj", 'rb'))
            for application in self.subscriptions:
                self.createSubscription(application)

    def saveCandidates(self):
        pickle.dump(self.subscriptions, open(path_ml_model+"./subscription.obj","wb"))
        print("Candidates and subscriptions struct saved")

    def restart(self):
        self.stopEvaluator()
        print("Restart in 10s")
        time.sleep(10)
        self.readCandidatesFile()
        self.run()

    def run(self):
        print("Evaluator started ...")
        self.listen()


evaluation = Evaluation()
evaluation.start()