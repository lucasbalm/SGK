package com.android.app.sgk;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.InputStream;
import java.net.URL;

/**
 * Created by Edward on 5/9/2016.
 */
public class UserAction extends AppCompatActivity {

    private static final String TAG = "UserAction";

    //buttons
    Button accept;
    Button reject;

    //image variables
    Bitmap bmp;
    ImageView iv;
    String image_url;


    public UserAction(){}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_action);


        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        image_url = pref.getString("URL", "");

        Log.d("IMAGE", image_url);

        iv = (ImageView)findViewById(R.id.Imageprev);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    InputStream in = new URL(image_url).openStream();
                    bmp = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    // log error
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (bmp != null)
                    iv.setImageBitmap(bmp);
            }

        }.execute();


        //Accept or Reject User
        accept = (Button)findViewById(R.id.btnAccept);
        reject = (Button)findViewById(R.id.btnReject);

        accept.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MQTT_SEND("Access Granted");
            }
        });

        reject.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MQTT_SEND("Access Denied");
            }
        });

    }

    private void MQTT_SEND(String s) {
        String topic        = "Result";
        String content      = s;
        int qos             = 2;
        String broker       = "tcp://52.33.59.166:1883";
        String clientId     = "Android";
        MemoryPersistence persistence = new MemoryPersistence();
        Log.d("Ans", s);

        if(content != null){
            try {
                MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                Log.d("Connect", "broker");
                sampleClient.connect(connOpts);
                Log.d("Connect", "connected");
                Log.d("Connect", "broker");
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                sampleClient.disconnect();
                //Use to close App
                //System.exit(0);
            } catch(MqttException me) {
                System.out.println("reason "+me.getReasonCode());
                System.out.println("msg "+me.getMessage());
                System.out.println("loc "+me.getLocalizedMessage());
                System.out.println("cause "+me.getCause());
                System.out.println("excep "+me);
                me.printStackTrace();
            }
        }
    }

}
