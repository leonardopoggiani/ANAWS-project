from user import TestUser

REST_IP_ADDR = "192.168.122.1:8080"
DUMMY_SERVER = "127.0.0.2:5000"

if __name__ == '__main__':

    usr = TestUser(DUMMY_SERVER)
    usr.create_resource()
    usr.subscribe_cn(1, "10.0.0.1", "00:00:00:00:00:01")
    usr.subscribe_cn(1, "10.0.0.2", "00:00:00:00:00:02")
    usr.create_resource()
    usr.subscribe_cn(2, "10.0.0.3", "00:00:00:00:00:03")
    usr.subscribe_cn(2, "10.0.0.4", "00:00:00:00:00:04")


    usr.subscribe_cn(3, "10.0.0.5", "00:00:00:00:00:05")
