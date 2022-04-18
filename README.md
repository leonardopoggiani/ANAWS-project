# ANAWS-project
Repository for the ANAWS project.

## How to run the demo
- start the patched Floodlight controller 
- run the *demo/config_sensor_test.py* using `python config_sensor_test.py` in order to configure the network creating some resources and subscribing some host to these resources

![config output](https://github.com/leonardopoggiani/ANAWS-project/blob/main/docs/expected_output/config_output.png?raw=true "Configuration output").

![config output](https://github.com/leonardopoggiani/ANAWS-project/blob/main/docs/expected_output/config_output_controller.png?raw=true "Configuration output at controller").

- run one of the topologies in the *mininet_topologies/* folder using `sudo python topology_2_sw.py` in order to build the Mininet topology 
- run the *actuator.py* app on the host h4 and h5
- run the *server_main.py* app on the host h1 and *server.py* h2
- run the *auto_sensor.py* app on the host h3

The demo starts when the *auto_sensor.py* application is launched and to terminate the execution you can just press CTRL + C on the corresponding terminal.
The flow of the demo execution is illustrated on the documentation (chapter *Testing*).

### Troubleshooting

A possible error may arise when executing the *auto_sensor.py* application because the python package *numpy* may not be installed. In order to install the package just run `pip install numpy` on a terminal of the VM ( not on the terminal of an host! ).
