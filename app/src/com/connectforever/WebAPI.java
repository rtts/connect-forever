package com.connectforever;

import android.location.Location;
import android.util.Log;
import android.net.ConnectivityManager;
import android.content.Context;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.net.ssl.SSLException;
import com.google.android.c2dm.C2DMessaging;

/**
 * Handles all communication with the webAPI.
 * Methods of this class throw a custom exception when an error occurs.
 * This exception contains a message that can be presented to the user. 
 */
public class WebAPI {
  public static final String TAG = "WebAPI";
  
  private static final String WEBSITE = 
      "https://connect-forever.appspot.com/";

  private static final String REGISTER = "register/";
  private static final String LOCATION = "location/";
  private static final String PROPOSAL = "proposal/";
  private static final String RESET    = "reset/";

  private static ConnectivityManager connectivityManager;
  private static Context context;

  public static void registerOrUpdateID() throws WebApiException {
    UUID uuid;
    String uuidString = SharedPrefs.getInstance().getUUID();
    if ("".equals(uuidString)) {
      uuid = UUID.randomUUID();
    }
    else {
      uuid = UUID.fromString(uuidString);
    }
    requestRegister(uuid);
    // registration or update succeeded
    SharedPrefs.getInstance().setShouldSendId(false);
    if ("".equals(uuidString)) {
      // Save uuid
      SharedPrefs.getInstance().setUUID(uuid);
    }
  }
  
  /**
   * Gets the current proposal key. If it was not yet requested,
   * the web API will generate one.
   */
  public static String getProposal() throws WebApiException {
    String proposalKey = SharedPrefs.getInstance().getProposalCode();
    if ("".equals(proposalKey)) {
      proposalKey = requestGetProposal();
      SharedPrefs.getInstance().setProposalCode(proposalKey);
    }
    return proposalKey;
  }
  
  /**
   * Posts the key to the web API, which leads to the devices' connection.
   */
  public static boolean setProposal(String key) throws WebApiException {
    if (requestSetProposal(key)) {
      SharedPrefs.getInstance().setConnected(true);
      return true;
    }
    return false;
  }

  /**
   * Deletes the current proposal key.
   */
  public static boolean deleteProposal() throws WebApiException {
    if (requestDeleteProposal()) {
      SharedPrefs.getInstance().setProposalCode("");
      return true;
    }
    return false;
  }
  
  /**
   * Gets the most recent location from the other device.
   */
  public static void getLocation() throws WebApiException {
    requestGetLocation();
  }
  
  /**
   * Pushes your location to the web API.
   */
  public static void setLocation(Location location) throws WebApiException {
    requestSetLocation(location);
  }
  
  public static void deleteLocation() throws WebApiException {
    requestDeleteLocation();
  }
  
  /**
   * Resets the app.
   */
  public static boolean reset() throws WebApiException {
    if (requestReset()) {
      SharedPrefs.getInstance().setConnected(false);
      return true;
    }
    else return false;
  }
  
  
  

  private static URI getURI(String key) throws WebApiException {
    try {
      String id = C2DMessaging.getRegistrationId(context);
      if (id == null || "".equals(id)) {
        throw new WebApiException(R.string.error_registration_failed);
      }
      return new URI(WEBSITE + key + SharedPrefs.getInstance().getUUID());
    }
    catch (URISyntaxException e) {
      throw new WebApiException(WebApiException.REASON_UNKNOWN);
    }
  }
  
  private static void requestRegister(UUID uuid) throws WebApiException {
    URI uri;
    try {
      uri = new URI(WEBSITE + REGISTER + uuid.toString());
      Log.d(TAG, "Request from: " + uri.toString());
      HttpPost request = new HttpPost(uri);
      List<NameValuePair> pairs = new ArrayList<NameValuePair>();
      pairs.add(new BasicNameValuePair("c2dm_id", C2DMessaging.getRegistrationId(context)));
      request.setEntity(new UrlEncodedFormEntity(pairs));
      execute(request);
    }
    catch (URISyntaxException e1) {
      throw new WebApiException(WebApiException.REASON_UNKNOWN);
    }
    catch (UnsupportedEncodingException e) {
      throw new WebApiException(WebApiException.REASON_UNKNOWN);
    }
  }
  
  private static String requestGetProposal() throws WebApiException {
    URI uri = getURI(PROPOSAL);
    HttpGet request = new HttpGet(uri);
    String response = execute(request);
    Log.d(TAG, response);
    return response;
  }
  
  private static boolean requestSetProposal(String key) throws WebApiException {
    URI uri = getURI(PROPOSAL);
    HttpPost request = new HttpPost(uri);
    List<NameValuePair> pairs = new ArrayList<NameValuePair>();
    pairs.add(new BasicNameValuePair("key", key));
    try {
      request.setEntity(new UrlEncodedFormEntity(pairs));
    }
    catch (UnsupportedEncodingException e) {
      throw new WebApiException(WebApiException.REASON_UNKNOWN);
    }
    String response = execute(request);
    Log.d(TAG, "push proposal response: " + response);
    return "Congratulations!".equals(response);
  }
  
  private static boolean requestDeleteProposal() throws WebApiException {
    URI uri = getURI(PROPOSAL);
    HttpDelete request = new HttpDelete(uri);
    return "Proposal withdrawn".equals(execute(request));
  }
  
  /**
   * @return if you are connected
   */
  private static void requestGetLocation() throws WebApiException {
    URI uri = getURI(LOCATION);
    HttpGet request = new HttpGet(uri);
    execute(request);
    Log.d(TAG, "Sent location request, now waiting for push with location");
  }
  
  /* Always send a message to let server know we exist! */
  /* Returns the number of seconds since other last wanted to know your location */
  private static void requestSetLocation(Location location) throws WebApiException {
    URI uri = getURI(LOCATION);
    HttpPut request = new HttpPut(uri);
    String loc = location == null ? "Unknown" 
        : "" + location.getLatitude() + ":" + location.getLongitude();
    Log.d(TAG, "Sending location: " + loc);
    try {
      request.setEntity(new StringEntity(loc));
    }
    catch (UnsupportedEncodingException e) {
      throw new WebApiException(WebApiException.REASON_UNKNOWN);
    }
    try {
      execute(request);
    }
    catch (WebApiException e) {
    }
  }
  
  private static void requestDeleteLocation() throws WebApiException {
    URI uri = getURI(LOCATION);
    HttpDelete request = new HttpDelete(uri);
    execute(request);
  }
  
  private static boolean requestReset() throws WebApiException {
    URI uri = getURI(RESET);
    HttpGet request = new HttpGet(uri);
    String response = execute(request);
    return "OK".equals(response);
  }
  
  private static String execute(HttpUriRequest request) throws WebApiException {
    if (connectivityManager.getActiveNetworkInfo() == null ||
        ! connectivityManager.getActiveNetworkInfo().isConnected()) {
      throw new WebApiException(WebApiException.REASON_NO_INTERNET);
    }
    // BEGIN WORKER THREAD, ON MAIN THREAD SHOW PROGRESSDIALOG
    HttpResponse response;
    try {
      HttpClient client = new DefaultHttpClient();
      response = client.execute(request);
    }
    catch (SSLException e) {
     throw new WebApiException(WebApiException.REASON_SSL);
    }
    catch (ClientProtocolException e) {
      throw new WebApiException(WebApiException.REASON_INVALID_RESPONSE);
    }
    catch (IOException e) {
      throw new WebApiException(WebApiException.REASON_HTTP);
    }
    int responseCode = response.getStatusLine().getStatusCode();
    if (responseCode == 405) {
      throw new WebApiException(WebApiException.REASON_NOT_CONNECTED);
    }
    else if (responseCode != 200) {
      throw new WebApiException(WebApiException.REASON_UNKNOWN);
    }
    // Careful that no fallthroughs occur, this would lead to nullpointerexception
    try {
      HttpEntity entity = response.getEntity();
      String responseBody = EntityUtils.toString(entity);
      return responseBody;
    }
    catch (IllegalArgumentException e) {
      throw new WebApiException(WebApiException.REASON_INVALID_RESPONSE);
    }
    catch (IOException e) {
      throw new WebApiException(WebApiException.REASON_INVALID_RESPONSE);
    }
  }
  
  public static void setConnectivityManager(ConnectivityManager manager) {
    connectivityManager = manager;
  }
  
  public static void setContext(Context c) {
    context = c;
  }
}
