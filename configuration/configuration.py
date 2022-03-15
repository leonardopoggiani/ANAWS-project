import requests
import json
from time import sleep

controller_ip = "192.168.122.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_resources = base_url + "resources/json"
url_subscribers = base_url + "/subscribers/{resource}/json"

# Create 3 resources
print(requests.post(url_resources, data=json.dumps({}), headers=header).json())
print(requests.post(url_resources, json.dumps({}), headers=header).json())
print(requests.post(url_resources, data=json.dumps({}), headers=header).json())
print(requests.post(url_resources,data=json.dumps({}), headers=header).json())

# Subscribe host to resources
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.1", "MAC": "00:00:00:00:00:01"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.2", "MAC": "00:00:00:00:00:02"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.3", "MAC": "00:00:00:00:00:03"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.4", "MAC": "00:00:00:00:00:04"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.3/json", json.dumps({"address": "10.0.0.5", "MAC": "00:00:00:00:00:05"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.3/json", json.dumps({"address": "10.0.0.6", "MAC": "00:00:00:00:00:06"}), headers=header).json())

#print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.1", "MAC": "00:00:00:00:00:01"}), headers=header).json())
#print(requests.post(base_url + "/subscribers/1.1.1.3/json", json.dumps({"address": "10.0.0.6", "MAC": "00:00:00:00:00:06"}), headers=header).json())

#print(requests.delete(base_url + "/subscribers/1.1.1.1/json", data=json.dumps({"MAC": "00:00:00:00:00:01"}), headers=header).json())

#print(requests.delete(base_url + "/resources/json", data=json.dumps({"resource":"1.1.1.1"}), headers=header).json())

#print(requests.delete(base_url + "/resources/json", data=json.dumps({"resource":"1.1.1.4"}), headers=header).json())

