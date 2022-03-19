import requests
import json

controller_ip = "192.168.122.1:8080"
base_url = "http://" + controller_ip + "/db/"
header = {"Content-type": "application/json", "Accept": "text/plain"}

url_resources = base_url + "resources/json"
url_subscribers = base_url + "/subscribers/{resource}/json"

# Subscribe host to resources

# resource 1.1.1.1
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.1", "MAC": "00:00:00:00:00:01"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.2", "MAC": "00:00:00:00:00:02"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.1/json", json.dumps({"address": "10.0.0.3", "MAC": "00:00:00:00:00:03"}), headers=header).json())

# resource 1.1.1.2

# multiple subscription for a resource (
print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.1", "MAC": "00:00:00:00:00:01"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.2", "MAC": "00:00:00:00:00:02"}), headers=header).json())
# )

print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.4", "MAC": "00:00:00:00:00:04"}), headers=header).json())

# resource 1.1.1.3
print(requests.post(base_url + "/subscribers/1.1.1.3/json", json.dumps({"address": "10.0.0.5", "MAC": "00:00:00:00:00:05"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.3/json", json.dumps({"address": "10.0.0.6", "MAC": "00:00:00:00:00:06"}), headers=header).json())

# resource 1.1.1.4
print(requests.post(base_url + "/subscribers/1.1.1.4/json", json.dumps({"address": "10.0.0.7", "MAC": "00:00:00:00:00:07"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.4/json", json.dumps({"address": "10.0.0.8", "MAC": "00:00:00:00:00:08"}), headers=header).json())

# trying to subscribe to a resource that you have already subscribed to
print(requests.post(base_url + "/subscribers/1.1.1.4/json", json.dumps({"address": "10.0.0.7", "MAC": "00:00:00:00:00:07"}), headers=header).json())
print(requests.post(base_url + "/subscribers/1.1.1.2/json", json.dumps({"address": "10.0.0.4", "MAC": "00:00:00:00:00:04"}), headers=header).json())
