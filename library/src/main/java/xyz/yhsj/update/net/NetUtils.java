package xyz.yhsj.update.net;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import xyz.yhsj.update.listener.NetCallBack;

/**
 * 网络请求基类
 * Created by LOVE on 2016/8/31 031.
 * 2017-01-03王宽，支持https。
 */

public class NetUtils {
    //url
    private String url;
    private HttpMetHod method;
    private Map<String, String> params;
    private NetCallBack callBack;


    public NetUtils(String url, HttpMetHod method, Map<String, String> params, NetCallBack callBack) {
        this.url = url;
        this.method = method;
        this.params = params;
        this.callBack = callBack;
        if (params == null) {
            this.params = new HashMap<>();
        }
        request();
    }


    /**
     * 请求网络
     */
    private void request() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                StringBuffer paramsStr = new StringBuffer();
                Set<String> set = params.keySet();
                for (String s : set) {
                    paramsStr.append(s).append("=").append(params.get(s)).append("&");
                }

                if (url.startsWith("https://")) {
                    // Create a trust manager that does not validate certificate chains
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }};
                    // Install the all-trusting trust manager
                    try {// 注意这部分一定要
                        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession sslSession) {
                                Log.i("NetUtils", "Approving certificate for " + hostname);
                                return true;
                            }
                        });
                        SSLContext sc = SSLContext.getInstance("TLS");
                        sc.init(null, trustAllCerts, new SecureRandom());
                        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                        URL httpUrl = new URL(url);
                        URLConnection urlConnection = null;
                        switch (method) {
                            case POST:
                                urlConnection = httpUrl.openConnection();
                                urlConnection.setDoOutput(true);
                                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "utf-8"));
                                bw.write(paramsStr.toString());
                                bw.flush();
                                break;
                            default:
                                urlConnection = new URL(url + "?" + paramsStr.toString()).openConnection();
                                break;
                        }
                        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                        String line = null;
                        StringBuffer result = new StringBuffer();
                        while ((line = br.readLine()) != null) {
                            result.append(line);
                        }
                        return result.toString();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        URLConnection uc;
                        switch (method) {
                            case POST:
                                uc = new URL(url).openConnection();
                                uc.setDoOutput(true);
                                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(uc.getOutputStream(), "utf-8"));
                                bw.write(paramsStr.toString());
                                bw.flush();
                                break;
                            default:
                                uc = new URL(url + "?" + paramsStr.toString()).openConnection();
                                break;
                        }
                        BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
                        String line = null;
                        StringBuffer result = new StringBuffer();
                        while ((line = br.readLine()) != null) {
                            result.append(line);
                        }
                        return result.toString();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (callBack != null) {
                    if (s != null) {
                        callBack.onSuccess(s);
                    } else {
                        callBack.onFail();
                    }
                }
                super.onPostExecute(s);
            }
        }.execute();
    }
}
