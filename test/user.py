import requests
import json


class TestUser(object):
  
    def __init__(self, broker_rest_ip):

        self.broker_rest_ip = broker_rest_ip

        self.broker_rest_base_url = "http://" + self.broker_rest_ip + "/db/"
        self.broker_rest_url_resources = self.broker_rest_base_url + "resources/json"

        self.broker_rest_header = {"Content-type": "application/json", "Accept": "text/plain"}

        self.base_resource_ip = "1.1.1."
        self.MAX_RESOURCES = 256
        self.n_resources = 0


    def create_resource(self):
        if self.n_resources < self.MAX_RESOURCES:
            #  access REST interface /resources - POST to create a resource at IP range 1.1.1.0/24
            r = requests.post(self.broker_rest_url_resources, json.dumps({}), headers=self.broker_rest_header) #  send the broker a message to create a resource
            if (r.status_code == 200):  # success
                print("REST server response: ")
                print(r.text)
                self.n_resources += 1
            else: #  failed to create the resource
                print(r.text)
        else:
            print("max number of resources created")


    def delete_resource(self, resourceIP):
        #  access REST interface /resources - DELETE to remove a resource
        controller_ip = "192.168.122.1:8080"
        base_url = "http://" + controller_ip + "/db/"
        header = {"Content-type": "application/json", "Accept": "text/plain"}
        url_resources = base_url + "resources/json"
        print(requests.delete(url_resources, data=json.dumps({"resource": '"' + str(resourceIP) + '"'}), headers=header).json())

  
    def get_resources(self):
        #  access REST interface /resources - GET to retrieve deployed Virtual IPs (Resources)
        r = requests.get(self.broker_rest_url_resources, json.dumps({}), headers=self.broker_rest_header)
        if (r.status_code == 200):  # success
            print("REST server response: ")
            print(r.text)
        else: #  failed to get resources
            print(r.text)


    def subscribe_cn(self, resource_ip, sub_ip, sub_mac):
        #  access REST interface /subscribers - POST to add a subscriber to a resource
        rest_addr = self.broker_rest_base_url + "/subscribers/" + resource_ip + "/json"
        r = requests.post(rest_addr, json.dumps({"address": sub_ip, "MAC": sub_mac}), headers=self.broker_rest_header)
        if (r.status_code == 200):  # success
            print("REST server response: ")
            print(r.text)
        else: #  failed to get resources
            print(r.text)


    def get_subscribers (self, resource_ip):
        #  access REST interface /subscribers - GET to retrieve the list of subscribers in a resource
        rest_addr = self.broker_rest_base_url + "/subscribers/" + resource_ip + "/json"
        #r = requests.get(rest_addr, json.dumps({"address": sub_ip, "MAC": sub_mac}), headers=self.broker_rest_header)
        r = requests.get(rest_addr, headers=self.broker_rest_header)
        if (r.status_code == 200):  # success
            print("REST server response: ")
            print(r.text)
        else: #  failed to get resources
            print(r.text)


    def remove_cn(self, resourceIP, userMAC):
        #  access REST interface /subscribers - DELETE to remove the subscriber from a resource
        controller_ip = "192.168.122.1:8080"
        base_url = "http://" + controller_ip + "/db/"
        header = {"Content-type": "application/json", "Accept": "text/plain"}
        print(requests.delete(base_url + "/subscribers/" + str(resourceIP) + "/json", data=json.dumps({"MAC": str(userMAC)}), headers=header).json())
        
        
        

