package go.glass.realwear.hyundai.test.socket;

import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import go.glass.realwear.hyundai.test.DeepFineApplication;
import go.glass.realwear.hyundai.test.video.VideoConferenceUser;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

public class SocketService {
    protected Socket mSocket;
    protected static SocketService INSTANCE;
    protected SocketServiceInterface mCallback;

    public static synchronized SocketService getInstance() {
        if ( null == INSTANCE ) {
            INSTANCE = new SocketService();
        }

        return INSTANCE;
    }

    @SuppressLint("TrustAllX509TrustManager") final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
        public void checkClientTrusted(X509Certificate[] chain, String authType) { }
        public void checkServerTrusted(X509Certificate[] chain, String authType) { }
    }};

    public Emitter.Listener onConnect = (args) -> {
        mCallback.onConnect();
    };

    public Emitter.Listener onConnectError = (args) -> {
        mCallback.onConnectError(args[0].toString());
    };

    public Emitter.Listener onDisConnect = (args) -> {
        mCallback.onDisConnect(args[0].toString());
    };

    public Emitter.Listener user_joined = (args) -> {
        ArrayList<VideoConferenceUser> users = new ArrayList<>();

        try {
            JSONArray userArrs = new JSONArray(args[2].toString());

            for ( int i = 0; i < userArrs.length(); i++ ) {
                VideoConferenceUser data = new VideoConferenceUser();
                data.setSocketId(userArrs.get(i).toString());
                users.add(data);
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        finally {
            mCallback.user_joined(args[0].toString(), Integer.parseInt(args[1].toString()), users);
        }
    };

    public Emitter.Listener user_left = (args) -> {
        mCallback.user_left(args[0].toString());
    };

    public Emitter.Listener gotMessageFromServer = (args) -> {
        try {
            mCallback.gotMessageFromServer(args[0].toString(), new JSONObject(args[1].toString()));
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    };

    /**
     * 소켓 연결
     * @param poCallback
     */
    public void init(SocketServiceInterface poCallback) {
        this.mCallback = poCallback;
        close();

        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, trustAllCerts, null);

            IO.setDefaultHostnameVerifier((hostname, session) -> true);
            IO.setDefaultSSLContext(sslcontext);

            IO.Options opt = new IO.Options();
            opt.sslContext = sslcontext;
            opt.transports = new String[]{WebSocket.NAME};
            opt.reconnection = false;
            opt.reconnectionDelay = 0;

            mSocket = IO.socket(DeepFineApplication.SOCKET_URL, opt);
            mSocket.on(SocketConstants.CONNECT, onConnect);
            mSocket.on(SocketConstants.DISCONNECT, onDisConnect);
            mSocket.on(SocketConstants.CONNECT_ERROR, onConnectError);
            mSocket.on(SocketConstants.USER_JOINED, user_joined);
            mSocket.on(SocketConstants.SIGNAL, gotMessageFromServer);
            mSocket.on(SocketConstants.USER_LEFT, user_left);
            mSocket.connect();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            mSocket.close();
            mSocket = null;
        }
    }

    /**
     * 소켓 닫기
     */
    public void close() {
        if ( null != mSocket ) {
            if (mSocket.connected()) {
                mSocket.off(SocketConstants.CONNECT, onConnect);
                mSocket.off(SocketConstants.DISCONNECT, onDisConnect);
                mSocket.off(SocketConstants.CONNECT_ERROR, onConnectError);
                mSocket.off(SocketConstants.USER_JOINED, user_joined);
                mSocket.off(SocketConstants.SIGNAL, gotMessageFromServer);
                mSocket.off(SocketConstants.USER_LEFT, user_left);
                mSocket.disconnect();
            }

            mSocket.close();
            mSocket = null;
        }
    }

    /**
     * 소켓 연결상태 확인
     * @return
     */
    public boolean getSocketStatus() {
        return (null != mSocket && mSocket.connected());
    }

    /**
     * 소켓 객체 정보
     * @return
     */
    public Socket getSocket() {
        return mSocket;
    }

    /**
     * 소켓 아이디
     * @return
     */
    public String getSocketId() {
        if (null != mSocket) {
            return mSocket.id();
        }
        return null;
    }
}
