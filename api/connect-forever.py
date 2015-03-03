
# This is the WebAPI for Connect-Forever, a romantic pair of apps to
# replace the wedding ring.


# Copyright 2011 Return to the Source - http://r2src.com/
# All rights reserved


# Here is a list of all the valid API calls, preceded by their request
# method. The <id> is the unique identifier for the device making
# the request.
# 
# POST /register/<id>   expects body with a value for "c2dm_id",
#                       returns "OK"
#
# GET /location/<id>    sends the "please_send_location" message to the
#                       connected device with data.send=True and returns 200 OK (no body).
#                       if not connected returns 405 Method Not Allowed,
#                       if other device has no c2dm_id returns 503 Service Unavailable
#
# PUT /location/<id>    expects a request body and pushes an "others_location"
#                       message to the other device with the body as data.location
#                       if not connected returns 405 Method Not Allowed,
#                       if other device has no c2dm_id returns 503 Service Unavailable,
#                       otherwise returns 200 OK (no body)
#
# DELETE /location/<id> pushes the please_send_location message with data.send=False
#                       to signify that putting the location is no longer neccesary
#                       if not connected returns 405 Method Not Allowed,
#                       if other device has no c2dm_id returns 503 Service Unavailable,
#                       otherwise returns 200 OK (no body)
#
# GET /proposal/<id>    returns a body with the proposal code
#
# POST /proposal/<id>   expects a body with a value for "key",
#                       crossreferences both devices,
#                       pushes "connection_succeeded" to other device,
#                       returns "Congratulations!"
#
# DELETE /proposal/<id> deletes all pending proposals (should only be 1)
#
# GET /reset/<id>       Resets the connection for this and other device,
#                       meaning the crossreferences (not the devices itself)
#                       will be deleted from the datastore


# Here is a list of possible messages sent via C2DM:
#
# Collapse Key             | Meaning
# ----------------------------------------------------------
#                          |
# please_send_location     | Please send your location twice: once as soon
#                          | as possible, followed by one as accurate as possible
#                          |
# others_location          | Message with body containing other's location
#                          |
# connection_succeeded     | You may start issuing GET requests for location,
#                          | since you are now connected


import logging
import random
import urllib
import httplib
import re
from django.utils import simplejson
from datetime import datetime
from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app

CONTENT_TYPE = 'text/plain'
VALID_ID = r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
KEY_CHARS = '23456789abcdefghijkmnpqrstuvwxyz'
KEY_LENGTH = 8

#################################### MODELS ####################################

class Device(db.Model):
  """
  Represents a device running Connect-Forever
  """
  other = db.SelfReferenceProperty()
  created = db.DateTimeProperty(auto_now_add=True)
  c2dm_id = db.TextProperty(default="")

class Proposal(db.Model):
  """
  A temporary proposal to connect forever
  """
  device = db.ReferenceProperty(Device)
  created = db.DateTimeProperty(auto_now_add=True)

class AuthToken(db.Model):
  """
  The authorization token for C2DM
  """
  token = db.TextProperty()
  
#################################### VIEWS #####################################

class Register(webapp.RequestHandler):
  """
  Interface for registering device UUID and c2dm_id
  """
  
  def post(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    c2dm_id = self.request.get('c2dm_id')
    if not c2dm_id:
      self.error(400) # Bad Request
      return
    device = Device.get_or_insert(id)
    device.c2dm_id = c2dm_id
    device.put()

class Location(webapp.RequestHandler):
  """
  Interface for requesting location
  """

  def get(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    device = Device.get_by_key_name(id)
    if not device:
      self.error(404)
      return
    if not device.other:
      self.error(405)
      return
    if not device.other.c2dm_id:
       self.error(503)
       return

    push(urllib.urlencode({
        'registration_id': device.other.c2dm_id,
           'collapse_key': "please_send_location",
              'data.send': True}))
    
  def put(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    device = Device.get_by_key_name(id)
    if not device:
      self.error(404)
      return
    if not device.other:
      self.error(405)
      return
    if not device.other.c2dm_id:
       self.error(503)
       return
    
    push(urllib.urlencode({
        'registration_id': device.other.c2dm_id,
           'collapse_key': "others_location",
          'data.location': self.request.body}))

  def delete(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    device = Device.get_by_key_name(id)
    if not device:
      self.error(404)
      return
    if not device.other:
      self.error(405)
      return
    if not device.other.c2dm_id:
       self.error(503)
       return

    push(urllib.urlencode({
        'registration_id': device.other.c2dm_id,
           'collapse_key': "please_send_location",
              'data.send': False}))
    

class Propose(webapp.RequestHandler):
  """
  Ceremonial procedure that connects two devices
  
  GET - returns a proposal key
  POST - expects an existing key, performs the connection
  DELETE - withdraws all proposals of the given device
  """
  def get(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    device = Device.get_by_key_name(id)
    if not device:
      self.error(404)
      return
    key = ''.join(random.choice(KEY_CHARS) for n in range(KEY_LENGTH))
    Proposal(key_name=key, device=device).put()
    self.response.out.write(key)
  
  def post(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    key = self.request.get('key')
    if not key:
      self.error(400) # Bad Request
      return
    device = Device.get_by_key_name(id)
    if not device:
      self.error(404)
      return
    proposal = Proposal.get_by_key_name(key)
    if not proposal:
      self.response.out.write("Key not found")
      return
    other_device = proposal.device
    device.other = other_device
    device.put()
    other_device.other = device
    other_device.put()
    proposal.delete()

    if device.other.c2dm_id:
      push(urllib.urlencode({
          'registration_id': device.other.c2dm_id,
             'collapse_key': "connection_succeeded"}))

    self.response.out.write('Congratulations!')
  
  def delete(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    device = Device.get_by_key_name(id)
    if not device:
      self.error(404)
      return
    for p in Proposal.all().filter('device = ', device):
      p.delete()
    self.response.out.write('Proposal withdrawn')

class Reset(webapp.RequestHandler):
  def get(self, id):
    self.response.headers['Content-Type'] = CONTENT_TYPE
    device = Device.get_or_insert(id)
    if device.other:
      device.other.other = None
      device.other.put()
    device.other = None
    device.put()
    self.response.out.write('OK')
    

############################## CONVENIENCE METHODS #############################

def push(params):
  '''
  Push params to an android device through the C2DM servers.
  '''
  headers = { "Content-type": "application/x-www-form-urlencoded",
              "Accept": "text/plain",
              "Authorization": "GoogleLogin auth=" + get_token() 
            }
  conn = httplib.HTTPConnection("android.apis.google.com/c2dm/send")
  conn.request("POST", "", params, headers)
  response = conn.getresponse()

  logging.debug("Google's %i response: %s" % (response.status, response.read()))

  if response.status == 401:
    logging.error('c2dm authentication token invalid --- P A N I C ! ! !')
    return    

  if not (response.status == 200):
    logging.error('c2dm responded with status code ' + response.status)
    return
  
  match = re.search(r'Error=(\w+)', response.read())
  if match:
    logging.error('c2dm responded with error ' + match.group(1))
  
  update_token(response.getheader("Update-Client-Auth"))
  conn.close()

def update_token(new_token):
  '''
  Refresh the Google Authorization Token if changed.
  '''
  auth_token = AuthToken.all().get()
  if new_token != None and auth_token.token != new_token:
    logging.debug('updating c2dm auth token')
    auth_token.token = new_token
    auth_token.put()

def get_token():
  '''
  Return the known authorization token or request a new one if no
  token is present.
  '''
  token = AuthToken.all().get()
  if token:
    return token.token
  else:
    logging.debug('requesting c2dm auth token')
    params = urllib.urlencode(
      {
        'accountType':"HOSTED_OR_GOOGLE",
        'Email': [REDACTED],
        'Passwd': [REDACTED],
        'service':"ac2dm",
        'source':"ReturnToTheSource-ConnectForever-1"
      })
    headers = {"Content-type": "application/x-www-form-urlencoded",
               "Accept": "text/plain"}
    conn = httplib.HTTPSConnection("www.google.com")
    conn.request("POST", "/accounts/ClientLogin", params, headers)
    response = conn.getresponse()
    conn.close()
    if response.status == 200:

      match = re.search(r'Auth=([\w-]+)', response.read())
      if match:
        a = AuthToken(token=match.group(1))
        a.put()
        return a.token
      else:
        logging.error('c2dm returned invalid auth token --- P A N I C ! ! !')

################################## CONTROLLER ##################################

webapi = webapp.WSGIApplication(
  [(r'/register/(%s)' % VALID_ID, Register),
   (r'/location/(%s)' % VALID_ID, Location),
   (r'/proposal/(%s)' % VALID_ID, Propose),
   (r'/reset/(%s)' % VALID_ID, Reset)],
  debug = False)

def main():
  run_wsgi_app(webapi)

if __name__ == '__main__':
  main()
