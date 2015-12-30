package com.carpool.carpool;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by isaaclong on 10/12/15.
 *
 * Used for basic http network requests to our server.
 * For now, this will just make requests to specific, server-side scripts.
 *
 * Edited to extend AsyncTask, include more descriptive error handling,
 * and have the option of blocking with a java lock.
 * by Ben Davis on 12/16/15.
 */
public class NetworkRequest extends AsyncTask<Void,Boolean,Boolean> {
    private String hostname; // our server --> http://45.55.29.36/
    private String scriptName;
    private String httpMethod;
    private Boolean isBlockingCall;
    private JSONArray payload;
    private JSONArray response;
    public Semaphore sendLock;

    /**
     *
     * @param operation         --type of change to make to database
     * @param httpRequestType   --type of httpRequest to execute (GET, POST, etc)
     * @param obj               --arguments to send to server side scripts
     *
     */
    NetworkRequest(String operation, String httpRequestType, Boolean block, JSONObject obj){
        hostname = "http://45.55.29.36/";
        response = new JSONArray();
        scriptName = operation;
        isBlockingCall = block;
        httpMethod = httpRequestType;
        payload = new JSONArray();
        payload.put(obj);
        sendLock = new Semaphore(1);
    }


    @Override
    protected void onPreExecute(){
        if(isBlockingCall) {
            sendLock.acquireUninterruptibly();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params){
        try {
            response = this.send();
        } catch (Exception e) {
            e.printStackTrace();
            response.put(e.getMessage());
            return false;
        }
        return true;
    }

    public JSONArray getResponse() { return this.response; }


    /** script name is the path to the script on the server
     *  http method is the method name, e.g. GET, POST
     *  payload is what we are sending in the case of a POST or a PUT, will be empty otherwise
     *  returns "response" which is a JSONArray containing either data from the server
     *  or an error message, both in the form of a JSONObject.
     */
    public JSONArray send() throws JSONException, IOException {

        URL url = new URL(hostname + scriptName);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        JSONArray serverResponse = new JSONArray();

        urlConn.setRequestMethod(httpMethod);
        urlConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        urlConn.setDoInput(true);

        if(httpMethod.equals("POST")) {
            // if it's a post, write data first. Data should be sent and received in JSON UTF-8
            urlConn.setDoOutput(true);
            urlConn.setChunkedStreamingMode(0);
            if(urlConn.getOutputStream() == null) System.out.println("We got problems!");
            OutputStream out = new BufferedOutputStream(urlConn.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(payload.toString());

            writer.close();
            out.close();
        }

        // Handle response from server
        InputStream in = new BufferedInputStream(urlConn.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;


        while((line = reader.readLine()) != null) {
            JSONObject j;
            j = new JSONObject(line); // assuming guaranteed JSON responses from our server
            serverResponse.put(j);
        }

        reader.close();
        in.close();
        urlConn.disconnect();

        return serverResponse;
    }

    @Override
    protected void onPostExecute(Boolean status){
        if(isBlockingCall){
            sendLock.release();
        }

        if(!status){
            System.out.println("Error in doInBackground, continuing execution...");
        }
        System.out.println("NetworkRequest Thread Done");
    }
}
