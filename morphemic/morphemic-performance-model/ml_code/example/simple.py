import requests, json, time 

url = "http://localhost:8766"
#train model request 

url_file = '/home/jean-didier/Projects/morphemic/performance-model/ml_code/example/dataset.csv'
#url_file_3 = '/home/jean-didier/Projects/morphemic/performance-model/ml_code/example/all.csv'

features = ['time','served_request','request_rate','response_time','performance','cpu_usage','memory']
#features_3 = ['number','served_request','request_rate','number_instances','response_time','performance','cpu_usage','cpu_alloc','memory','memory_alloc']
#features_2 = ['time','cpu_usage','memory']


post_data = {'url_file': url_file, 'application': 'application-1','target':'performance','features': features}
#post_data_2 = {'url_file': "", 'application': 'application-2','target':'response_time','features': features_2}
#post_data_3 = {'url_file': url_file_3, 'application': 'application-3','target':'performance','features': features_3}
#print("Get model")

#response = requests.post(url+"/api/v1/model", data='{"application":"application-1"}', headers={'Content-Type':'application/json'})
#print(response.text)



#response = requests.post(url+"/api/v1/model/train", data=json.dumps(post_data),headers={'Content-Type':'application/json'}).text 
#print("Training phase")
#print(response)

#time.sleep(5)
#prediction request 
#'time','served_request','request_rate','response_time','performance','cpu_usage','memory'


features = {'served_request': 267, 'request_rate': 60,'time':1602538627.766, 'response_time': 2, 'cpu_usage': 31, "memory": 51086677.3333}
post_data = {'application': 'application-1','target':'performance','features':features}
response = requests.post(url+"/api/v1/model/predict", data=json.dumps(post_data),headers={'Content-Type':'application/json'}).text 
print(response) 