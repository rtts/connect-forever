#!/usr/bin/env python
import sys
from socket import socket
BUFSIZE = 1024
HOST, PORT = 'localhost', 8888

socket = socket()
socket.connect((HOST, PORT))
print "[Connected to server]"

try:
  while True:
    print socket.recv(BUFSIZE),
    socket.send(sys.stdin.readline())

except KeyboardInterrupt:
  socket.close()
  print "\nGoodbye"
