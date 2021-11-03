package go.glass.realwear.hyundai.test.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class NetworkManager {
    public enum NetworkType {
        WIFI, MOBILE, NONE
    }

    public interface NetworkSpeedListner {
        void getSpeed(String psText);
    }

    private final Context mContext;
    private static NetworkManager mNetworkManager;
    private Thread mNetworkThread;
    private NetworkSpeedListner mCallback;

    public NetworkManager(Context poContext) {
        this.mContext = poContext.getApplicationContext();
    }

    public static NetworkManager getInstance(Context poContext) {
        if ( null == mNetworkManager ) {
            mNetworkManager = new NetworkManager(poContext);
        }

        return mNetworkManager;
    }

    public boolean isConnection() {
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    public NetworkType getNetworkType() {
        NetworkType networkType = NetworkType.NONE;

        ConnectivityManager connectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if ( networkInfo != null ) {
            if ( networkInfo.getType() == ConnectivityManager.TYPE_WIFI ) {
                networkType = NetworkType.WIFI;
            }
            else if ( networkInfo.getType() == ConnectivityManager.TYPE_MOBILE ) {
                networkType = NetworkType.MOBILE;
            }
        }
        else {
            networkType = NetworkType.NONE;
        }

        return networkType;
    }

    public void startNetworkCheck(NetworkSpeedListner poCallback) {
        stopNetworkCheck();

        if (!isConnection()) {
            return;
        }

        mCallback = poCallback;

        try {
            final Handler handler = new Handler(msg -> {
                try {
                    WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo info = wifiManager.getConnectionInfo();
                    int rssi = info.getRssi();
                    int level = WifiManager.calculateSignalLevel(rssi, 4);

                    String text0 = "[ " + rssi + " ]";
                    String text1 = " 현재 네트워크 상태 ";
                    String text2 = "";

                    if ( rssi >= -55 ) {
                        text2 = "좋음";
                    }
                    else if ( rssi >= -70 ) {
                        text2 = "양호";
                    }
                    else if ( rssi >= -85 ) {
                        text2 = "안좋음";
                    }
                    else {
                        text2 = "최악";
                    }

                    if ( null != mCallback ) {
                        mCallback.getSpeed(text0 + text1 + text2);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                return true;
            });

            Runnable task = () -> {
                while (null != mNetworkThread && !mNetworkThread.isInterrupted()) {
                    try {
                        Thread.sleep(3500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    handler.sendEmptyMessage(0);
                }
            };
            mNetworkThread = new Thread(task);
            mNetworkThread.start();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void stopNetworkCheck() {
        try {
            mNetworkThread.interrupt();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        finally {
            mNetworkThread = null;
            mCallback = null;
        }
    }
}
