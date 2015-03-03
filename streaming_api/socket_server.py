#!/usr/bin/env python
from socket import socket
from socket import SOL_SOCKET, SO_REUSEADDR
BUFSIZE = 1024
HOST, PORT = 'localhost', 8888

local_socket = socket()
local_socket.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
local_socket.bind((HOST, PORT))
local_socket.listen(1)

try:
  while True:
    print "Waiting for client..."
    remote_socket, addr = local_socket.accept()
    print "Incoming connection from %s:%d" % addr
    remote_socket.send("Hello client!\n")
    while True:
      try:
        data = remote_socket.recv(BUFSIZE)
        if not data:
          break
      except IOError:
        break
      remote_socket.send("Do you really mean %s?\n" % data.rstrip())
    remote_socket.close()
except KeyboardInterrupt:
  local_socket.close()
  print "\nGoodbye"
