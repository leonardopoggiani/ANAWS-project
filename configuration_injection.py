import requests
import json
from time import sleep

controller_ip = "127.0.0.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_resources = base_url + "resources/json"
url_subscribers = base_url + "/subscribers/{resource}/json"

# Users
print(requests.post(url_resources, data=json.dumps({}), headers=header).json())
print(requests.post(url_resources, json.dumps({}), headers=header).json())
print(requests.post(url_resources, json.dumps({}), headers=header).json())

# Servers
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.1", "MAC": "00:00:00:00:00:01"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.2", "MAC": "00:00:00:00:00:02"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.5", "MAC": "00:00:00:00:00:05"}), headers=header).json())

print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.3", "MAC": "00:00:00:00:00:03"}), headers=header).json())

