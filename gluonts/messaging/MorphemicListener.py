from json import JSONDecodeError

from stomp.listener import ConnectionListener
import logging
import json
from slugify import slugify

class MorphemicListener(ConnectionListener):

    def is_topic(self,headers, event):
        if not hasattr(event,"_match"):
            return False
        match = getattr(event,'_match')
        return headers.get('destination').startswith(match)


    def has_topic_name(self,headers, string):
        return headers.get('destination').startswith(string)

    def get_topic_name(self,headers):
        return headers.get('destination').replace('/topic/','')


    def has_topic_name(self,headers, string):
        return headers.get('destination').startswith(string)

    def get_topic_name(self,headers):
        return headers.get('destination').replace('/topic/','')


    def on(self,headers, res):
        logging.debug("Unknown message %s %s ",headers, res)
        pass

    def on_message(self, frame):
        headers = frame.headers
        body = frame.body
        logging.debug("Headers %s",headers)
        logging.debug("        %s",body)

        try:
            res = json.loads(body)
            func_name='on_%s' % slugify(headers.get('destination').replace('/topic/',''), separator='_',)
            logging.debug("func_name %s", func_name)
            if hasattr(self,func_name):
                func = getattr(self,  func_name)
                func(res)
            else:
                self.on(headers,res)
        except JSONDecodeError:
            logging.error("Error decoding %s", body)