package com.product.simpleq_drive;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

//implementing onclicklistener
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //View Objects
    private Button buttonScan;
    private TextView textViewName, textViewAddress;
    private String serverDomain = "http://13.52.76.220:8080/";
    private int busID = 1001;
    private int busCapacity = 50;
    TextToSpeech t1;
    //qr code scanner object
    private IntentIntegrator qrScan;

    private Map<String, Integer> usertoQNo = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //View objects
        buttonScan = (Button) findViewById(R.id.buttonScan);

        Log.d("akhi", "Init 1");
        //intializing scan object
        qrScan = new IntentIntegrator(this);

        //attaching onclick listener
        Log.d("akhi", "Btn listener");
        t1 =new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
        buttonScan.setOnClickListener(this);
        new getBuses().execute(serverDomain + "fetchbuses");
        new getUsers().execute(serverDomain + "fetchuserqueue?busID=" + busID);
    }

    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
        Log.d("akhi " , " DATAAA777 " + result.getContents());
        if (result != null) {
            try {
                String userUID = result.getContents();
                Integer qNo = usertoQNo.get(userUID);
                if(qNo == null || qNo <= 0) {
                    qNo = Integer.MAX_VALUE;
                }
                Log.d("akhi", "DATATADA " + usertoQNo);
                Log.d("akhi" , " DATAAA 1111" + userUID + " asdasd " + qNo);
                if (qNo > busCapacity) {
                    Log.d("akhi" , " DATAAA 222222" + userUID);
                    Toast.makeText(this, "You don't have a reservation. Kindly wait for the next bus.", Toast.LENGTH_LONG).show();
                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_RING, 100);
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                    Thread.sleep(500);
                    ToneGenerator toneGen2 = new ToneGenerator(AudioManager.STREAM_RING, 100);
                    toneGen2.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                    Thread.sleep(500);
                    ToneGenerator toneGen3 = new ToneGenerator(AudioManager.STREAM_RING, 100);
                    toneGen3.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                    t1.speak("You don't have a reservation. Kindly wait for the next bus", TextToSpeech.QUEUE_FLUSH, null);
                    buttonScan.performClick();

                } else {
                    Log.d("akhi" , " DATAAA 3333" + userUID);
                    t1.speak("Kindly proceed, Have a nice day.", TextToSpeech.QUEUE_FLUSH, null);
                    Toast.makeText(this, "Kindly proceed", Toast.LENGTH_LONG).show();
                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                    buttonScan.performClick();
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    public void onClick(View view) {
        //initiating the qr code scan
        qrScan.initiateScan();
    }

    public class getBuses extends AsyncTask<String , Void ,String> {
        public String server_response;

        @Override
        protected String doInBackground(String... strings) {

            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                int responseCode = urlConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK){
                    server_response = readStream(urlConnection.getInputStream());
                    Log.d("CatalogClient", server_response);
                    JSONObject response = new JSONObject(server_response);
                    JSONObject buses = response.getJSONObject("buses");
                    JSONArray bussesArr = buses.getJSONArray("buses");
                    for(int i = 0 ; i < bussesArr.length(); i++){
                        JSONObject bus = bussesArr.getJSONObject(i);
                        int id = bus.getInt("id");
                        if(id == busID) {
                            int capacity = bus.getInt("capacity");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return server_response;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.e("Response", "" + server_response);


        }

// Converting InputStream to String

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
    }

    public class getUsers extends AsyncTask<String , Void ,String> {
        public String server_response;

        @Override
        protected String doInBackground(String... strings) {
            Log.d("akhi", "---akhi testttt");
            URL url;
            try {
                url = new URL(strings[0]);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    JSONObject obj = convertAsJSON(convertToReadableStream(urlConnection.getContentEncoding(), in));
                    String decodedBusesJsonString = URLDecoder.decode(obj.getString("response"));
                    JSONObject usersToQNo = new JSONObject(decodedBusesJsonString);
                    Iterator userToQNoIterator = usersToQNo.keys();
                    Log.d("akhi", "---akhi" +  decodedBusesJsonString);
                    while(userToQNoIterator.hasNext()){
                        String userUID = (String) userToQNoIterator.next();
                        int qNo = (Integer) usersToQNo.get(userUID);
                        usertoQNo.put(userUID, qNo);
                        Log.d("akhi", "---akhi" + userUID + " " + qNo);

                    }
//		     System.out.println(obj.toString());
//		     System.out.println(decodedBusesJsonString);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return server_response;


        }

    private InputStream convertToReadableStream(String contentEncoding, InputStream inputStream) throws Exception {
        return contentEncoding != null && contentEncoding.contains("gzip") ? new GZIPInputStream(inputStream) : inputStream;
    }


    private JSONObject convertAsJSON(InputStream is) throws Exception {
        BufferedReader bufReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String str = null;
        while ((str = bufReader.readLine()) != null) {
            builder.append(str);
        }
        return new JSONObject(builder.toString());
    }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.e("Response", "" + server_response);


        }

// Converting InputStream to String

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
    }
}

