import os, time, train 

"""
def main():
    print("We are in the first")
    pid = os.fork()
    if pid:
        while True:
            print("Parent process")
            time.sleep(5)
    else:
        print("Child process")
        train.start()


if __name__ == "__main__":
    main()

"""
command = ['python3','-u','train.py']
os.spawnlp(os.P_NOWAIT,*command)
while True:
    time.sleep(10)