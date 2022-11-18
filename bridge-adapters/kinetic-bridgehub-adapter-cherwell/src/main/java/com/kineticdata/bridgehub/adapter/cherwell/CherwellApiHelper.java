package com.kineticdata.bridgehub.adapter.cherwell;

import com.kineticdata.bridgehub.adapter.BridgeError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a Rest service helper.
 */
public class CherwellApiHelper {
    private static final Logger LOGGER = 
        LoggerFactory.getLogger(CherwellApiHelper.class);
    
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String clientId;
    private String token;
    
    
    public CherwellApiHelper(String baseUrl, String clientId, String username, String password) {
        
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }
        
    public JSONObject executeGetRequest (String path) throws BridgeError{
        
        String url = baseUrl + path;
        JSONObject output;      
        // System time used to measure the request/response time
        long start = System.currentTimeMillis();
        
        try (
            CloseableHttpClient client = HttpClients.createDefault()
        ) {
            HttpResponse response;
            HttpGet get = new HttpGet(url);
            
            // Append HTTP BASIC Authorization header to HttpGet call
            get.setHeader("Content-Type", "application/json");
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + token);
            
            response = client.execute(get);
            LOGGER.debug("Recieved response from \"{}\" in {}ms.",
                url,
                System.currentTimeMillis()-start);

            int responseCode = response.getStatusLine().getStatusCode();
            LOGGER.trace("Request response code: " + responseCode);
            
            HttpEntity entity = response.getEntity();
            
            // Confirm that response is a JSON object
            output = parseResponse(EntityUtils.toString(entity));
            
            // Handle all other failed repsonses
            if (responseCode >= 400) {
                handleFailedRequest(responseCode);
            }
        }
        catch (IOException e) {
            throw new BridgeError(
                "Unable to make a connection to the Harvest service server.", e);
        }
        
        return output;
    }
    
    // Get a JWT to be used with subsequent requests.
    public void getToken () throws BridgeError {
        String url = baseUrl + "/token";
        
        try (
            CloseableHttpClient client = HttpClients.createDefault()
        ) {
            HttpResponse response;
            HttpPost httpPost = new HttpPost(url);
      
            // Create entity with username and pass for use in the Post.
            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("username", username));
            form.add(new BasicNameValuePair("password", password));
            form.add(new BasicNameValuePair("client_id", clientId));
            form.add(new BasicNameValuePair("grant_type", "password"));
            UrlEncodedFormEntity requestEntity = new UrlEncodedFormEntity(form, Consts.UTF_8);

            httpPost.setEntity(requestEntity);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Accept", "application/json");
            
            // Make the call to the REST source to retrieve data and convert the 
            // response from an HttpEntity object into a Java string so more response
            // parsing can be done.
            response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();
            
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == 401) {
                throw new BridgeError("401: Unauthorized. Invalid credentials "
                    + "provided while trying to obtain an authorization token.");
            } else if (responseCode  >= 400) {
                handleFailedRequest(responseCode);
            }

            JSONObject responseObj = parseResponse(EntityUtils.toString(entity));
            token = (String) responseObj.get("access_token");
        } catch (IOException e) {
            throw new BridgeError("Unable to make a connection to the REST"
                + " Service", e);
        }
    }
    
    private void handleFailedRequest (int responseCode) throws BridgeError {
        switch (responseCode) {
            case 400:
                throw new BridgeError("400: Bad Reqeust");
            case 401:
                throw new BridgeError("401: Unauthorized");
            case 404:
                throw new BridgeError("404: Page not found");
            case 405:
                throw new BridgeError("405: Method Not Allowed");
            case 500:
                throw new BridgeError("500 Internal Server Error");
            default:
                throw new BridgeError("Unexpected response from server");
        }
    }
        
    private JSONObject parseResponse(String output) throws BridgeError{
        
        JSONObject responseObj = new JSONObject();
        try {
            responseObj = (JSONObject)JSONValue.parseWithException(output);
            // A message in the response means that the request failed with a 400
            if(responseObj.containsKey("message")) {
                throw new BridgeError(String.format("The server responded with: "
                    + "\"%s\"", responseObj.get("message")));
            }
        } catch (ParseException e){
            // Assume all 200 responses will be JSON format.
            LOGGER.error("There was a parse exception with the response", e);
        } catch (BridgeError e) {
            throw e;
        } catch (Exception e) {
            throw new BridgeError("An unexpected error has occured ", e);
        }
        
        return responseObj;
    }
}
