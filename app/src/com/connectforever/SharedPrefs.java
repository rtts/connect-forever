package com.connectforever;

import java.util.UUID;
import android.content.SharedPreferences;

public class SharedPrefs {
  static SharedPrefs mSharedPrefs;
  SharedPreferences sharedPreferences;

  private static final String KEY_SEND_ID = "should_send_id";
  private static final String KEY_UUID = "uuid";
  private static final String KEY_PROPOSAL = "proposalkey";
  private static final String KEY_ENTERED_PROPOSAL = "enteredkey";
  private static final String KEY_CONNECTED = "isconnected";
  private static final String KEY_FRESH_CONNECT = "fresh_connect";
  private static final String KEY_ARROW = "arrow";
  private static final String KEY_FIRST_RUN = "firstrun";
  private SharedPrefs() { }
  
  public static SharedPrefs getInstance() {
    if (mSharedPrefs == null) {
      mSharedPrefs = new SharedPrefs();
    }
    return mSharedPrefs;
  }
  
  public void setSharedPreferences(SharedPreferences sharedPrefs) {
    this.sharedPreferences = sharedPrefs;
  }
  
  /** Sets state of the app where you still have to send c2dm id to web api. */
  public void setShouldSendId(boolean shouldSendId) {
    sharedPreferences.edit()
                     .putBoolean(KEY_SEND_ID, shouldSendId)
                     .commit();
  }
  
  /** Should you send c2dm id to web api? */
  public boolean shouldSendId() {
    return sharedPreferences.getBoolean(KEY_SEND_ID, false);
  }
  
  /** Persistently stores the uuid. */
  public void setUUID(UUID uuid) {
    sharedPreferences.edit()
                     .putString(KEY_UUID, uuid.toString())
                     .commit();
  }
  
  /** Returns the stored uuid. */
  public String getUUID() {
    return sharedPreferences.getString(KEY_UUID, "");
  }
  
  /** Persistently stores the proposal code. */
  public void setProposalCode(String code) {
    sharedPreferences.edit()
                     .putString(KEY_PROPOSAL, code)
                     .commit();
  }
  
  /** 
   * Gets the proposal code that was stored locally. Returns an empty string
   * if no proposal code was stored.
   */
  public String getProposalCode() {
    return sharedPreferences.getString(KEY_PROPOSAL, "");
  }
  
  /**
   * Returns true if a proposal code is stored locally.
   */
  public boolean hasProposalCode() {
    return ! "".equals(sharedPreferences.getString(KEY_PROPOSAL, ""));
  }

  /**
   * Sets the locally entered proposal key.
   */
  public void setEnteredKey(String key) {
    sharedPreferences.edit()
                     .putString(KEY_ENTERED_PROPOSAL, key)
                     .commit();
  }

  /**
   * Gets the last-known locally entered proposal key.
   */
  public String getEnteredKey() {
    return sharedPreferences.getString(KEY_ENTERED_PROPOSAL, "");
  }

  /**
   * Returns whether or not there is a locally entered proposal key.
   */
  public boolean hasEnteredKey() {
    return ! "".equals(getEnteredKey());
  }

  /**
   * Sets the connected status of the device. The device state is kept
   * consistent, so no proposal key or entered key are available if the
   * device is connected.
   */
  public void setConnected(boolean connected) {
    if (connected) {
      setProposalCode("");
      setEnteredKey("");
      setIsFreshConnect(true);
    }
    sharedPreferences.edit()
                     .putBoolean(KEY_CONNECTED, connected)
                     .commit();
  } 
  
  /**
   * Is the device connected to another device?
   */
  public boolean isConnected() {
    return sharedPreferences.getBoolean(KEY_CONNECTED, false);
  }

  /** Stores if next program start is first after connected to other. */
  public void setIsFreshConnect(boolean fresh_connect) {
    sharedPreferences.edit()
                     .putBoolean(KEY_FRESH_CONNECT, fresh_connect)
                     .commit();
  }
  
  /** Is this the first program start since connected to other? */
  public boolean isFreshConnect() {
    return sharedPreferences.getBoolean(KEY_FRESH_CONNECT, false);
  }
  
  public void setArrowDrawable(int arrow_id) {
    sharedPreferences.edit()
                     .putInt(KEY_ARROW, arrow_id)
                     .commit();
  }
  
  public int getArrowDrawable() {
    return sharedPreferences.getInt(KEY_ARROW, -1);
  }
  
  public void setFirstRun(boolean firstRun) {
    sharedPreferences.edit()
                     .putBoolean(KEY_FIRST_RUN, firstRun)
                     .commit();
  }
  
  public boolean isFirstRun() {
    return sharedPreferences.getBoolean(KEY_FIRST_RUN, true);
  }
}
