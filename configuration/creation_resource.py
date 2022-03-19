import requests
import json

controller_ip = "192.168.122.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_resources = base_url + "resources/json"
url_subscribers = base_url + "/subscribers/{resource}/json"

# Create 4 resources
print(requests.post(url_resources, data=json.dumps({}), headers=header).json())
print(requests.post(url_resources, json.dumps({}), headers=header).json())
print(requests.post(url_resources, data=json.dumps({}), headers=header).json())
print(requests.post(url_resources,data=json.dumps({}), headers=header).json())