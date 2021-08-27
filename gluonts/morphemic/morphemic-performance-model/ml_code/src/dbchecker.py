import time, os, sqlite3, stomp, json

db_last_evaluation = time.time()
db_evaluation_period = 60*2 #2 minutes

subscription_topic = 'performance_model_evaluator'
activemq_username = os.getenv("ACTIVEMQ_USER","aaa") 
activemq_password = os.getenv("ACTIVEMQ_PASSWORD","111") 
activemq_hostname = os.getenv("ACTIVEMQ_HOST","localhost")
activemq_port = int(os.getenv("ACTIVEMQ_PORT","61613")) 

local_database_path = os.environ.get("LOCAL_DATABASE_PATH","./db/")

def DBEvaluationRoutine():
    print("DB Evaluator routine started")
    global db_last_evaluation
    while True:
        if time.time() - db_last_evaluation >= db_evaluation_period:
            try:
                conn = sqlite3.connect(local_database_path + "prediction.db")
                cursor = conn.cursor()
                data_to_send = []
                for row in cursor.execute("SELECT * FROM Prediction"):
                    _json = {'application': row[1], 'target': row[2], 'features': json.loads(row[4]), 'prediction': row[3], 'variant': row[4]}
                    data_to_send.append(_json)
                conn.close()
                if data_to_send != []:
                    conn = stomp.Connection(host_and_ports = [(activemq_hostname, activemq_port)])
                    conn.connect(login=activemq_username,passcode=activemq_password)
                    time.sleep(2)
                    conn.send(body=json.dumps(data_to_send), destination=subscription_topic, persistent='false')
                    conn.disconnect()
                    print("Messages pushed to activemq")
                    print("Removing message to Local DB")
                    conn = sqlite3.connect(local_database_path + "prediction.db")
                    cursor = conn.cursor()
                    cursor.execute("DELETE FROM Prediction")
                    conn.commit()
                    conn.close()
                    
                else:
                    print("Nothing found")
            except Exception as e:
                print("An error occured")
                print(e)
            db_last_evaluation = time.time()
        time.sleep(5)
    print("DB Evaluation stopped")

DBEvaluationRoutine()