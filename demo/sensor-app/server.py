import socket
import fcntl
import struct
import psutil

UDP_PORT = 5005
ACTUATORS_IP = "1.1.1.2"

moving_avg_size = 10
avg_array = [0]*moving_avg_size
temp_threshold = 24

# retrieve ip address of an ifname interface
def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])

# retrieve all the interfaces of the host
addrs = psutil.net_if_addrs()

for address in addrs:
    # i'm searching the interface h[x]-eth0 where x is the host-number
    if address != "lo" and address.split("-")[1] == "eth0":
        UDP_IP = get_ip_address(address)

print("Server IP addr: " + UDP_IP)

sock = socket.socket(socket.AF_INET, # Internet
                     socket.SOCK_DGRAM) # UDP
sock.bind((UDP_IP, UDP_PORT))

# wait for a message
n = 0
while True:
    data, addr = sock.recvfrom(1024) # buffer size is 1024 bytes
    print("Temperature Sensor Reading (C): %s" % data)
    # moving average
    avg_array[n] = float(data)
    n += 1
    if n > moving_avg_size-1:
        temp_sum = 0
        for i in avg_array:
            temp_sum += i
        print("Moving Average (C): "+ str(round(temp_sum/moving_avg_size, 2)) + "\n")
        n = 0
