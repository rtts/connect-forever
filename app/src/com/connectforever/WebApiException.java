package com.connectforever;

public class WebApiException extends Exception {

  private static final long serialVersionUID = 8318010771380579790L;
  
  public static final int REASON_NO_INTERNET = 0;
  public static final int REASON_NOT_CONNECTED = 1;
  public static final int REASON_INVALID_RESPONSE = 2;
  public static final int REASON_UNKNOWN = 3;
  public static final int REASON_HTTP = 4;
  public static final int REASON_SSL = 5;
  
  
  
  private int reason;
  
  
  
  public WebApiException(int reason) {
    super();
    this.reason = reason;
  }
  
  public int getReason() {
    return reason;
  }
  
  public int getMessageStringId() {
    switch(reason) {
    case REASON_NOT_CONNECTED:
      return R.string.alert_not_connected;
    case REASON_INVALID_RESPONSE:
      return R.string.error_invalid_response;
    case REASON_UNKNOWN:
      return R.string.error_unknown;
    case REASON_HTTP:
      return R.string.error_general_http;
    case REASON_SSL:
      return R.string.error_ssl_exception;
    }
    return -1;
  }
}
