
import time, json, requests


class Dataset:

    def __init__(self, url, port,database,application,usernaname,password):

        self._url =url
        self._port =port
        self._database =database
        self._application =application
        self._usernaname =usernaname
        self._password =password

    def count(self):


        """
        curl -XPOST localhost:8086/api/v2/query -sS -H 'Accept:application/csv' -H 'Content-type:application/vnd.flux' -H 'Authorization: Token username:password' -d 'from(bucket:"telegraf")|> range(start:-5m) |> filter(fn:(r) => r._measurement == "cpu")'
        """

        url = self._url
        username = self._port
        password = self._password
        database = self._database
        application = 'demo'
        params = '-sS'

        headers = {'Accept': 'application/csv', 'Content-type': 'application/vnd.flux','Authorization': 'Token '+username+':'+password}
        data_post = 'from(bucket:"'+database+'")|> range(start:-5m)|> filter(fn:(r) => r._measurement == "'+application+'")'

        response = requests.post(url+'/api/v2/query',data=json.dumps(data_post),headers=headers)
