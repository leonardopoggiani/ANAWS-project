from mininet.topo import Topo

class MyTopo(Topo):
    def __init__(self):

        # Initialize topology
        Topo.__init__(self)

        # Add hosts and switches
        leftHost = self.addHost('h1')
        rightHost = self.addHost('h2')
        anotherLeftHost = self.addHost('h5')
        leftSwitch = self.addSwitch('s3')
        rightSwitch = self.addSwitch('s4')


        # Add links
        self.addLink(leftHost, leftSwitch)
        self.addLink(leftSwitch, rightSwitch)
        self.addLink(anotherLeftHost, leftSwitch)
        self.addLink(rightSwitch, rightHost)

        self.plotGraph(max_x=100, max_y=100)

topos = {'mytopo': (lambda: MyTopo())}


