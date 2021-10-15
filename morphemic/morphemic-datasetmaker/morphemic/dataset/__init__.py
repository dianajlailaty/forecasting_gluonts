import os, json, time 
from influxdb import InfluxDBClient
import pandas as pd 
from datetime import datetime

url_path_dataset = None 

class Row():
    def __init__(self, features,metricsname):
        self.features = features 
        if "time" in self.features:
            time_str = self.features["time"]
            _obj = datetime.strptime(time_str,'%Y-%m-%dT%H:%M:%S.%fZ')
            self.features["time"] = int(_obj.timestamp())
        if 'application' in metricsname:
            metricsname.remove('application')
        for field_name in metricsname:
            if not field_name in self.features:
                self.features[field_name] = None 

    def getTime(self):
        if "time" in self.features:
            return self.features["time"]
        if "timestamp" in self.features:
            return self.features["timestamp"]
        return None 

    def makeCsvRow(self):
        if "application" in self.features:
            del self.features["application"]
        result = ''
        for key, _value in self.features.items():
            result += "{0},".format(_value)
        return result[:-1] + "\n"

class Dataset():
    def __init__(self):
        self.rows = {}
        self.size = 0
    def addRow(self,row):
        self.rows[row.getTime()] = row 
        self.size +=1
    def reset(self):
        self.rows = {}
        self.size = 0
        print("Dataset reset")
    def getSize(self):
        return self.size 
    def sortRows(self):
        return sorted(list(self.rows.values()), key=lambda x: x.getTime(), reverse=True)
    def getRows(self):
        return list(self.rows.values())
    def getRow(self,_time, tolerance):
        for i in range(tolerance):
            if int(_time + i) in self.rows:
                return self.rows[int(_time+i)]
        return None 
    def save(self,metricnames,application_name):
        if "application" in metricnames:
            metricnames.remove("application")
        dataset_content = ''
        for metric in metricnames:
            dataset_content += "{0},".format(metric)
        dataset_content = dataset_content[:-1] + "\n"
        for row in list(self.rows.values()):
            dataset_content += row.makeCsvRow()
        _file = open(url_path_dataset + "{0}.csv".format(application_name),'w')
        _file.write(dataset_content)
        _file.close()
        return url_path_dataset + "{0}.csv".format(application_name)

class DatasetMaker():
    def __init__(self, application, start, configs):
        self.application = application
        self.start_filter = start
        self.influxdb = InfluxDBClient(host=configs['hostname'], port=configs['port'], username=configs['username'], password=configs['password'], database=configs['dbname'])
        self.dataset = Dataset()
        self.tolerance = 5
        global url_path_dataset
        url_path_dataset = configs['path_dataset']
        if url_path_dataset[-1] != "/":
            url_path_dataset += "/"

    def getIndex(self, columns, name):
        return columns.index(name)

    def makeRow(self,columns, values):
        row = {}
        index = 0
        for column in columns:
            row[column] = values[index]
            index +=1
        return row 

    def prepareResultSet(self, result_set):
        result = []
        columns = result_set["series"][0]["columns"]
        series_values = result_set["series"][0]["values"]
        index = 0
        for _values in series_values:
            row = self.makeRow(columns,_values)
            result.append(row)
        return result

    def make(self):
        try:
            self.influxdb.ping()
        except Exception as e:
            print("Could not establish connexion with InfluxDB, please verify connexion parameters")
            print(e)
            return {"message": "Could not establish connexion with InfluxDB, please verify connexion parameters"}
        if self.getData() == None:
            return {"message":"No data found"}

        metricnames, _data = self.getData()
        for _row in _data:
            row = Row(_row,metricnames)
            self.dataset.addRow(row)
        
        print("Rows construction completed")
        print("{0} rows found".format(self.dataset.getSize()))
        #self.dataset.sortRows()
        url = self.dataset.save(metricnames,self.application)
        features = self.getFeatures(url)
        if features == None:
            return {'status': False, 'message': 'An error occured while building dataset'}
        return {'status': True,'url': url, 'application': self.application, 'features': features}
        
    def getFeatures(self, url):
        try:
            df = pd.read_csv(url)
            return df.columns.to_list()
        except Exception as e:
            print("Cannot extract data feature list")
            return None 
 
    def extractMeasurement(self, _json):
        return _json["series"][0]["columns"]
        
    def getData(self):
        query = None 
        try:
            if self.start_filter != None and self.start_filter != "":
                query = "SELECT * FROM " + self.application +" WHERE time > now() - "+ self.start_filter
            else:
                query = "SELECT * FROM " + self.application 
            result_set = self.influxdb.query(query=query)
            series = self.extractMeasurement(result_set.raw)
            #self.influxdb.close() #closing connexion
            return [series, self.prepareResultSet(result_set.raw)]
        except Exception as e:
            print("Could not collect query data points")
            print(e)
            return None 
