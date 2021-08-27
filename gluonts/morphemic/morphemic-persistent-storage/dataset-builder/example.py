from morphemic.dataset import DatasetMaker
import os 

influxdb_hostname = os.environ.get("INFLUXDB_HOSTNAME","localhost")
influxdb_port = int(os.environ.get("INFLUXDB_PORT","8086"))
influxdb_username = os.environ.get("INFLUXDB_USERNAME","morphemic")
influxdb_password = os.environ.get("INFLUXDB_PASSWORD","password")
influxdb_dbname = os.environ.get("INFLUXDB_DBNAME","morphemic")
influxdb_org = os.environ.get("INFLUXDB_ORG","morphemic")
application_name = "demo"

_start_collection = None # '30m','1h', '2d', #None for everything
configs = {
    'hostname': influxdb_hostname, 
    'port': influxdb_port,
    'username': influxdb_username,
    'password': influxdb_password,
    'dbname': influxdb_dbname,
    'path_dataset': "./datasets"
}

datasetmaker = DatasetMaker(application_name,_start_collection,configs)
response = datasetmaker.make()