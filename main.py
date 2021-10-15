import logging
import gluonts_listener

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

e = gluonts_listener.Gluonts()
#try:
#    e.start()
#except KeyboardInterrupt:
#    e.stop()
e.start()
while True:
	pass
