from concurrent import futures
import grpc, service_pb2_grpc, sqlite3 
from predictgrpc import PredictRPC
import time, os 

class ServerRPC():
    def __init__(self):
        self.port = 8767
        self.max_workers = 10
        #super(ServerRPC, self).__init__()

    def run(self):
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=self.max_workers))
        service_pb2_grpc.add_PredictServicer_to_server(PredictRPC(), server)
        server.add_insecure_port('[::]:{0}'.format(self.port))
        server.start()
        print("RPC server started on {0}".format(self.port))
        #server.wait_for_termination()
        while True:
            time.sleep(5)
        print("RPC server stopped normally")
            
rpc_server = ServerRPC()
rpc_server.run()