import os, time, json, logging

from initial_ML_module import Trainer

train_folder = os.environ.get("TRAIN_DATA_FOLDER","./train")
log_folder = os.environ.get("LOGS_FOLDER","./logs")

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
def start():
    logger.info("Training process started ...")
    _json = None 
    try:
        _file = open(train_folder + "/train.data",'r')
        _json = json.loads(_file.read())
        _file.close()
    except Exception as e:
        logger.info("An error occured")
        logger.info(e)
        return False 
    try:
        trainer = Trainer(_json["url_file"],_json["target"],_json["application"],_json["features"], _json['variant'])
        trainer.train()
    except Exception as e:
        print(e)
        logger.info(e)

if __name__ == "__main__":
    start()