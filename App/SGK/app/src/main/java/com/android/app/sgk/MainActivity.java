package com.android.app.sgk;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;


import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    //User ID
    private String user_name = " ";
    EditText text;

    //Buttons
    Button capture;
    Button register;

    //Picture variables
    String mCurrentPhotoPath;
    ImageView mImageView;
    String ba1;
    Bitmap bitmap;
    JSONObject jsonObject;

    //Config
    String SENDER_ID = "660494153901";
    //SERVER API KEY: AIzaSyDjTUYDqTyD0jKupmUMLxA2MoGBMTq3gxE;

    //String SENDER_ID = "824217929011";
    //SERVER API KEY: AIzaSyD6J35kHbIJdA53mEHw4vpgP6TxAsb0m8s

    private static final String TAG = "MainActivity";

    public static final String UPLOAD_URL = "http://52.67.130.72:3000/addOwner";
    public static final String UPLOAD_KEY = "image";
    public static final String USER_KEY = "userName";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Dialog with Instructions
        android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(this);
        alertDialog.setTitle("Notice");
        alertDialog.setMessage("For Each User, Please Take 6-8 Pictures for Better Recognition.");
        alertDialog.setCancelable(true);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        android.app.AlertDialog alert = alertDialog.create();
        alert.show();

        //Token
        getGCMToken();

        //User Name
        text = (EditText)findViewById(R.id.editText);
        Log.d("UserName", user_name);

        //Take Picture Button
        capture = (Button) findViewById(R.id.btnCapture);
        capture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                take_picture();
            }
        });

        //Register User Button
        register = (Button)findViewById(R.id.btnUpload);
        mImageView = (ImageView)findViewById(R.id.Imageprev);
        register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Check if user name and picture
                user_name = text.getText().toString();
                Log.d("Clicked", user_name);
                if (user_name != null && !user_name.isEmpty() && !user_name.equals("User Name") && mImageView.getDrawable() != null) {
                    Log.d("SuccessClick", user_name);
                    uploadUser();
                } else {
                    Toast.makeText(getApplicationContext(), "Need Picture and User Name!", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    //Take picture action
    private void take_picture(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, 100);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == RESULT_OK) {
            setPic();
        }
    }

    @SuppressWarnings("deprecation")
    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        //int targetW = 250;
        //int targetH = 250;

        //Log.d("W", Integer.toString(targetW));
        //Log.d("H", Integer.toString(targetH));

        //Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        //int photoW = 250;
        //int photoH = 250;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        mImageView.setImageBitmap(bitmap);

        bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        //bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions), 250, 250, true);

        mImageView.setImageBitmap(bitmap);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
        String imageFileName = user_name + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        //Log.d("Getpath1",  Long.toString(photoFile.length()));
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.e("Getpath", "Cool" + mCurrentPhotoPath);

        return image;
    }

    private String getStringImage(Bitmap bm) {
        bm = BitmapFactory.decodeFile(mCurrentPhotoPath);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        //change quality
        bm.compress(Bitmap.CompressFormat.JPEG, 50, bao);
        byte[] ba = bao.toByteArray();
        ba1 = Base64.encodeToString(ba, Base64.DEFAULT);

        return ba1;
    }

    public void uploadUser(){
        class UploadImage extends AsyncTask<Bitmap,Void,String>{

            ProgressDialog loading;
            RequestHandler rh = new RequestHandler();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Uploading Image", "Please wait...",true,true);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(),"Upload Successful",Toast.LENGTH_LONG).show();
            }

            @Override
            protected String doInBackground(Bitmap... params) {
                Bitmap bitmap = params[0];
                String uploadImage = getStringImage(bitmap);

                try {
                    jsonObject = new JSONObject();
                    jsonObject.put(USER_KEY, user_name);
                    jsonObject.put(UPLOAD_KEY, uploadImage);
                    String data = jsonObject.toString();

                    URL url = new URL(UPLOAD_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestMethod("POST");
                    connection.setFixedLengthStreamingMode(data.getBytes().length);
                    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write(data);
                    Log.d("Ed", "Data to node = " + data);
                    writer.flush();
                    writer.close();
                    out.close();
                    Log.d("Ed", "Close");
                    connection.connect();

                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    in.close();
                    String result = sb.toString();
                    Log.d("Ed", "Response from node = " + result);
                    //Response = new JSONObject(result);
                    connection.disconnect();
                } catch (Exception e) {
                    Log.d("Ed", "Error Encountered");
                    e.printStackTrace();
                }
                return null;
            }
        }

        UploadImage ui = new UploadImage();
        ui.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, Bitmap.createScaledBitmap(bitmap, 250, 250, true));
        //ui.execute(Bitmap.createScaledBitmap(bitmap, 250, 250, true));
    }

    //Get token
    private void getGCMToken() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
                    String token = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    Log.d("GCM Token", token);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

}

