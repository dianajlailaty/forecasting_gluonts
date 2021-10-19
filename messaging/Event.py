

class Metric(enumerate):
    """
    [0] (current/detected) Metrics & SLOs Events Format:


    This event is aggregated by EMS and it is persisted in InfluxDB. Moreover,
    Prediction Orchestrator will subscribe and receive the current metrics in order to
    evaluate the forecasting methods, according to the defined KPIs (e.g., MAPE)

    * Topic: [metric_name]
        > (e.g. MaxCPULoad)


    {
        "metricValue": 12.34,

        "level": 1,

        "timestamp": 143532341251,

        "refersTo": "MySQL_12345",

        "cloud": "AWS-Dublin",

        "provider": "AWS"

    }



    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication

    """
    TIMESTAMP = "timestamp"
    METRIC_VALUE = "metricValue"
    REFERS_TO = "refersTo"
    CLOUD = "cloud"
    PROVIDER = "provider"



class PredictionMetric(enumerate):

    """
    [1] Predicted Metrics & SLOs Events Format


    This event is produced by the Prediction Orchestrator and reflects the final predicted value for a metric.

     - Topic: prediction.[metric_name]
        > (e.g. prediction.MaxCPULoad)


    {
        "metricValue": 12.34,

        "level": 1,

        "timestamp": 143532341251,

        "probability": 0.98,

        "confidence_interval " : [8,15]

        "predictionTime": 143532342,

        "refersTo": "MySQL_12345",

        "cloud": "AWS-Dublin",

        "provider": "AWS"

    }


    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication

    """

    _match = "prediction."

    METRICVALUE= "metricValue"
    '''Predicted metric value'''
    LEVEL= "level"
    '''Level of VM where prediction occurred or refers'''
    TIMESTAMP= "timestamp"
    '''Prediction creation date/time from epoch'''
    PROBABILITY= "probability"
    '''Probability of the predicted metric value (range 0..1)'''
    CONFIDENCE_INTERVAL= "confidence_interval"
    '''the probability-confidence interval for the prediction'''
    PREDICTION_TIME= "predictionTime"
    '''This refers to time point in the imminent future (that is relative to the time
     that is needed for reconfiguration) for which the predicted value is considered 
     valid/accurate (in UNIX Epoch)'''
    REFERSTO= "refersTo"
    '''The id of the application or component or (VM) host for which the prediction refers to'''
    CLOUD= "cloud"
    '''Cloud provider of the VM (with location)'''
    PROVIDER= "provider"
    '''Cloud provider name'''



class MetricsToPredict(enumerate):

    """
    [2] Translator – to – Forecasting Methods/Prediction Orchestrator Events Format


    This event is produced by the translator, to:

    imform Dataset Maker which metrics should subscribe to in order to aggregate the appropriate tanning dataset in the time-series DB.
    instruct each of the Forecasting methods to predict the values of one or more monitoring metrics
    inform the Prediction Orchestrator for the metrics which will be forecasted

    * Topic: metrics_to_predict


    *Note:* This event could be communicated through Mule


        [
            {

                "metric": "MaxCPULoad",

                "level": 3,

                "publish_rate": 60000,

            },

            {

                "metric": "MinCPULoad",

                "level": 3,

                "publish_rate": 50000,

            }

        ]


    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication

    """

    _match = "metrics_to_predict"

    METRIC = "metric"
    '''name of the metric to predict'''
    LEVEL = "level"
    '''Level of monitoring topology where this metric may be produced/found'''
    PUBLISH_RATE = "publish_rate"
    '''expected rate for datapoints regarding the specific metric (according to CAMEL)'''


class TraningModels(enumerate):
    """

    [3] Forecasting Methods – to – Prediction Orchestrator Events Format


    This event is produced by each of the Forecasting methods, to inform the
    Prediction Orchestrator that the method has (re-)trained its model for one or more metrics.

    * Topic: training_models


        {

            "metrics": ["MaxCPULoad","MinCPULoad"]",

             "forecasting_method": "ESHybrid",

            "timestamp": 143532341251,

        }


    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication

    """
    _match = "training_models"

    METRICS = "metrics"
    '''metrics for which a certain forecasting method has successfully trained or re-trained its model'''
    FORECASTING_METHOD = "forecasting_method"
    '''the method that is currently re-training its models'''
    TIMESTAMP = "timestamp"
    '''date/time of model(s) (re-)training'''


class IntermediatePrediction(enumerate):
    """

    [4] Forecasting Methods – to – Prediction Orchestrator Events Format


    This event is produced by each of the Forecasting methods, and is used by the Prediction Orchestrator to determine the final prediction value for the particular metric.


    * Topic: intermediate_prediction.[forecasting_method].[metric_name]
        * (e.g. intermediate_prediction.ESHybrid.MaxCPULoad)
        * We note that any component will be able to subscribe to topics like:
            * intermediate_prediction.*.MaxCPULoad → gets MaxCPULoad predictions produced by all forecasting methods or
            * intermediate_prediction.ESHybrid.* → gets all metrics predictions from ESHybrid method
        * We consider that each forecasting method publishes a static (but configurable) number m of predicted values (under the same timestamp) for time points into the future. These time points into the future are relevant to the reconfiguration time that it is needed (and can also be updated).
            * For example if we configure m=5 predictions into the future and the reconfiguration time needed is TR=10 minutes, then at t0 a forecasting method publishes 5 events with the same timestamp and prediction times t0+10, t0+20, t0+30, t0+40, t0+50.



    {
        "metricValue": 12.34,

        "level": 3,

        "timestamp": 143532341251,

        "probability": 0.98,

        "confidence_interval " : [8,15]

        "predictionTime": 143532342,

        "refersTo": "MySQL_12345",

        "cloud": "AWS-Dublin",

        "provider": "AWS"

    }


    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication

    """

    _match="intermediate_prediction."

    METRICVALUE = "metricValue"
    '''Predicted metric value (more than one such events will be produced for different time points into the future – this can be valuable to the Prediction Orchestrator in certain situations e.g., forecasting method is unreachable for a time period)'''

    LEVEL = "level"
    '''Level of VM where prediction occurred or refers'''

    TIMESTAMP = "timestamp"
    '''Prediction creation date/time from epoch'''

    PROBABILITY = "probability"
    '''Probability of the predicted metric value (range 0..1)'''

    CONFIDENCE_INTERVAL = "confidence_interval"
    '''the probability-confidence interval for the prediction'''

    PREDICTION_TIME = "predictionTime"
    '''This refers to time point in the imminent future (that is relative to the time that is needed for reconfiguration) for which the predicted value is considered valid/accurate (in UNIX Epoch)'''

    REFERS_TO = "refersTo"
    '''The id of the application or component or (VM) host for which the prediction refers to'''

    CLOUD = "cloud"
    '''Cloud provider of the VM (with location)'''

    PROVIDER = "provider"
    '''Cloud provider name'''



class Prediction(enumerate):
    """

    [5] Prediction Orchestrator – to – Severity-based SLO Violation Detector Events Format


    This event is used by the Prediction Orchestrator to inform the SLO Violation Detector about the current values of a metric, which can possibly lead to an SLO Violation detection.

    * Topic: prediction.[metric_name]
        * (e.g. prediction.MaxCPULoad)


    {
        "metricValue": 12.34,

        "level": 1,

        "timestamp": 143532341251,

        "probability": 0.98,

        "confidence_interval " : [8,15]

        "predictionTime": 143532342,

        "refersTo": "MySQL_12345",

        "cloud": "AWS-Dublin",

        "provider": "AWS"

    }



    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication


    """

    _match = "prediction."

    METRICVALUE = "metricValue"
    '''Predicted metric value'''

    LEVEL = "level"
    '''Level of VM where prediction occurred or refers'''

    TIMESTAMP = "timestamp"
    '''Prediction creation date/time from epoch'''

    PROBABILITY = "probability"
    '''Probability of the predicted metric value (range 0..1)'''

    CONFIDENCE_INTERVAL = "confidence_interval"
    '''the probability-confidence interval for the prediction'''

    PREDICTIONTIME = "predictionTime"
    '''This refers to time point in the imminent future (that is relative to the time that is needed for reconfiguration) for which the predicted value is considered valid/accurate (in UNIX Epoch)'''

    REFERSTO = "refersTo"
    '''The id of the application or component or (VM) host for which the prediction refers to'''

    CLOUD = "cloud"
    '''Cloud provider of the VM (with location)'''

    PROVIDER = "provider"
    '''Cloud provider name'''


class StopForecasting(enumerate):
    """
    [6] Prediction Orchestrator – to – Forecasting Methods Events Format


    This event is used by the Prediction Orchestrator to instruct a forecasting method to stop producing predicted values for a selection of metrics.


    * Topic: stop_forecasting.[forecasting_method]
    * Each component that implements a specific forecasting method it should subscribe to its relevant topic (e.g. the ES-Hybrid component should subscribe to stop_forecasting.eshybrid topic)


    {
        "metrics": ["MaxCPULoad","MinCPULoad"],
        "timestamp": 143532341251,
    }

    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication


    """

    _match="stop_forecasting."

    METRICS = "metrics"
    '''metrics for which a certain method should stop producing predictions (because of poor results)'''
    TIMESTAMP = "timestamp"
    '''date/time of the command of the orchestrator'''


class StartForecasting(enumerate):
    """

    [7] Prediction Orchestrator – to – Forecasting Methods Events Format

    This event is used by the Prediction Orchestrator to instruct a forecasting method to start producing predicted values for a selection of metrics.


    * Topic: start_forecasting.[forecasting_method]
    * Each component that implements a specific forecasting method it should subscribe to its relevant topic (e.g. the ES-Hybrid component should subscribe to start_forecasting.eshybrid topic)
    * We consider that each forecasting method should publish a static (but configurable) number m of  predicted values (under the same timestamp) for time points into the future. These time points into the future are relevant to the reconfiguration time that it is needed (and can also be updated).
        * For example if we configure m=5 predictions into the future and the reconfiguration time needed is TR=10 minutes, then at t0 a forecasting method publishes 5 events with the same timestamp and prediction times t0+10, t0+20, t0+30, t0+40, t0+50.




    {
        "metrics": ["MaxCPULoad","MinCPULoad"],

        "timestamp": 143532341251,

        "epoch_start": 143532341252,

        "number_of_forward_predictions": 5,

        "prediction_horizon": 600

    }

    https://confluence.7bulls.eu/display/MOR/Forecasting+Mechanism+Sub-components+Communication


    """

    _match="start_forecasting."

    METRICS = "metrics"
    '''metrics for which a certain method should start producing predictions'''
    TIMESTAMP = "timestamp"
    '''date/time of the command of the orchestrator'''
    EPOCH_START = "epoch_start"
    '''this time refers to the start time after which all predictions will be considered (i.e. t0)'''
    NUMBER_OF_FORWARD_PREDICTIONS = "number_of_forward_predictions"
    ''' this is a number that indicates how many time points into the future do we need predictions for.'''
    PREDICTION_HORIZON = "prediction_horizon"
    '''This time equals to the time (in seconds) that is needed for the platform to implement an application reconfiguration (i.e. TR).'''