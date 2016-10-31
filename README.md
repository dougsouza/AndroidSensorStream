# AndroidSensorStream


A (very) simple app that streams the device's orientation along with 4 buttons states to a IP:Port using UDP protocol. The orientation is obtained from the Rotation Vector virtual sensor. The axes of the device are defined [this way](https://developer.android.com/images/axis_device.png).

The data format sent to the specified IP:Port is:
```
{
  orientX: float,
  orientY: float,
  orientZ: float,
  button1: boolean,
  button2: boolean,
  button3: boolean,
  button4: boolean
}
```

Orientantion is in degrees, the state of the buttons are: pressed (true) not pressed (false).

A simple program to receive (and process) the data could be (in Node.js):
```
var dgram = require('dgram');
var server = dgram.createSocket('udp4');
var port = 41234;
server.on('message', function(msg, rinfo){
  var data = JSON.parse(msg.toString());
  console.log(data);
});
server.bind(port);
```
