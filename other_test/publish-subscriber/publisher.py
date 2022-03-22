import socket
import sys

UDP_PORT = 5005
MESSAGE = b"Hello, World!"
   
   
print("********************")
print("UDP target IP: %s" % sys.argv[1])
print("UDP target port: %s" % UDP_PORT)
print("Publishing: %s" % MESSAGE)
print("********************")

# send message to the target ip
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) 
sock.sendto(MESSAGE, (sys.argv[1], UDP_PORT))