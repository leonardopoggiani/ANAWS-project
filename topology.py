from __future__ import print_function

import os
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node
from mininet.log import setLogLevel, info
from mininet.cli import CLI
from mininet.link import Intf
from mininet.node import RemoteController, OVSKernelSwitch, Host


def run():
        
    net = Mininet( ipBase="10.0.1.0/8" )
    info("*** Adding controller\n")
    c1 = net.addController(name="c1",
                           controller=RemoteController,
                           protocol="tcp",
                           ip="127.0.0.1",
                           port=6653)

    info("*** Adding switches\n")
    s1 = net.addSwitch("s1", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:01:01", protocols="OpenFlow13")
    s2 = net.addSwitch("s2", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:01:02", protocols="OpenFlow13")
    s3 = net.addSwitch("s3", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:01:03", protocols="OpenFlow13")
    s4 = net.addSwitch("s4", cls=OVSKernelSwitch, dpid="00:00:00:00:00:00:01:04", protocols="OpenFlow13")

    info("*** Adding hosts\n")
    h1 = net.addHost("h1", cls=Host, ip="10.0.0.1", mac="00:00:00:00:00:01", defaultRoute="h1-eth0")
    h2 = net.addHost("h2", cls=Host, ip="10.0.0.2", mac="00:00:00:00:00:02", defaultRoute="h2-eth0")
    h3 = net.addHost("h3", cls=Host, ip="10.0.0.3", mac="00:00:00:00:00:03", defaultRoute="h3-eth0")
    h4 = net.addHost("h4", cls=Host, ip="10.0.0.4", mac="00:00:00:00:00:04", defaultRoute="h4-eth0")
    h5 = net.addHost("h5", cls=Host, ip="10.0.0.5", mac="00:00:00:00:00:05", defaultRoute="h5-eth0")
    h6 = net.addHost("h6", cls=Host, ip="10.0.0.6", mac="00:00:00:00:00:06", defaultRoute="h6-eth0")
    h7 = net.addHost("h7", cls=Host, ip="10.0.0.7", mac="00:00:00:00:00:07", defaultRoute="h7-eth0")
    h8 = net.addHost("h8", cls=Host, ip="10.0.0.8", mac="00:00:00:00:00:08", defaultRoute="h8-eth0")

    info("*** Creating links\n")

    net.addLink(s1, s4)
    net.addLink(s2, s4)
    net.addLink(s3, s4)

    net.addLink(h1, s1)
    net.addLink(h2, s1)

    net.addLink(h3, s2)
    net.addLink(h4, s2)

    net.addLink(h5, s3)
    net.addLink(h6, s3)

    net.addLink(h7, s4)
    net.addLink(h8, s4)

    info("*** Starting network\n")
    net.build()
    c1.start()
    s1.start([c1])
    s2.start([c1])
    s3.start([c1])
    s4.start([c1])

    CLI( net )

    net.stop()


if __name__ == '__main__':
    setLogLevel( 'info' )
    run()

