import requests
import json
import sys
from time import sleep

controller_ip = "192.168.122.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_resources = base_url + "resources/json"
url_subscribers = base_url + "/subscribers/{resource}/json"
resourceIP = sys.argv[1]
subscriberMAC = sys.argv[2]

while(True):
    action = input("1) Delete resource\n2)Unsubscribe user\n ")

    if str(action) == "1":
        resourceIP = input("Insert the resource virtual ip\n")
        print(requests.delete(url_resources, data=json.dumps({"resource": '"' + str(resourceIP) + '"'}), headers=header).json())
    elif str(action) == "2":
        resourceIP = input("Insert the resource virtual ip\n")
        print(resourceIP)
        userMAC = input("Insert user MAC")
        print(requests.delete(base_url + "/subscribers/" + str(resourceIP) + "/json", data=json.dumps({"MAC": str(userMAC)}), headers=header).json())
    else:
        print("Invalid choice")
            