package com.br.sensorstream;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements SensorEventListener{
    private SensorManager mSensorManager;
    private Sensor mOrientation;
    private ToggleButton buttonSwitch;
    private EditText IPeditText, portEditText;
    private Button button1, button2, button3, button4;
    private TextView textOrientX, textOrientY, textOrientZ;


    // concurrency
    private ExecutorService executor;
    private BlockingQueue<UDPPacket> queue;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        queue = null;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_GAME);
        buttonSwitch = (ToggleButton) findViewById(R.id.buttonSwitch);
        textOrientX = (TextView) findViewById(R.id.OrientX);
        textOrientY = (TextView) findViewById(R.id.OrientY);
        textOrientZ = (TextView) findViewById(R.id.OrientZ);

        IPeditText = (EditText) findViewById(R.id.IPeditText);
        portEditText = (EditText) findViewById(R.id.portEditText);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);


        buttonSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToggleButton t = (ToggleButton) view;
                boolean checked = t.isChecked();
                if(checked){
                    try {
                        // validate ip address
                        String IP = IPeditText.getText().toString().replace(" ", "");
                        Matcher matcher = Patterns.IP_ADDRESS.matcher(IP);
                        if (!matcher.matches()){
                            throw new UnknownHostException();
                        }
                        InetAddress inetAddress = InetAddress.getByName(IP);
                        DatagramSocket socket = new DatagramSocket();
                        int port = Integer.parseInt(portEditText.getText().toString());
                        NetworkParams params = new NetworkParams(socket, inetAddress, port);
                        executor = Executors.newSingleThreadExecutor();
                        queue = new LinkedBlockingQueue<UDPPacket>(10);
                        executor.submit(new UDPStream(params, queue));
                        enableFields(false);

                    } catch (UnknownHostException e) {
                        Toast.makeText(MainActivity.this, "Error, Invalid IP!",
                                Toast.LENGTH_LONG).show();
                        enableFields(true);
                        // e.printStackTrace();
                        t.setChecked(false);
                    } catch (SocketException e) {
                        Toast.makeText(MainActivity.this, "Error, Socket Problem!",
                                Toast.LENGTH_LONG).show();
                        enableFields(true);
                        // e.printStackTrace();
                        t.setChecked(false);
                    } catch (NumberFormatException e){
                        Toast.makeText(MainActivity.this, "Error, please provide a valid port number!",
                                Toast.LENGTH_LONG).show();
                        t.setChecked(false);
                    }

                }
                else{
                    queue = null;
                    executor.shutdownNow();
                    enableFields(true);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == mOrientation){

            // Convert the rotation-vector to a 4x4 matrix.
            float[] mRotationMatrix = new float[16];
            // Convert the rotation-vector to a 4x4 matrix.
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, sensorEvent.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z,
                    mRotationMatrix);
            float[] orientationVals = new float[3];
            SensorManager.getOrientation(mRotationMatrix, orientationVals);


            // Optionally convert the result from radians to degrees
            orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
            orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
            orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);

            textOrientX.setText(String.format("%.3f", orientationVals[0]));
            textOrientY.setText(String.format("%.3f", orientationVals[1]));
            textOrientZ.setText(String.format("%.3f", orientationVals[2]));

            if(queue != null){
                queue.offer(new UDPPacket(orientationVals[0], orientationVals[1], orientationVals[2],
                        button1.isPressed(), button2.isPressed(), button3.isPressed(), button4.isPressed()));
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void enableFields(boolean enable){
        IPeditText.setEnabled(enable);
        portEditText.setEnabled(enable);
    }



    private class UDPStream implements Runnable {
        private NetworkParams params;
        private BlockingQueue<UDPPacket> queue;

        public UDPStream(NetworkParams params, BlockingQueue<UDPPacket> queue) {
            this.params = params;
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    UDPPacket packet = this.queue.take();
                    String msg = packet.toJSON();
                    int msg_length = msg.length();
                    byte[] message = msg.getBytes();
                    DatagramPacket p = new DatagramPacket(message, msg_length, this.params.getInetAddress(), this.params.getPort());
                    this.params.getSocket().send(p);
                }
                catch(Exception ex){
                    Toast.makeText(MainActivity.this, "Error sending data!" + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                }
            }

        }

    }

    private class NetworkParams {
        private DatagramSocket socket;
        private InetAddress inetAddress;
        private int port;

        public NetworkParams(DatagramSocket socket, InetAddress inetAddress, int port) {
            this.socket = socket;
            this.inetAddress = inetAddress;
            this.port = port;
        }

        public DatagramSocket getSocket() {
            return socket;
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }

        public int getPort() {
            return port;
        }
    }

}
