import requests
import json
import sys
from time import sleep

controller_ip = "192.168.122.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}
url_subscribers = base_url + "/subscribers/{resource}/json"
howManyArgument = len(sys.argv)

if howManyArgument < 2
    print("Insert the MAC address of the user and the ip address of the resource as arguments\n")
elif howManyArgument > 2
    print("Too many arguments, just enter the MAC address of the user and the ip address of the resource\n")
else
    userMAC = sys.arg[1]
    resourceIP = sys.argv[2]
    print(requests.delete(base_url + "/subscribers/" + str(resourceIP) + "/json", data=json.dumps({"MAC": str(userMAC)}), headers=header).json())
   