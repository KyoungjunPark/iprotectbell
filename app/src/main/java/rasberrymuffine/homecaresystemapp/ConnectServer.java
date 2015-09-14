package rasberrymuffine.homecaresystemapp;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by 경준 on 2015-09-13.
 */
public class ConnectServer {
    private static CommunicationTask task;
    private static String userID;
    private static String userPW;
    private static String resultCode;
    private static ArrayList<ArrayList<String>> logList;

    ConnectServer(){
       task = new CommunicationTask();
    }

    public static void Send_Login_Info(String id, String password){
        new CommunicationTask().execute("sendLoginInfo", id, password);
    }

    // 함수 자체가 달라질 수 있음
    public static boolean getPermission(){

        return true;
    }

    public static ArrayList<ArrayList<String>> Get_Log() {
        new CommunicationTask().execute("log");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return logList;
    }

    private static class CommunicationTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            if(params[0].equals("log")) {
                try {


                    URL url = new URL("http://165.194.104.19:5000/log");
                    /* BufferedReader rd = new BufferedReader(new InputStreamReader(url.openStream()));

                    String line;
                    String log = null;
                    while ((line = rd.readLine()) != null) {
                        log+=line;
                        Log.d("hello", line);
                    }
                    */

                    // logExam 대신 서버에서 받아온 정보 써야함.....
                    String logExam = "[{\"date\":\"2015-09-17 18:26\",\"information\":\"신고\",\"importance\":\"MAJOR\"}," +
                            "{\"date\":\"2015-09-17 18:26\",\"information\":\"신고\",\"importance\":\"MAJOR\"}," +
                            "{\"date\":\"2015-09-17 18:26\",\"information\":\"종료\",\"importance\":\"MAJOR\"}," +
                            "{\"date\":\"2015-09-17 18:26\",\"information\":\"종료\",\"importance\":\"MINOR\"}," +
                            "{\"date\":\"2015-09-17 18:26\",\"information\":\"신고\",\"importance\":\"MINOR\"}]";
                    logList =jsonParse(logExam);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (params[0] == "sendLoginInfo") {

                    URL obj = null;
                    try {
                        obj = new URL("http://165.194.104.19:5000/login");
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        con.setRequestProperty("Accept-Language", "ko-kr,ko;q=0.8,en-us;q=0.5,en;q=0.3");
                        con.setDoOutput(true);
                        String parameter = URLEncoder.encode("user_id", "UTF-8") + "=" + URLEncoder.encode(params[1], "UTF-8");

                        parameter += "&" + URLEncoder.encode("user_password", "UTF-8") + "=" + URLEncoder.encode(params[2], "UTF-8");

                        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                        wr.write(parameter);
                        wr.flush();
                        BufferedReader rd = null;
                        if (con.getResponseCode() == 200) {
                            //로그인 성공
                        } else {
                            //로그인 실패
                            //rd.readLine() means fail reason
                            //so you must toast this message to user
                            rd = new BufferedReader(new InputStreamReader(con.getErrorStream(), "UTF-8"));
                            Log.d("server", String.valueOf(rd.readLine()));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            return true;
        }
    }

    // 마음의 여유가 생기면 Tuple로 만들게여.........
    private static ArrayList<ArrayList<String>> jsonParse(String log){
        ArrayList<ArrayList<String>> logList = new ArrayList<ArrayList<String>>();

        ArrayList<String> jsonKey = new ArrayList<String>();
        jsonKey.add("date");
        jsonKey.add("information");
        jsonKey.add("importance");

        ArrayList<String> oneLog;

        try {
            JSONArray jsonArray = new JSONArray(log);
            org.json.JSONObject json = null;

            for(int i=0; i<jsonArray.length(); i++){
                json = jsonArray.getJSONObject(i);
                Log.d("-------", json.toString());
                if(json!=null){
                    oneLog = new ArrayList<String>();
                    for(int j=0; j<jsonKey.size();j++){

                        Log.d("key",jsonKey.get(j));
                        Log.d("result",json.getString(jsonKey.get(j)));
                        oneLog.add(json.getString(jsonKey.get(j)));
                    }
                    logList.add(oneLog);
                }
            }

            for(int i=0; i<logList.size(); i++){
                for(int j=0; j<logList.get(0).size(); j++)
                    Log.d("result /// ", jsonKey.get(j) + " - " + logList.get(i).get(j));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return logList;
    }
}
