import time, json, requests 

"""
curl -XPOST localhost:8086/api/v2/query -sS -H 'Accept:application/csv' -H 'Content-type:application/vnd.flux' -H 'Authorization: Token username:password' -d 'from(bucket:"telegraf")|> range(start:-5m) |> filter(fn:(r) => r._measurement == "cpu")'
"""

url = "http://localhost:8086"
username = "morphemic"
password = "password"
database = "morphemic"
application = 'application-1'
params = '-sS'

headers = {'Accept': 'application/csv', 'Content-type': 'application/vnd.flux','Authorization': 'Token '+username+':'+password}
data_post = 'from(bucket:"'+database+'")|> range(start:-5m)|> filter(fn:(r) => r._measurement == "'+application+'")'

response = requests.post(url+'/api/v2/query',data=json.dumps(data_post),headers=headers)
print(response.text)