
class MetricsToPredict:


    def load(self,body):
        self.metrics = body["metrics"]
        self.timestamp = body["timestamp"]
        self.epoch_start = body["epoch_start"]
        self.number_of_forward_predictions = body["number_of_forward_predictions"]
        self.prediction_horizon = body["prediction_horizon"]
