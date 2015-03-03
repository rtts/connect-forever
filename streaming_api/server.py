#!/usr/bin/env python

import socket
import threading
import SocketServer
from time import sleep

class ThreadedTCPRequestHandler(SocketServer.BaseRequestHandler):
    def handle(self):
        self.data = self.request.recv(1024)
        cur_thread = threading.current_thread()
        print '%s received: "%s"' % (cur_thread.name, self.data.rstrip())
        try:
          self.request.send('Thank you, please await further instructions...\n')
          sleep(5)
          self.request.send('Could not contact other, hanging up.\n')
        except IOError:
          print "%s's client disconnected" % cur_thread.name
        print '%s terminating' % cur_thread.name

class ThreadedTCPServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass

if __name__ == "__main__":
    HOST, PORT = "localhost", 10000
    server = ThreadedTCPServer((HOST, PORT), ThreadedTCPRequestHandler)
    print "Serving!"
    server.serve_forever()
