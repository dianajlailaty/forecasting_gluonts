import os, time, json, requests, stomp, train 
from threading import Thread
#from flask import Flask, Response, request 
from initial_ML_module import Trainer, Predictor, MLModelManager
from concurrent import futures
import grpc, service_pb2_grpc, sqlite3 
from predictgrpc import PredictRPC
from morphemic.dataset import DatasetMaker
import subprocess
from pydantic import BaseModel
from typing import Optional

import uvicorn
from fastapi import FastAPI 
from fastapi.exceptions import RequestValidationError
from fastapi.responses import PlainTextResponse

dataset_local_repository = os.environ.get("DATASET_LOCAL_REPOSITORY","./datasets/")
train_folder = os.environ.get("TRAIN_DATA_FOLDER","./train")
local_database_path = os.environ.get("LOCAL_DATABASE_PATH","./db/")
influxdb_hostname = os.environ.get("INFLUXDB_HOSTNAME","localhost")
influxdb_port = int(os.environ.get("INFLUXDB_PORT","8086"))
influxdb_username = os.environ.get("INFLUXDB_USERNAME","morphemic")
influxdb_password = os.environ.get("INFLUXDB_PASSWORD","password")
influxdb_dbname = os.environ.get("INFLUXDB_DBNAME","morphemic")
influxdb_org = os.environ.get("INFLUXDB_ORG","morphemic")
#
subscription_topic = 'performance_model_evaluator'
activemq_username = os.getenv("ACTIVEMQ_USER","aaa") 
activemq_password = os.getenv("ACTIVEMQ_PASSWORD","111") 
activemq_hostname = os.getenv("ACTIVEMQ_HOST","localhost")
activemq_port = int(os.getenv("ACTIVEMQ_PORT","61613")) 

_process_stop = False 
db_evaluation_period = 60*2
db_last_evaluation = time.time()

"""
class ServerRPC(Thread):
    def __init__(self):
        self.port = 8767
        self.max_workers = 10
        super(ServerRPC, self).__init__()

    def run(self):
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=self.max_workers))
        service_pb2_grpc.add_PredictServicer_to_server(PredictRPC(), server)
        server.add_insecure_port('[::]:{0}'.format(self.port))
        server.start()
        print("RPC server started on {0}".format(self.port))
        #server.wait_for_termination()
        while True:
            if _process_stop:
                break
            time.sleep(5)
        print("RPC server stopped normally")
            
rpc_server = ServerRPC()
rpc_server.start() 
"""

"""
def DBEvaluationRoutine():
    print("DB Evaluator routine started")
    global db_last_evaluation
    while True:
        if _process_stop:
            break
        if time.time() - db_last_evaluation >= db_evaluation_period:
            try:
                conn = sqlite3.connect(local_database_path + "prediction.db")
                cursor = conn.cursor()
                data_to_send = []
                for row in cursor.execute("SELECT * FROM Prediction"):
                    _json = {'application': row[1], 'target': row[2], 'features': json.loads(row[4]), 'prediction': row[3], 'variant': row[4]}
                    data_to_send.append(_json)
                conn.close()
                if data_to_send != []:
                    conn = stomp.Connection(host_and_ports = [(activemq_hostname, activemq_port)])
                    conn.connect(login=activemq_username,passcode=activemq_password)
                    time.sleep(2)
                    conn.send(body=json.dumps(data_to_send), destination=subscription_topic, persistent='false')
                    conn.disconnect()
                    print("Messages pushed to activemq")
                    print("Removing message to Local DB")
                    conn = sqlite3.connect(local_database_path + "prediction.db")
                    cursor = conn.cursor()
                    cursor.execute("DELETE FROM Prediction")
                    conn.commit()
                    conn.close()
                    
                else:
                    print("Nothing found")
            except Exception as e:
                print("An error occured")
                print(e)
            db_last_evaluation = time.time()
        time.sleep(5)
    print("DB Evaluation stopped")

thread = Thread(target=DBEvaluationRoutine, args=())
thread.start()
"""

class TrainRequest(BaseModel):
    url_file: Optional[str] = None
    application: str 
    features: list
    target: str 
    variant: str 

class Model(BaseModel):
    application: str 
    target: str 

class PredictRequest(BaseModel):
    application: str 
    features: dict 
    target: str 
    variant: str 

url_output_api = os.environ.get("URL_OUTPUT_API","http://localhost:8767/api/v1/make")
#app = Flask(__name__)
app = FastAPI()


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    return PlainTextResponse(str(exc), status_code=400) 

@app.get('/')
def home():
    return {"Message": "Welcome to the Performance Model API", "current_version": "1.0","service_path":"/api/v1"}

@app.post('/api/v1/model/train')
async def train_model(req: TrainRequest):
    url_file, application, features, target, variant = None, None, None, None , None 
    url_file = req.url_file
    application = req.application
    features = req.features
    target = req.target
    variant = req.variant

    if url_file == None or url_file == "":
        try:
            configs = {'hostname': influxdb_hostname, 
                        'port': influxdb_port,
                        'username': influxdb_username,
                        'password': influxdb_password,
                        'dbname': influxdb_dbname,
                        'path_dataset': dataset_local_repository
            }
            datasetmaker = DatasetMaker(application,None,configs)
            response = datasetmaker.make()
            #response = requests.post(url=url_output_api,data='{"application":"'+application+'", "start":"10m"}', headers={'Content-type':'application/json'}).json()
            url_file = response['url']
            #features = response['features']
        except Exception as e:
            print(e)
            return {'status': False, 'message':'Could not create dataset','application': application}
    try:
        data = {"url_file": url_file, "target": target, "application": application, "features": features, "variant": variant}
        _file = open(train_folder + '/train.data', 'w')
        _file.write(json.dumps(data))
        _file.close()
        #trainer = Trainer(url_file,target,application,features)
        #response = {"data": trainer.train_separated_thread()}
        command = ['python','-u','train.py']
        #os.spawnlp(os.P_DETACH,*command)
        p = subprocess.Popen(command, start_new_session=True)
        return {"status": True, "message": "Training started", "application": application }
        
    except Exception as e:
        print(e)
        return {'status': False, 'message':'An error occured while training the model','application': application}

@app.post('/api/v1/model')
async def get_model(req: Model):
    application = req.application
    target = req.target

    model_manager = MLModelManager()
    return {"data": model_manager.getModelTrainingData(application, target)}
    
@app.post('/api/v1/model/predict')
def predict_model(req: PredictRequest):
    
    application = req.application
    features = req.features
    target = req.target
    variant = req.variant

    try:
        predictor = Predictor(application, features, target, variant)
        return json.loads(predictor.predict())
    except Exception as e:
        print(e)
        return {'status': False, 'message':'An error occured while training the model','application': application}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8766)

