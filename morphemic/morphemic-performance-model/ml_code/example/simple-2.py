import requests, json, time 

url = "http://localhost:8766"
#train model request 

url_file = '/home/jean-didier/Projects/morphemic/performance-model/ml_code/example/dataset.csv'
url_file_3 = '/home/jean-didier/Projects/morphemic/performance-model/ml_code/example/all.csv'

url_file_4 = '/var/performance_model/datasets/all.csv'

#features = ['time','served_request','request_rate','response_time','performance','cpu_usage','memory']
#features_3 = ['number','served_request','request_rate','number_instances','response_time','performance','cpu_usage','cpu_alloc','memory','memory_alloc']
#features_2 = ['cpu_usage','memory','level','response_time','latency']
features_4 = ["EstimatedRemainingTimeContext","SimulationLeftNumber","SimulationElapsedTime","NotFinishedOnTime","MinimumCoresContext","NotFinished","WillFinishTooSoonContext","NotFinishedOnTimeContext","MinimumCores","ETPercentile","RemainingSimulationTimeMetric","TotalCores"]


#post_data = {'url_file': url_file, 'application': 'application-1','target':'performance','features': features}
#post_data_2 = {'url_file': "", 'application': 'demo','target':'response_time','features': features_2, "variant": "vm"}
#post_data_3 = {'url_file': url_file_3, 'application': 'application-3','target':'performance','features': features_3}
post_data_4 = {'url_file': url_file_4, 'application': 'genome','target':'EstimatedRemainingTimeContext','features': features_4,'variant':'vm'}
#print("Get model")
#response = requests.post(url+"/api/v1/model", data='{"application":"application-2"}', headers={'Content-Type':'application/json'})
#print(response.text)


response = requests.post(url+"/api/v1/model/train", data=json.dumps(post_data_4),headers={'Content-Type':'application/json'}).text 
print("Training phase")
print(response)

#time.sleep(5)
#prediction request 

#features = {'cpu_alloc': 1 ,'memory_alloc': 64,'number_instances':4, "memory": 51086677.3333}
#features = {'cpu_usage': 31, "memory": 4500.23, 'latency': 2.1, 'level': 1}
#post_data = {'application': 'fcr','target':'response_time','features':features}
#response = requests.post(url+"/api/v1/model/predict", data=json.dumps(post_data),headers={'Content-Type':'application/json'}).text 
#print(response) 