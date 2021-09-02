import pandas as pd
import numpy as np
import logging
import itertools

from sklearn import preprocessing
from math import log
from math import exp
pd.set_option('display.max_row', 500)
import itertools
from sklearn.model_selection import ParameterGrid
#from dataset_maker import CSVData
from time import time
from datetime import datetime
from datetime import timedelta
import ast
import pickle
import json
import os
import matplotlib.pyplot as plt

import mxnet as mx
from mxnet import gluon
from gluonts.dataset.common import ListDataset
from gluonts.model.simple_feedforward import SimpleFeedForwardEstimator
from gluonts.model.deepar import DeepAREstimator
from gluonts.mx.trainer import Trainer
from gluonts.evaluation.backtest import make_evaluation_predictions
from gluonts.evaluation import Evaluator
from gluonts.dataset.util import to_pandas
from gluonts.dataset.field_names import FieldName
from gluonts.model.forecast import SampleForecast
from pandas import Timestamp
import itertools
from sklearn.model_selection import ParameterGrid
import statistics
import math

directory_path = "/morphemic_project/forecasting_gluonts/gluonts/"

def train(metric):
    #loading the dataset
    #filename='dataset/demo'
    print(os.getcwd())
    data_file_path = os.path.join(os.environ.get("DATA_PATH", "./"), f'{os.environ.get("APP_NAME", "demo")}.csv')
    #dataset= pd.read_csv("/morphemic_project/forecasting_prophet/prophet/default_application.csv")
    dataset = pd.read_csv(data_file_path)
   
    #changing the names and the format of the attributes
    gluonts_dataset= pd.DataFrame()
    gluonts_dataset['ds'] = dataset["time"]
    gluonts_dataset['y']=dataset[metric]
    gluonts_dataset['y'] = pd.to_numeric(gluonts_dataset['y'], errors='coerce')
    for  i in range (0,len(gluonts_dataset['ds'])):
        gluonts_dataset['ds'] [i]= datetime.fromtimestamp(gluonts_dataset['ds'] [i])
    
    for i in range(0,len(gluonts_dataset)):
        ds=gluonts_dataset['ds'][i]
        gluonts_dataset['ds'][i+1]=ds + timedelta(seconds=60)
        
    for i in range(0,len(gluonts_dataset['y'])):
        if math.isnan(float(gluonts_dataset['y'][i])):
           # print("true")
            gluonts_dataset['y'][i] = gluonts_dataset['y'].mean()
        
    size = len(gluonts_dataset)
    
    logging.debug("STARTED TRAINING FOR: "+ metric)

    #splitting to train and test
    test_percentage=0.2
    training_window_size=int(len(gluonts_dataset)-(len(gluonts_dataset)*test_percentage))
    
    train=gluonts_dataset[:training_window_size]
    validation=gluonts_dataset[training_window_size:]

    train_time = train['ds'][training_window_size-1]
    validation_time = gluonts_dataset['ds'][size-1]
    freq='1min'

    
    gluonts_dataset=gluonts_dataset.set_index('ds')         
    train_ds = ListDataset([{"start":gluonts_dataset.index[0], "target":gluonts_dataset.y[:train_time]}],freq=freq)
    validation_ds = ListDataset([{"start":gluonts_dataset.index[0],"target":gluonts_dataset.y[:validation_time]}],freq=freq)

 
    train_entry = next(iter(train_ds))
    train_entry.keys()

    validation_entry = next(iter(validation_ds))
    validation_entry.keys()

    train_series = to_pandas(train_entry)
    validation_series = to_pandas(validation_entry)

    prediction_length = len(validation_series) - len(train_series)

    
        
    #hyperparameter tuning and cross validation
    batch_size = [75]
    epochs = [5]
    num_batches_per_epoch = [10]
    learning_rate = [1e-3]
    context_length = [5]
    
    param_grid = {'batch_size': batch_size,
              'epochs': epochs,
              'num_batches_per_epoch': num_batches_per_epoch,
              'learning_rate': learning_rate,
              'context_length': context_length
             }
    grid = ParameterGrid(param_grid)
    cnt = 0
    for p in grid:
        cnt = cnt+1
    
    all_params = [dict(zip(param_grid.keys(), v)) for v in itertools.product(*param_grid.values())]
    agg_metrics_all=list()
    item_metrics_all=list()
    for params in all_params:
        estimator = DeepAREstimator(
                            #num_hidden_dimensions=[params['num_hidden_dimensions']],
                            prediction_length=prediction_length,
                            context_length=params['context_length'],
                            freq=freq,
                            trainer=Trainer(ctx="cpu",
                                            epochs=params['epochs'],
                                            learning_rate=params['learning_rate'],
                                            num_batches_per_epoch=params['num_batches_per_epoch']
                                           )
                    )
        predictor = estimator.train(training_data = train_ds)
        forecast_it, ts_it = make_evaluation_predictions(
                                dataset=validation_ds,  # validationdataset
                                predictor=predictor,  # predictor
                                num_samples=20,  # number of sample paths we want for evaluation
                             )
        forecasts = list(forecast_it)
        tss = list(ts_it)
        evaluator = Evaluator(quantiles=[0.1,0.5,0.9])
        agg_metrics, item_metrics = evaluator(iter(tss), iter(forecasts), num_series=len(validation_ds))
        #agg_metrics['num_hidden_dimensions'] = params['num_hidden_dimensions']
        agg_metrics['epochs'] = params['epochs']
        agg_metrics['learning_rate'] = params['learning_rate']
        agg_metrics['num_batches_per_epoch'] = params['num_batches_per_epoch']
        agg_metrics['context_length'] = params['context_length']
        agg_metrics['forecast'] = forecasts
        agg_metrics_all.append(agg_metrics)
        item_metrics_all.append(item_metrics)
        
    dataframe = pd.DataFrame(agg_metrics_all)
    sorted1 = dataframe.sort_values(by=['MAPE'])
    sorted1 = sorted1.reset_index(drop=True)
    
    estimator1 = DeepAREstimator(
        prediction_length=prediction_length,
        context_length=sorted1['context_length'][0],
        freq=freq,
        trainer=Trainer(ctx="cpu",
                        epochs=sorted1['epochs'][0],
                        learning_rate=sorted1['learning_rate'][0],
                        num_batches_per_epoch=sorted1['num_batches_per_epoch'][0]
                       )
                )
                
    
    predictor1 = estimator1.train(training_data=validation_ds)
    

    #probabilities[metric] = prob
    #checking if probabilities file exist
    prob=0.8
    if(os.path.isfile(directory_path+'prob_file.npy')):
        probs = np.load(directory_path+'prob_file.npy' , allow_pickle='TRUE').item()
        probs[metric] = prob
    else:
        probs=dict()
        probs[metric] = prob
    #writing probabilities in a file
    npy_filename= directory_path+'prob_file.npy'
    f = open(npy_filename, "w+")
    np.save(directory_path+'prob_file.npy', probs) 
    f.close()
    return predictor1


def predict(model , number_of_forward_predictions , prediction_horizon , epoch_start , metric ):


    data_file_path = os.path.join(os.environ.get("DATA_PATH", "./"), f'{os.environ.get("APP_NAME", "demo")}.csv')
    #dataset= pd.read_csv("/morphemic_project/forecasting_prophet/prophet/default_application.csv")
    dataset = pd.read_csv(data_file_path)
    gluonts_dataset= pd.DataFrame()
    gluonts_dataset['ds'] = dataset["time"]
    gluonts_dataset['y']=dataset[metric]
    for  i in range (0,len(gluonts_dataset['ds'])):
        gluonts_dataset['ds'] [i]= datetime.fromtimestamp(gluonts_dataset['ds'] [i]) 
    for i in range(0,len(gluonts_dataset)):
        ds=gluonts_dataset['ds'][i]
        gluonts_dataset['ds'][i+1]=ds + timedelta(seconds=60)
    
    gluonts_dataset['y'] = pd.to_numeric(gluonts_dataset['y'], errors='coerce')
    
    for i in range(0,len(gluonts_dataset['y'])):
        if math.isnan(float(gluonts_dataset['y'][i])):
            gluonts_dataset['y'][i] = gluonts_dataset['y'].mean()
    
    future = list()
    for i in range(1, number_of_forward_predictions+1):
        dateInSec = epoch_start + i*prediction_horizon*60
        date=datetime.fromtimestamp(dateInSec)
        future.append(date)
    future = pd.DataFrame(future)
    future.columns = ['ds']
    
    target = list(gluonts_dataset.y[-number_of_forward_predictions:])

    
    for i in range(0,len(target)):
        new_row = {'ds': future['ds'][i], 'y':target[i]}
        gluonts_dataset=gluonts_dataset.append( new_row , ignore_index=True)
    
    
    gluonts_dataset=gluonts_dataset.set_index('ds')      
     
    test_time =  list(future['ds'])[-1]
    test_ds = ListDataset([{"start":gluonts_dataset.index[0],"target":gluonts_dataset.y[:test_time]}],freq='1min')
    
    forecast_it, ts_it = make_evaluation_predictions(
        dataset=test_ds,  # test dataset
        predictor=model,  # predictor
        num_samples=20,  # number of sample paths we want for evaluation
    )
   
    forecasts = list(forecast_it)
    forecast_entry = forecasts[0]
    
    predictions=forecast_entry.samples
    
    mins=list()
    maxs=list()
    values=list()
    
    returnDict=dict()
    for i in range(0 , number_of_forward_predictions):
        mylist=list()
        for line in predictions:
            mylist.append(line[i])
        mini = min(mylist)
        maxi = max(mylist)
        value = statistics.mean(mylist)
        
        mins.append(mini)
        maxs.append(maxi)
        values.append(value)
      
    
    returnDict = {'mins':mins, 'maxs':maxs, 'values':values}
    
    return returnDict
 
