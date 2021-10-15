#!/bin/bash
python3 -m grpc_tools.protoc -I./src/protos --python_out=. --grpc_python_out=. ./src/protos/service.proto
sudo docker build -t jdtotow/performance_model -f ./deployment/Dockerfile .
sudo docker push jdtotow/performance_model 