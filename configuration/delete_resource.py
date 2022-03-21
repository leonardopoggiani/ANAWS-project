import requests
import json
import sys
from time import sleep

controller_ip = "192.168.122.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_resources = base_url + "resources/json"
if len(sys.arg) < 1
    print("Insert the ip address of the resource as argument\n")
elif len(sys.arg) > 1
    print("Too many arguments, just enter the ip address of the resource\n")
else
    resourceIP = sys.argv[1]
    print(requests.delete(url_resources, data=json.dumps({"resource": '"' + str(resourceIP) + '"'}), headers=header).json())
   