import socket
import sys

UDP_PORT = 5005
MESSAGE = b"Hello, World!"

print("Arguments without the script name ", (len(sys.argv)-1))
print("Arguments given ")
for i in range(1, len(sys.argv)):
    print(sys.argv[i])
   
print("UDP target IP: %s" % sys.argv[1])
print("UDP target port: %s" % UDP_PORT)
print("message: %s" % MESSAGE)
 
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) 
sock.sendto(MESSAGE, (sys.argv[1]), UDP_PORT))