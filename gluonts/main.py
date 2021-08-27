import logging
from forecasting import gluontsv2

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

e = gluontsv2.Gluonts()
#try:
#    e.start()
#except KeyboardInterrupt:
#    e.stop()
e.start()
while True:
	pass
