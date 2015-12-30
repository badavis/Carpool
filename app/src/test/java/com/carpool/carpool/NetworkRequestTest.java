package com.carpool.carpool;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.httpclient.FakeHttp;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Benjamin on 12/18/2015.
 * ATTENTION: NONE OF THIS ACTUALLY WORKS HOW YOU WOULD EXPECT
 * -- I've only discovered after writing these tests that Roboelectric runs single threaded.
 *    This makes these tests somewhat useless, as they don't simulate actual run time conditions.
 *    I've still been able to find some utility in the lock test, especially in trying to test/find deadlock scenarios.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class NetworkRequestTest {

    private JSONObject buildExpectedResult(String... params){
        JSONObject obj = new JSONObject();
        for(int i = 0; i < params.length; i += 2){
            try {
                obj.put(params[i],params[i+1]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    //Currently fails no matter what because Roboelectric and HttpURLConnection don't play together nicely
    /**
    @Test
    public void networkRequestVerifyTest(){
        FakeHttp.getFakeHttpLayer().interceptHttpRequests(false); //tell Roboelectric to let http calls through
        JSONObject arguments = new JSONObject();
        JSONArray expectedResponse = new JSONArray();

        expectedResponse.put(buildExpectedResult("status", "ok", "UID", "62"));
        try {
            arguments.put("Users", "");
            arguments.put("email", "bjamin.davis@gmail.com");
            arguments.put("password", "password");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        NetworkRequest testRequest = new NetworkRequest("../cgi-bin/db-verify.py", "POST", true, arguments);
        testRequest.execute();
        assertThat(testRequest.getResponse(), equalTo(expectedResponse));
    }**/

    @Test
    public void networkRequestLockTest(){
        FakeHttp.getFakeHttpLayer().interceptHttpRequests(false); //tell Roboelectric to let http calls through
        JSONObject arguments = new JSONObject();
        JSONArray expectedResponse = new JSONArray();

        expectedResponse.put(buildExpectedResult("status", "ok", "UID", "62"));
        try {
            arguments.put("Users", "");
            arguments.put("email", "bjamin.davis@gmail.com");
            arguments.put("password", "password");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        NetworkRequest testRequest = new NetworkRequest("../cgi-bin/db-verify.py", "POST", true, arguments);
        testRequest.execute();
        while(!testRequest.sendLock.tryAcquire()){
            System.out.println("Lock held by AsyncTask");
        }
        System.out.println("UI Thread Done");
    }
}
