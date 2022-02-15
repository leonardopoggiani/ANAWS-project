import requests
import json


class TestUser(object):
  
    def __init__(self, broker_rest_ip):

        self.broker_rest_ip = broker_rest_ip

        self.broker_rest_base_url = "http://" + self.broker_rest_ip + "/db/"
        self.broker_rest_url_resources = self.broker_rest_base_url + "resources/json"

        self.broker_rest_header = {"Content-type": "application/json", "Accept": "text/plain"}
        self.broker_rest_url_subs_lst = []
        self.resources_ip_lst = []
        self.n_resources = 0
        self.base_resource_ip = "1.1.1."
        self.MAX_RESOURCES = 100


    def create_resource(self):
        if self.n_resources < self.MAX_RESOURCES:
            r = requests.post(self.broker_rest_url_resources, json.dumps({}), headers=self.broker_rest_header) #  send the broker a message to create a resource
            if (r.status_code == 200):  # success
                print(r.text)
                self.n_resources += 1
                self.resources_ip_lst.append(self.base_resource_ip + str(self.n_resources))  # append resouce end ip addr to base ip addr
                #  add the resource ip to the subscriber addition rest interface
                self.broker_rest_url_subs_lst.append(self.broker_rest_base_url + "/subscribers/" + self.resources_ip_lst[self.n_resources-1] + "/json")
                print("resource created at: " + self.resources_ip_lst[self.n_resources-1])
            else: #  failed to create the resouce
                print(r.text)
        else:
            print("max number of resources created")


    def delete_resource(self, data):
        print("TODO")

  
    def get_resources(self):
        if resource_n <= self.n_resources and resource_n > 0:
            r = requests.get(self.broker_rest_url_resources, json.dumps({}), headers=self.broker_rest_header)
            print(r.text)
        else:
            print("invalid resource number")

    # resources number starts from 1
    def subscribe_cn(self, resource_n, sub_ip, sub_mac):
        if resource_n <= self.n_resources and resource_n > 0:
            print("subscribe user at resource (Virtual Address): " + self.resources_ip_lst[resource_n-1])
            r = requests.post(self.broker_rest_url_subs_lst[resource_n-1], json.dumps({"address": sub_ip, "MAC": sub_mac}), headers=self.broker_rest_header)
            print(r.text)
        else:
            print("invalid resource number")


    def remove_cn(self, ):
        print("TODO")


    def get_subscribers_at_resource (self, resource_n):
        if resource_n <= self.n_resources and resource_n > 0:
            r = requests.get(self.broker_rest_url_subs_lst[resource_n-1], json.dumps({}), headers=self.broker_rest_header)
            print(r.text)
        else:
            print("invalid resource number")























        #
