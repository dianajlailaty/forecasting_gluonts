from morphemic.dataset import DatasetMaker
import os
from filelock import FileLock

"""Script for preparing csv data downloaded form InfluxDB database, data"""


class CSVData(object):
    def __init__(self, name, start_collection=None):
        self.name = name
        self.config = {
            "hostname": os.environ.get("INFLUXDB_HOSTNAME", "localhost"),
            "port": int(os.environ.get("INFLUXDB_PORT", "8086")),
            "username": os.environ.get("INFLUXDB_USERNAME", "morphemic"),
            "password": os.environ.get("INFLUXDB_PASSWORD", "password"),
            "dbname": os.environ.get("INFLUXDB_DBNAME", "morphemic"),
            "path_dataset": os.environ.get("DATA_PATH", "./"),
        }
        self.start_collection = start_collection

    def prepare_csv(self):
        lockfile = os.path.join(self.config["path_dataset"], f"{self.name}.csv")
        lock = FileLock(lockfile + ".lock")

        if os.path.isfile(lockfile):
            with lock:
                datasetmaker = DatasetMaker(
                    self.name, self.start_collection, self.config
                )
                response = datasetmaker.make()

        else:
            datasetmaker = DatasetMaker(self.name, self.start_collection, self.config)
            response = datasetmaker.make()
