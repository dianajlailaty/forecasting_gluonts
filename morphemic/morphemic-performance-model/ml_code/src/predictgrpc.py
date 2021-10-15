import service_pb2, service_pb2_grpc, os, requests, json 
from initial_ML_module import Predictor, MLModelManager, Trainer 
from morphemic.dataset import DatasetMaker
import subprocess
from google.protobuf.json_format import MessageToJson

url_output_api = os.environ.get("URL_OUTPUT_API","http://localhost:8767/api/v1/make")
dataset_local_repository = os.environ.get("DATASET_LOCAL_REPOSITORY","./datasets/")
influxdb_hostname = os.environ.get("INFLUXDB_HOSTNAME","localhost")
influxdb_port = int(os.environ.get("INFLUXDB_PORT","8086"))
influxdb_username = os.environ.get("INFLUXDB_USERNAME","morphemic")
influxdb_password = os.environ.get("INFLUXDB_PASSWORD","password")
influxdb_dbname = os.environ.get("INFLUXDB_DBNAME","morphemic")
influxdb_org = os.environ.get("INFLUXDB_ORG","morphemic")

train_folder = os.environ.get("TRAIN_DATA_FOLDER","./train")

class PredictRPC(service_pb2_grpc.PredictServicer):
    def PredictPerformance(self, request, context):
        _json = json.loads(MessageToJson(request.features))
        features = dict(_json["fields"])
        #features = {'memory': {'floatValue': 4500.23}, 'level': {'floatValue': 1.0}, 'cpu_usage': {'floatValue': 31.0}, 'latency': {'floatValue': 2.1}}
        final_features = {}
        for k, _value in features.items():
            final_features[k] = _value['floatValue']
        predictor = Predictor(request.application, final_features, request.target, request.variant)
        response = json.loads(predictor.predict())

        #dict_result = service_pb2.Dictionary()
        #dict_result.fields['results'].string_value = json.dumps(response['results'])

        #dict_ml = service_pb2.Dictionary()
        #dict_ml.fields['ml'].string_value = json.dumps(response['ml'])

        reply = service_pb2.PredictReply()
        reply.status = response['status']
        reply.results.fields['results'].string_value = json.dumps(response['results'])
        reply.ml.fields['ml'].string_value = json.dumps(response['ml'])
        reply.message = response['message']
        return reply

    def getModel(self, request, context):
        model_manager = MLModelManager()
        response = model_manager.getModelTrainingData(request.application, request.target)
        reply = service_pb2.ModelReply()
        if response != None:
            reply.results.fields['results'].string_value = json.dumps(response)
        else:
            reply.results.fields['results'].string_value = '\{"data":"No model found"\}'
        return reply 

    def trainModel(self, request, context):
        if request.url_file == "":
            try:
                configs = {'hostname': influxdb_hostname, 
                        'port': influxdb_port,
                        'username': influxdb_username,
                        'password': influxdb_password,
                        'dbname': influxdb_dbname,
                        'path_dataset': dataset_local_repository
                }
                datasetmaker = DatasetMaker(request.application,None,configs)
                response = datasetmaker.make()
                #response = requests.post(url=url_output_api,data='{"application":"'+request.application+'", "start":"10m"}', headers={'Content-type':'application/json'}).json()
                request.url_file = response['url']
                #features = response['features']
            except Exception as e:
                print(e)
                reply = service_pb2.TrainReply()
                reply.status = False 
                reply.message = str(e)
                reply.application = request.application
                return reply
        try:
            _json = json.loads(MessageToJson(request.features))
            features = list(_json["strings"])
            data = {"url_file": request.url_file, "target": request.target, "application": request.application, "features": features, "variant": request.variant}
            _file = open(train_folder + '/train.data', 'w')
            _file.write(json.dumps(data))
            _file.close()

            command = ['python','-u','train.py']
            p = subprocess.Popen(command, start_new_session=True)

            reply = service_pb2.TrainReply()
            reply.status = True 
            reply.message = "Training started"
            reply.application = request.application
            return reply
        except Exception as e:
            reply = service_pb2.TrainReply()
            reply.status = False 
            reply.message = str(e)
            reply.application = request.application
            return reply
        