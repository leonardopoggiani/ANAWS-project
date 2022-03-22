from user import TestUser

REST_IP_ADDR = "192.168.122.1:8080"

if __name__ == '__main__':

    usr = TestUser(REST_IP_ADDR)
    
    ## add resources to the broker
    usr.create_resource() # resource at 1.1.1.1
    usr.create_resource() # resource at 1.1.1.2
    ## check created resources
    usr.get_resources()

    ## subscribe computing nodes
    usr.subscribe_cn("1.1.1.1", "10.0.0.1", "00:00:00:00:00:01")
    usr.subscribe_cn("1.1.1.1", "10.0.0.2", "00:00:00:00:00:02")
    usr.get_subscribers("1.1.1.1")
    
    usr.subscribe_cn("1.1.1.2", "10.0.0.5", "00:00:00:00:00:05")
    usr.subscribe_cn("1.1.1.2", "10.0.0.4", "00:00:00:00:00:04")
    usr.get_subscribers("1.1.1.2")
