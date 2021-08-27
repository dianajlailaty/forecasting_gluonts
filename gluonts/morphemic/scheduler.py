
import datetime
import time
import logging

logging.basicConfig(level=logging.DEBUG)

class Scheduler:

    def __init__(self, epoch_start=0, forward_predictons=5, horizon=600):
        """

        :param epoch_start: The start UNIX UTC timestamp in milliseconds
        :param forward_predictons: The number of forward predictions requested
        :param horizon: The time horizon in seconds
        """
        self._epoch_start = epoch_start/1000
        self._forward_predictions = forward_predictons
        self._horizon = horizon
        self._next_time = self.compute_next()

        logging.debug(
            """
                Epoch: %s 
                Horizon: %s 
                Step: %s 
                Next: %s 
                Now: %s 
                
            """ % (datetime.datetime.fromtimestamp(self._epoch_start/1000),
                   horizon,
                   forward_predictons,
                   datetime.datetime.fromtimestamp(self._next_time),
                   datetime.datetime.now(),

                   )
        )

    def compute_next(self):
        step = int((time.time() - self._epoch_start) / self._horizon)
        return self._epoch_start + ((step + 1) * self._horizon)

    def check(self, handler):
        t = int(time.time())
        # logging.debug("Checking t = %s(%s)  > next_time %s(%s) " % (datetime.datetime.fromtimestamp(t), t, datetime.datetime.fromtimestamp(self._next_time), self._next_time))
        times = []
        if t > self._next_time:
            for i in range(0, self._forward_predictions):
                # logging.info(" t%s %s ", i, datetime.datetime.fromtimestamp(self._next_time + ( i * self._horizon) ) )
                times.append(datetime.datetime.fromtimestamp(self._next_time + ( i * self._horizon) ) )

            self._next_time = self.compute_next()

        if handler:
            handler.on_schedule(times)


class Handler:

    def on_schedule(self, times):
        pass