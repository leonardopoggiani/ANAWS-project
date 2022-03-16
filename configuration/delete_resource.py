import requests
import json
from time import sleep

controller_ip = "192.168.122.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_resources = base_url + "resources/json"
url_subscribers = base_url + "/subscribers/{resource}/json"

while(True)
    action = input("1) Delete resource\n 2)Unsubscribe user\n ")

    if action == "1":
        resourceIP = input("Insert the resource virtual ip")
        print(requests.delete(url_resources, data=json.dumps({"resource": str(resourceIP)}), headers=header).json())
    else if action == "2":
        resourceIP = input("Insert the resource virtual ip")
        userMAC = input("Insert user MAC")
        print(requests.delete(base_url + "/subscribers/" + str(resourceIP) + "/json", data=json.dumps({"MAC": str(userMAC)}), headers=header).json())
    else:
        print("Invalid choice")
            