package com.android.app.sgk;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

/**
 * Created by Edward on 5/9/2016.
 */

//Class to register device using service ID
public class RegistrationService extends IntentService {

    public RegistrationService(){super("RegistrationService");}

    @Override
    protected void onHandleIntent(Intent intent) {
        //Token
        InstanceID myID = InstanceID.getInstance(this);

        try {
            String registrationToken = myID.getToken(
                    "897831526404",
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE,
                    null
            );

            Log.d("Registration Token", registrationToken);

        }catch(Exception e){
            Log.e("REGISTRATION ERROR", e.toString());
        }
    }
}
