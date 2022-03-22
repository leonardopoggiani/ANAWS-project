import socket
import sys
import numpy as np
import time

UDP_PORT = 5005
TEMP_RESOURCE_IP = "1.1.1.1"

np.random.seed(3)

last_temp_reading = 19 # initial temperature

while 1:
    # generate rand temperature reading
    mean_var = 7 # mean value variation for one reading
    var = np.random.normal(0.05, 0.1, 1) # generate random multiplier, normal distribution centered in 0 - slightly skewed to increase the temp.
    change = mean_var*var # random generated temperature variation
    new_reading = last_temp_reading + change
    num_value = new_reading[0]
    print(str(round(num_value, 2))+" C - Sending to Resource: " + TEMP_RESOURCE_IP)
    MESSAGE = "%.2f"%num_value
    # send message to the target ip
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) 
    sock.sendto(MESSAGE, (TEMP_RESOURCE_IP, UDP_PORT))
    last_temp_reading = new_reading
    time.sleep(0.5)

