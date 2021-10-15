from __future__ import print_function
import logging

import grpc

import service_pb2 as pb2
import service_pb2_grpc as gpd2

features = ['cpu_usage','memory','level','response_time','latency']

proto_list = pb2.ListOfStrings()
proto_list.strings.extend(features)

def run():
    # NOTE(gRPC Python Team): .close() is possible on a channel and should be
    # used in circumstances in which the with statement does not fit the needs
    # of the code.
    response = None 
    with grpc.insecure_channel('localhost:8767') as channel:
        stub = gpd2.PredictStub(channel)
        response = stub.trainModel(pb2.TrainRequest(application='demo', target='response_time', url_file="",features=proto_list))
    print(response)


if __name__ == '__main__':
    logging.basicConfig()
    run()

