
import stomp
import logging
import json

from stomp.listener import PrintingListener

class Connection:

    subscriptions = []

    def __init__(self, username, password,
                 host='localhost',
                 port=61613,
                 debug=True):
        self.username = username
        self.password = password
        self.hosts = [(host, port)]
        self.conn = stomp.Connection(host_and_ports=self.hosts, auto_content_length=False)

        if debug:
            logging.debug("Enabling debug")
            self.conn.set_listener('print', PrintingListener())

    def _build_id(self,topic,id):
        return "id.%s-%s" % (topic,id)

    def set_listener(self, id, listener):
        if self.conn:
            self.conn.set_listener(id,listener)

    def subscribe(self,destination, id, ack='auto'):
        if not self.conn:
            raise RuntimeError('You need to connect first')

        self.conn.subscribe(destination, id, ack)

    def topic(self,destination, id, ack='auto'):
        self.subscribe("/topic/%s" % destination ,self._build_id(destination,id),ack)

    def queue(self,destination, id, ack='auto'):
        self.subscribe("/queue/%s" % destination ,self._build_id(destination,id),ack)

    def unsubscribe(self, topic,id):

        if not self.conn:
            return
        self.conn.unsubscribe(self._build_id(topic,id))


    def connect(self, wait=True):

        if not self.conn:
            return

        self.conn.connect(self.username, self.password, wait=wait)

    def disconnect(self):
        self.conn.disconnect()

    def send_to_topic(self,destination, body, headers={}, **kwargs):

        if not self.conn:
            logging.error("Connect first")
            return

        str = json.dumps(body)

        self.conn.send(destination="/topic/%s" % destination,
                       body= str,
                       content_type="application/json",
                       headers=headers, **kwargs)
