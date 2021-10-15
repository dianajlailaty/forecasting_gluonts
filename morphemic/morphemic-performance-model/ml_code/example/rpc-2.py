from __future__ import print_function
import logging, time, random 

import grpc

import service_pb2 as pb2
import service_pb2_grpc as gpd2

#proto_list = pb2.ListOfStrings()
#proto_list.strings.extend(features)
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

def makeProtoPair(key, value):
    pair = pb2.Pair()
    pair.key = key 
    pair.value = value 
    return pair 

def run():
    # NOTE(gRPC Python Team): .close() is possible on a channel and should be
    # used in circumstances in which the with statement does not fit the needs
    # of the code.
    response = None 
    features = {'cpu_usage': 31, "memory": 230, 'latency': 2.1, 'level': 1} 
    with grpc.insecure_channel('localhost:8767') as channel:
        stub = gpd2.PredictStub(channel)
        while True:
            features_proto = pb2.Dictionary()

            features_proto.fields['cpu_usage'].float_value = features['cpu_usage']
            features_proto.fields['memory'].float_value = features['memory']
            features_proto.fields['latency'].float_value = features['latency']
            features_proto.fields['level'].float_value = features['level']
            print(features)
            print("--------------------")
            response = stub.PredictPerformance(pb2.PredictRequest(application='demo', target='response_time', features=features_proto))
            print(response)
            time.sleep(5)
            for m in metrics:
                if m == "response_time":
                    generateMeasurement(m)
                    continue
                features[m] = generateMeasurement(m)
                features[m] = generateMeasurement(m)
    


if __name__ == '__main__':
    logging.basicConfig()
    run()

