import os, time, itertools, pickle, autosklearn, sklearn, json, logging
from os import path 
import numpy as np
import pandas as pd
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.preprocessing import OrdinalEncoder
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeRegressor
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import r2_score, mean_absolute_error, mean_squared_error
import autosklearn.regression
from sklearn.svm import SVR
from sklearn.neighbors import KNeighborsRegressor
from sklearn.model_selection import RandomizedSearchCV
from threading import Thread
import sqlite3

path_ml_model = os.environ.get("MLMODELPATH","./models")
log_folder = os.environ.get("LOGS_FOLDER","./logs")
time_sklearn_training = int(os.environ.get("SKLEARNTIME","600")) #in seconde
local_database_path = os.environ.get("LOCAL_DATABASE_PATH","./db/")

logFile = log_folder + '/ml.log'
logger = logging.getLogger(__name__)

# Create handlers
f_handler = logging.FileHandler(logFile)
f_handler.setLevel(logging.DEBUG)

# Create formatters and add it to handler
f_format = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
f_handler.setFormatter(f_format)

# Add handlers to the logger
logger.addHandler(f_handler)

class MLModel():
    def __init__(self,application, target):
        self.application = application
        self.target = target
        self.training_data = None 
    def setTrainingData(self, training_data):
        self.training_data = training_data
    def getTrainingData(self):
        return self.training_data

class MLModelManager():
    def __init__(self):
        self.ml_model_registry = {}
        self.readModels()

    def addModel(self,application,target,training_data):
        model = MLModel(application, target)
        model.setTrainingData(training_data)
        self.ml_model_registry[application+target] = model 
        logger.info("-------------Training Info--------------")
        logger.info(training_data)
        logger.info("-------------End Training data----------")
        self.saveModels()

    def getModelTrainingData(self, application, target):
        if application+target in self.ml_model_registry:
            return self.ml_model_registry[application+target].getTrainingData()
        else:
            return None 

    def readModels(self):
        if path.exists(path_ml_model+"/models.obj"):
            self.ml_model_registry = pickle.load(open(path_ml_model+"/models.obj", 'rb'))
            logger.info("Models obj found and loaded")

    def saveModels(self):
        pickle.dump(self.ml_model_registry, open(path_ml_model+"/models.obj", 'wb'))
        logger.info("Models saved")

class Pipeline():
    def __init__(self, functions):
        self.functions = functions 
    def execute(self, data):
        for f in self.functions:
            data = f(data)
        return data 

class Trainer():
    def __init__(self, url_file, target, application, features_list, variant):
        self.url_file = url_file
        self.target = target
        self.features_list = features_list
        self.features_list.sort()
        self.application = application
        self.ml_file_link = None
        self.variant = variant
        self.model_manager = MLModelManager()
        
    def load_data(self):
        # Check if we received a file that does not exist
        if os.path.isfile(self.url_file):
            pass
        else:
            logger.critical("Sorry, file does not exists.")
            raise ValueError("Sorry, file does not exists.")
        name, extension = os.path.splitext(self.url_file)
        # check if it is not a csv file
        if extension != '.csv':
            logger.critical("Oops!  That was no a csv file.  Try again...")
            raise ValueError("Oops!  That was no a csv file.  Try again...")
        else:
            pass
        # check if it is an empty file
        if os.stat(self.url_file).st_size == 0:
            logger.critical("Sorry!  That file is empty.  Try again...")
            raise ValueError("Sorry!  That file is empty.  Try again...")
        else:
            pass
        # Load the file
        data = pd.read_csv(self.url_file)
        data = data[self.features_list]
        if data.iloc[0].values[1] == 'Unnamed 0':
            data = data.drop(columns=['Unnamed 0'])
        if type(data.iloc[0].values[1]) == str:
            comparison = data.iloc[0].values == data.columns.values
            if comparison.all() == True:
                return data.iloc[1:]
            else:
                return data
        else:
            return data

    def choose_variable_based_on_importance(self):
        pass

    def missing_values_imputer(self, data):
        # data = self.load_data()

        # TODO ---> make a configuration column
        # TODO ---> use the missing values imputation based on configuration

        if data.isna().any().any() == False:
            pass
        elif data.isna().any().any() == True:
            names = data.columns
            # example
            imp = SimpleImputer(missing_values=np.nan, strategy='mean')
            imp.fit(data.values)
            data = imp.transform(data.values)
            data = pd.DataFrame(data, columns=names)
        return data

    def normalization(self, data):
        # data = self.load_data()
        scaler = StandardScaler()
        data_target = data[self.target]
        data = data.drop(columns=[self.target])
        names = data.columns
        scaler.fit(data)
        data = scaler.transform(data)
        data = pd.DataFrame(data, columns=names)
        data[self.target] = data_target
        return data

    def encode_categorical_values(self, data):
        # data = self.load_data()
        a = []
        j = 0
        for i in data.dtypes:
            if i == np.dtype('str') or i == np.dtype('object'):
                a.append(data.dtypes.index[j])
                j += 1
            elif i != np.dtype('str') or i != np.dtype('object'):
                j += 1

        # TODO ----> I shall add more encoder functions, not only Ordinal Encoder

        oe = OrdinalEncoder()
        categorical_data = data[a]
        names = categorical_data.columns
        oe.fit(categorical_data)
        categorical_data = oe.transform(categorical_data)
        categorical_data = pd.DataFrame(categorical_data, columns=names)
        # replace categorical with encoded data
        for i in a:
            data[i] = categorical_data[i].replace(',', '-')

        return data

    def train_and_test_split(self, data):
        # data = self.load_data()
        X_train, X_test, y_train, y_test = train_test_split(data.drop(columns=[self.target]),data[self.target], test_size=0.30)
        return X_train, X_test, y_train, y_test
        
    def train_separated_thread(self):
        thread = Thread(target=self.train)
        thread.start()
        return {"status": True, "message": "Training started", "application": self.application }

    def train(self):
        data = self.load_data()
        logger.info("Loading ...")
        print("Loading")
        
        pipeline = Pipeline([self.missing_values_imputer,self.encode_categorical_values,self.normalization])
        data = pipeline.execute(data)
        data = data.dropna()
        #print(data)
        # TODO ----> I shall put also here all possible regressor that I want to have

        # RandomForestRegressor
        _start = time.time()
        logger.info("Random Forest Regressor")
        self.regressor = RandomForestRegressor(random_state=0)
        X_train, X_test, y_train, y_test = self.train_and_test_split(data)
        self.regressor.fit(X_train, y_train)
        # cross_val_score(regressor, X_train, y_train)
        pred = self.regressor.predict(X_test)
        pred_train = self.regressor.predict(X_train)

        # Reporting for RandomForestRegressor
        mse = mean_squared_error(y_test, pred, squared=True)
        me = mean_squared_error(y_test, pred, squared=False)
        r2_test = r2_score(y_test, pred)
        r2_train = r2_score(y_train, pred_train)
        mae = mean_absolute_error(y_test, pred)
    
        # R2_test
        logger.info('R2_test: {0}'.format(r2_test))
        # R2_train
        logger.info('R2_train: {0}'.format(r2_train))
        # MSE
        logger.info('Mean Squared Error: {0}'.format(mse))
        # RMSE
        logger.info('Mean Error: {0}'.format(me))
        # MAE
        logger.info('Mean Absolute Error: {0}'.format(mae))
        logger.info('--------------------------------------\n')
        logger.info("RandomizedSearchCV RandomForestRegressor")
        rnd_f_regressor_training_duration = time.time() - _start
        # RandomizedSearchCV RandomForestRegressor
        # Number of trees in random forest
        n_estimators = [int(x) for x in np.linspace(start=200, stop=2000, num=10)]
        # Number of features to consider at every split
        max_features = ['auto', 'sqrt']
        # Maximum number of levels in tree
        max_depth = [int(x) for x in np.linspace(10, 110, num=11)]
        max_depth.append(None)
        # Minimum number of samples required to split a node
        min_samples_split = [2, 5, 10]
        # Minimum number of samples required at each leaf node
        min_samples_leaf = [1, 2, 4]
        # Method of selecting samples for training each tree
        bootstrap = [True, False]

        # Create the random grid
        random_grid = {'n_estimators': n_estimators,'max_features': max_features,'max_depth': max_depth,'min_samples_split': min_samples_split,'min_samples_leaf': min_samples_leaf,'bootstrap': bootstrap}
        # Use the random grid to search for best hyperparameters
        # First create the base model to tune
        rf = RandomForestRegressor()
        # Random search of parameters, using 3 fold cross validation,
        # search across 100 different combinations, and use all available cores
        self.rf_random = RandomizedSearchCV(estimator=rf, param_distributions=random_grid, n_iter=100, cv=3, verbose=2,random_state=42, n_jobs=-1)
        # Fit the random search model
        self.rf_random.fit(X_train, y_train)
        pred_rf_random = self.rf_random.predict(X_test)
        pred_train_rf_random = self.rf_random.predict(X_train)

        # Reporting for RandomForestRegressor
        mse_rf_random = mean_squared_error(y_test, pred_rf_random, squared=True)
        me_rf_random = mean_squared_error(y_test, pred_rf_random, squared=False)
        r2_test_rf_random = r2_score(y_test, pred_rf_random)
        r2_train_rf_random = r2_score(y_train, pred_train_rf_random)
        mae_rf_random = mean_absolute_error(y_test, pred_rf_random)

        # R2_test
        logger.info('R2_test_rf_test: {0}'.format(r2_test_rf_random))
        # R2_train
        logger.info('R2_train_rf_train: {0}'.format(r2_train_rf_random))
        # MSE
        logger.info('Mean Squared Error RF_random: {0}'.format(mse_rf_random))
        # RMSE
        logger.info('Mean Error RF_random: {0}'.format(me_rf_random))
        # MAE
        logger.info('Mean Absolute Error RF_random: {0}'.format(mae_rf_random))
        logger.info('--------------------------------------\n')
        # SVR
        logger.info("SVR Regressor")
        self.regressor_2 = SVR()
        self.regressor_2.fit(X_train, y_train)
        # cross_val_score(self.regressor_2, X_train, y_train)
        pred_2 = self.regressor_2.predict(X_test)
        pred_train_2 = self.regressor_2.predict(X_train)

        # Reporting for SVR
        mse_2 = mean_squared_error(y_test, pred_2, squared=True)
        me_2 = mean_squared_error(y_test, pred_2, squared=False)
        r2_test_2 = r2_score(y_test, pred_2)
        r2_train_2 = r2_score(y_train, pred_train_2)
        mae_2 = mean_absolute_error(y_test, pred_2)

        # R2_test
        logger.info('R2_test_2: {0}'.format(r2_test_2))
        # R2_train
        logger.info('R2_train_2: {0}'.format(r2_train_2))
        # MSE
        logger.info('Mean Squared Error 2: {0}'.format(mse_2))
        # RMSE
        logger.info('Mean Error 2: {0}'.format(me_2))
        # MAE
        logger.info('Mean Absolute Error 2: {0}'.format(mae_2))
        logger.info('--------------------------------------\n')
        # KNeighborsRegressor
        logger.info("KNeighbors Regressor")
        self.regressor_KNN = KNeighborsRegressor()
        self.regressor_KNN.fit(X_train, y_train)
        # cross_val_score(self.regressor_KNN, X_train, y_train)
        pred_KNN = self.regressor_KNN.predict(X_test)
        pred_train_KNN = self.regressor_KNN.predict(X_train)

        # Reporting for KNN
        mse_KNN = mean_squared_error(y_test, pred_KNN, squared=True)
        me_KNN = mean_squared_error(y_test, pred_KNN, squared=False)
        r2_test_KNN = r2_score(y_test, pred_2)
        r2_train_KNN = r2_score(y_train, pred_train_KNN)
        mae_KNN = mean_absolute_error(y_test, pred_KNN)

        # R2_test
        logger.info('R2_test_KNN: {0}'.format(r2_test_KNN))
        # R2_train
        logger.info('R2_train_KNN: {0}'.format(r2_train_KNN))
        # MSE
        logger.info('Mean Squared Error KNN: {0}'.format(mse_KNN))
        # RMSE
        logger.info('Mean Error KNN: {0}'.format(me_KNN))
        # MAE
        logger.info('Mean Absolute Error KNN: {0}'.format(mae_KNN))
        logger.info('--------------------------------------\n')
        # AutoSKlearn
        logger.info("Auto SK learn")
        self.automl = autosklearn.regression.AutoSklearnRegressor(time_left_for_this_task = time_sklearn_training)  ### here we can specify the minutes we want out take the automl for training
        self.automl.fit(X_train, y_train)
        pred_automl = self.automl.predict(X_test)
        pred_train_automl = self.automl.predict(X_train)
        # Reporting for AutoML
        mse_automl = mean_squared_error(y_test, pred_automl, squared=True)
        me_automl = mean_squared_error(y_test, pred_automl, squared=False)
        r2_test_automl = r2_score(y_test, pred_automl)
        r2_train_automl = r2_score(y_train, pred_train_automl)
        mae_automl = mean_absolute_error(y_test, pred_automl)

        # R2 test
        logger.info("R2 score automl: {0}".format(r2_test_automl))
        # R2_train
        logger.info('R2_train_automl: {0}'.format(r2_train_automl))
        # MSE
        logger.info('Mean Squared Error automl: {0}'.format(mse_automl))
        # RMSE
        logger.info('Mean Error automl: {0}'.format(me_automl))
        # MAE
        logger.info('Mean Absolute Error automl: {0}'.format(mae_automl))

        # Choose between two or more algorithms
        #my_dict = {'mse': mse, 'mse_2': mse_2, 'mse_KNN': mse_KNN, 'mse_automl': mse_automl, 'mse_rf_random' : mse_rf_random}
        #my_dict = {'mse': mse, 'mse_2': mse_2, 'mse_KNN': mse_KNN, 'mse_automl' : mse_automl}
        my_dict = {'mse': mse, 'mse_2': mse_2, 'mse_KNN': mse_KNN, 'mse_rf_random' : mse_rf_random, 'mse_automl': mse_automl}
        mse_min = min(my_dict.keys(), key=(lambda k: my_dict[k]))
        
        if mse_min == 'mse':
            # saving model to file
            self.ml_file_link = self.application + self.target + self.variant + '.sav'
            pickle.dump(self.regressor, open(path_ml_model + '/' + self.ml_file_link, 'wb'))
            training_data = {'algorithm':'random forest','r2_test': r2_test, 'r2_train': r2_train, 'mean_error': me,'dataset_url': self.url_file,'feature_list': self.features_list , 'mean_squared_error': mse, 'mean_absolute_error': mae}
            self.model_manager.addModel(self.application,self.target,training_data)
            return training_data

        elif mse_min == 'mse_2':
            # saving model to file
            self.ml_file_link = self.application + self.target + self.variant + '.sav'
            pickle.dump(self.regressor_2, open(path_ml_model + '/' + self.ml_file_link, 'wb'))
            training_data = {'algorithm':'svr','r2_test_2': r2_test_2,'dataset_url': self.url_file, 'r2_train_2': r2_train_2,'feature_list': self.features_list, 'mean_error_2': me_2, 'mean_squared_error_2': mse_2, 'mean_absolute_error_2': mae_2}
            self.model_manager.addModel(self.application,self.target,training_data)
            return training_data

        elif mse_min == 'mse_KNN':
            # saving model to file
            self.ml_file_link = self.application + self.target + self.variant + '.sav'
            pickle.dump(self.regressor_KNN, open(path_ml_model + '/' + self.ml_file_link, 'wb'))
            training_data = {'algorithm':'KNN','r2_test_KNN': r2_test_KNN, 'r2_train_KNN': r2_train_KNN,'dataset_url': self.url_file,'feature_list': self.features_list, 'mean_error_KNN': me_KNN, 'mean_squared_error_KNN': mse_KNN, 'mean_absolute_error_KNN': mae_KNN}
            self.model_manager.addModel(self.application,self.target,training_data)
            return training_data

        elif mse_min == 'mse_rf_random':
            # saving model to file
            self.ml_file_link = self.application + self.target + self.variant + '.sav'
            pickle.dump(self.automl, open(path_ml_model + '/' + self.ml_file_link, 'wb'))
            training_data = {'algorithm':'CV Random Forest','r2_test_rf_random': r2_test_rf_random, 'r2_train_rf_random': r2_train_rf_random,'feature_list': self.features_list,'dataset_url': self.url_file,'mean_error_rf_random': me_rf_random, 'mean_squared_error_rf_random': mse_rf_random,'mean_absolute_error_rf_random': mae_rf_random}
            self.model_manager.addModel(self.application,self.target,training_data)
            return training_data 

        elif mse_min == 'mse_automl':
            # saving model to file
            self.ml_file_link = self.application + self.target + self.variant + '.sav'
            pickle.dump(self.automl, open(path_ml_model + '/' + self.ml_file_link, 'wb'))
            training_data = {'algorithm':'automl','r2_test_automl': r2_test_automl, 'r2_train_automl': r2_train_automl,'mean_error_automl': me_automl,'feature_list': self.features_list,'dataset_url': self.url_file, 'mean_squared_error_automl': mse_automl, 'mean_absolute_error_automl': mae_automl}
            self.model_manager.addModel(self.application,self.target,training_data)
            return training_data 

        else:
            print("No candidate found")
        

    def getMLFile(self):
        self.train()
        # To filename να ειναι το ονομα της εφαρμογης
        filename = self.application + self.target + self.variant + '.sav'
        #pickle.dump(self.regressor, open(filename, 'wb'))
        self.ml_file_link = pickle.load(open(filename, 'rb'))
        path = os.path.dirname(os.path.realpath('self.ml_file_link'))
        # Now we can choose either to return the file or the path where the file it is stored
        return {'file': self.ml_file_link, 'application': self.application, 'target': self.target}

class Predictor():
    def __init__(self, application, features_dict, target, variant):
        self.application = application
        self.features_dict = features_dict
        self.features_dict = dict(sorted(self.features_dict.items()))
        self.target = target
        self.prediction = None
        self.variant = variant 
        self.model_manager = MLModelManager()

    def load_data(self,url_file,features_list):
        # Check if we received a file that does not exist
        if os.path.isfile(url_file):
            pass
        else:
            raise ValueError("Sorry, find does not exists.")
        name, extension = os.path.splitext(url_file)
        # check if it is not a csv file
        if extension != '.csv':
            raise ValueError("Oops!  That was no a csv file.  Try again...")
        else:
            pass
        # check if it is an empty file
        if os.stat(url_file).st_size == 0:
            raise ValueError("Sorry!  That file is empty.  Try again...")
        else:
            pass
        # Load the file
        data = pd.read_csv(url_file)
        data = data[features_list]
        if data.iloc[0].values[1] == 'Unnamed 0':
            data = data.drop(columns=['Unnamed 0'])
        if type(data.iloc[0].values[1]) == str:
            comparison = data.iloc[0].values == data.columns.values
            if comparison.all() == True:
                return data.iloc[1:]
            else:
                return data
        else:
            return data

    def completeFeaturesList(self,training_data):
        # get missing features 
        missing_features = []
        for feature in training_data['feature_list']:
            #if feature == self.target:
            #    continue
            if not feature in self.features_dict:
                missing_features.append(feature)
        if missing_features == []:
            return []

        #print("Missing features detected->", missing_features)
        print("Load training dataset")
        data = self.load_data(training_data['dataset_url'], training_data['feature_list'])
        
        missing_arrays = []
        for feature in missing_features:
            feature_array = []
            feature_array.append({feature: data[feature].min()})
            feature_array.append({feature: data[feature].max()})
            feature_array.append({feature: data[feature].mean()})
            #features_array = [{'feature': min },{'feature': max},{'feature': mean}]
            missing_arrays.append(feature_array) 
        #missing_arrays = [[{'feature': min },{'feature': max},{'feature': mean}],[{'feature1': min },{'feature1': max},{'feature1': mean}],[{'feature2': min },{'feature2': max},{'feature2': mean}]]
        
        missing_features_combined_list = list(itertools.product(*missing_arrays))
        #missing_features_combined_list = [({'feature': min }, {'feature1': min }, {'feature2': min }),({'feature': min }, {'feature1': max }), ...]
        #print(missing_features_combined_list)
        #print(self.features_dict)
        result = []
        for tp in missing_features_combined_list:
            result.append(self.addFeatures(tp))
        return result 

    def normalization2(self, data):
        # data = self.load_data()
        print(data)
        scaler = StandardScaler()
        data_target = data[self.target]
        data = data.drop(columns=[self.target])
        names = data.columns
        scaler.fit(data)
        data = scaler.transform(data)
        data = pd.DataFrame(data, columns=names)
        data[self.target] = data_target
        return data

    def missing_values_imputer(self, data):
        # data = self.load_data()

        # TODO ---> make a configuration column
        # TODO ---> use the missing values imputation based on configuration

        if data.isna().any().any() == False:
            pass
        elif data.isna().any().any() == True:
            names = data.columns
            # example
            imp = SimpleImputer(missing_values=np.nan, strategy='mean')
            imp.fit(data.values)
            data = imp.transform(data.values)
            data = pd.DataFrame(data, columns=names)
        return data

    def normalization(self, new_data, training_data):
        # data = self.load_data()
        data = self.load_data(training_data['dataset_url'], training_data['feature_list'])
        data = data.drop(self.target,1)
        data = data.apply(lambda x: pd.to_numeric(x, errors='coerce')).dropna()
        scaler = StandardScaler()
        names = data.columns
        scaler.fit(data)
        data = scaler.transform(new_data)
        data = pd.DataFrame(data, columns=names)
        return data

    def denormalization(self, training_data, prediction):
        data = pd.read_csv(training_data['dataset_url'], training_data['feature_list'])
        scaler = StandardScaler()
        #names = data.columns
        scaler.fit(data)
        data = scaler.inverse_transform(prediction)
        return data

    def addCandidate(self, application,target, features_dict,prediction, variant):
        try:
            conn = sqlite3.connect(local_database_path + "prediction.db")
            cursor = conn.cursor()
            cursor.execute("INSERT INTO Prediction VALUES (?,?,?,?,?)",(None,application,target,prediction,json.dumps(features_dict),variant))
            conn.commit()
            conn.close()
            return True 
        except Exception as e:
            print("Could not execute query")
            print(e)
            print("Retry in 10s")
            time.sleep(10)
            self.addCandidate(application,target,features_dict,prediction)

    def addFeatures(self, _tuple):
        result = []
        _new_tuple = {}
        for _dict in _tuple:
            _new_tuple.update(_dict)
        
        _new_tuple.update(self.features_dict)
        return _new_tuple
     
    def converter(self,obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)

    def predict(self):
        # Dont use target variable in feature_list
        # check the name of application
        # load the pickle file based on application name
        # Print (return) error if the file dont exist. This means that we dont have trained this model yet
        # 1 -> λαθος ονομα
        # 2 -> σωστο ονομα αλλα η εφαρμογή δεν εχει γινει ακομα evaluated (the application
        # was not evaluated yet or the invalid name)
        # Σε περιπτωση που υπάρχει θα λαμβάνουμε το feature_list και το target για να γίνει
        # το prediction
        #Jean-didier's code 
        if type(self.application) != type(""):
            response = {"status": False, "message": "Bad request application field must python string compatible", "results":{},"ml":{}}
            return json.dumps(response, default=self.converter) 

        if type(self.target) != type(""):
            response = {"status": False, "message": "Bad request target field must python string compatible", "results":{},"ml":{}}
            return json.dumps(response, default=self.converter) 

        if type(self.features_dict) != type({}):
            response = {"status": False, "message": "Bad request feature field must python dict compatible", "results":{},"ml":{}}
            return json.dumps(response, default=self.converter) 

        training_data = self.model_manager.getModelTrainingData(self.application,self.target)
        if training_data == None: 
            response = {"status": False, "message": "No ML model found for the application {0} with target {1}".format(self.application, self.target), "results":{},"ml":{}}
            return json.dumps(response, default=self.converter) 
        """
        completed_feature_list = self.completeFeaturesList(training_data)
        #print(completed_feature_list)
        #print(completed_feature_list)
        list_completed_feature_list = []
        if completed_feature_list != []:
            list_completed_feature_list = completed_feature_list
        else:
            list_completed_feature_list = [self.features_dict]
        #End jean-didier's code
        """
        list_completed_feature_list = [self.features_dict]
        prediction = None 
        for feature_dict in list_completed_feature_list:
            #del feature_dict[self.target]
            df = self.load_data(training_data['dataset_url'], training_data['feature_list'])
            #concatenate feature_dict to the data
            feature_dict[self.target] = 1
            #df2 = pd.DataFrame(feature_dict,index=0)
            print(df.columns.names)
            print(list(feature_dict.keys()))
            #df = pd.concat([df2,df1])
            df.loc[0] = list(feature_dict.values())
            df.replace(r'[a-zA-Z%]', np.nan, regex=True, inplace=True)
            df = self.missing_values_imputer(df)

            data = self.normalization2(df)
            data = data.drop(columns=[self.target])

            new_sample = data.iloc[[0]]
            if training_data['algorithm'] != "automl" and training_data['algorithm'] != "CV Random Forest":
                new_sample = new_sample.values
            
            self.name = path_ml_model + '/' + self.application + self.target + self.variant + '.sav'
            self.trained_algorithm = pickle.load(open(self.name, 'rb'))
            if training_data['algorithm'] == "automl" or training_data['algorithm'] == "CV Random Forest":
                prediction = self.trained_algorithm.predict(new_sample)[0]
            else:
                prediction = self.trained_algorithm.predict(new_sample)

        response = {'status': True,'results':{"prediction": prediction,"target": self.target,"application": self.application},'ml': training_data,'message':""}
        self.addCandidate(self.application,self.target,self.features_dict,float(prediction), self.variant)
        return json.dumps(response, default=self.converter) 
        
    def getPrediction(self):
        return self.prediction

