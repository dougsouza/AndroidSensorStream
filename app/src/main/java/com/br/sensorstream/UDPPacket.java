package com.br.sensorstream;

import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by it on 27/10/16.
 */

public class UDPPacket {
    private float orientX, orientY, orientZ;
    private boolean button1, button2, button3, button4;

    public UDPPacket(float orientX, float orientY, float orientZ, boolean button1, boolean button2,
                     boolean button3, boolean button4) {
        this.orientX = orientX;
        this.orientY = orientY;
        this.orientZ = orientZ;
        this.button1 = button1;
        this.button2 = button2;
        this.button3 = button3;
        this.button4 = button4;
    }

    public String toJSON(){
        JSONObject JSON = new JSONObject();
        try {
            JSON.put("orientX", this.orientX);
            JSON.put("orientY", this.orientY);
            JSON.put("orientZ", this.orientZ);
            JSON.put("button1", this.button1);
            JSON.put("button2", this.button2);
            JSON.put("button3", this.button3);
            JSON.put("button4", this.button4);
        } catch (JSONException e){
            e.printStackTrace();
        }
        return JSON.toString();
    }
}
