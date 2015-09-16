package rasberrymuffine.homecaresystemapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    /**
     * Created by 예림 on 2015-09-09.
     */
    public static final int REQUEST_CODE_LOG = 1001;
    public static final int REQUEST_CODE_SPEAK = 1002;
    public static final int REQUEST_CODE_SETTING = 1003;
    public static final int REQUEST_CODE_FULLSCREEN = 1004;
    public static final int RESULT_CODE1 = 1;
    public static final int RESULT_CODE2 = 2;
    public static final int VIDEO_PERMITTED = 200;
    public static final int VIDEO_DENIED = 404;
    private static String VIDEO_FOCUS;

    private static final int OK_CODE = 200;
    private static final int ERROR_CODE = 404;


    String isVideoPermitted;

    WebView videoView;
    Button fullScreenButton;
    Button callButton;
    Button speakButton;
    Button logButton;
    Switch doorControlSwitch;

    private String isOK;                    // 서버가 주는 코드( 200 / 404 )를 저장함
    private String action;                  // log 버튼이 눌리면 log를, call이 눌리면 call을 저장한다.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent fromLoginIntent = getIntent();

        videoView = (WebView)findViewById(R.id.videoView);

        //videoView.getSettings().setJavaScriptEnabled(true);
        //videoView.loadUrl("http://165.194.104.19:8080/stream");

        fullScreenButton = (Button)findViewById(R.id.fullScreenButton);
        fullScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullScreen();
            }
        });
        callButton = (Button) findViewById(R.id.callButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = "call";
                sendLogToServer("call", "call call call call", "MAJOR");
                call();
            }
        });
        speakButton = (Button) findViewById(R.id.speakButton);
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = "speak";
                sendLogToServer("speak", "speak", "MINOR");
                //  speak();
            }
        });
        logButton = (Button) findViewById(R.id.logButton);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = "log";
                sendLogToServer("log", "read log", "MINOR");
                // showLogView();
            }
        });
        doorControlSwitch = (Switch) findViewById(R.id.openSwitch);
        doorControlSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (doorControlSwitch.isChecked() == false) {
                        AlertDialog dialog = createDialogBox();
                        dialog.show();
                    }
                }
                return false;
            }
        });

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        VIDEO_FOCUS = "videoView";
        int width = videoView.getWidth();
        int height = videoView.getHeight();
        sendVideoInfoToServer(width, height);

    }

    private void showFullScreen() {
        DisplayMetrics displayMatrics = new DisplayMetrics();

        int height = getWindowManager().getDefaultDisplay().getHeight();
        int width = getWindowManager().getDefaultDisplay().getWidth();
        sendVideoInfoToServer(width, height);

    }

    private void call() {

        String num = "01093866983";                     // 사용자가 등록한 긴급전화번호를 사용해도 좋을듯
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);     // ACTION_DIAL 쓰면 바로 안걸리고 다이얼창만 나타남
            callIntent.setData(Uri.parse("tel:" + num));
            startActivity(callIntent);
        } catch (ActivityNotFoundException e) {
            Log.e("전화를 겁니다.", "전화를 걸 수 없습니다.", e);
        }
    }

    private AlertDialog createDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Warning");
        builder.setMessage("도어락을 해제하시겠습니까?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                action = "open";
                sendLogToServer("open", "do you want to build a snowman?", "MAJOR");
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                doorControlSwitch.toggle();
            }
        });

        AlertDialog dialog = builder.create();
        return dialog;

    }

    private void showLogView() {

        Intent intent = new Intent(getApplicationContext(), LogActivity.class);
        startActivityForResult(intent, REQUEST_CODE_LOG);
    }

    private void speak() {

        Intent intent = new Intent(getApplicationContext(), SpeakActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SPEAK);
    }

    private String getDate() {

        Calendar calendar = Calendar.getInstance();

        String date = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH) + " " +
                calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);

        return date;
    }

    private void sendLogToServer(final String type, final String information, final String importance) {
        ConnectServer.getInstance().setAsncTask(new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {

                URL obj = null;
                try {
                    obj = new URL("http://165.194.104.19:5000/send_log");
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    con = ConnectServer.getInstance().setHeader(con);
                    con.setRequestProperty("Accept-Language", "ko-kr,ko;q=0.8,en-us;q=0.5,en;q=0.3");
                    con.setDoOutput(true);

                    String parameter = URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode(type, "UTF-8");
                    parameter += "&" + URLEncoder.encode("information", "UTF-8") + "=" + URLEncoder.encode(information, "UTF-8");
                    parameter += "&" + URLEncoder.encode("date", "UTF-8") + "=" + URLEncoder.encode(getDate(), "UTF-8");
                    parameter += "&" + URLEncoder.encode("importance", "UTF-8") + "=" + URLEncoder.encode(importance, "UTF-8");

                    OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                    wr.write(parameter);
                    wr.flush();

                    BufferedReader rd = null;

                    if (con.getResponseCode() == OK_CODE) {
                        // OK
                        isOK = OK_CODE + "";
                    } else {
                        // ERROR
                        rd = new BufferedReader(new InputStreamReader(con.getErrorStream(), "UTF-8"));
                        isOK = rd.readLine();
                        Log.d("---- failed ----", String.valueOf(isOK));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if (isOK == OK_CODE + "") {
                    switch (action) {
                        case "speak":
                            speak();
                            break;
                        case "log":
                            showLogView();
                            break;
                        case "open":
                            Toast.makeText(getApplicationContext(), "도어락을 해제합니다.", Toast.LENGTH_LONG).show();
                            break;
                        default:
                            break;
                    }
                } else {
                    if (action != "call") {
                        Toast.makeText(getApplicationContext(), "연결에 실패했습니다.", Toast.LENGTH_LONG).show();
                    }
                    if(action == "open"){
                        Toast.makeText(getApplicationContext(), "도어락을 해제할 수 없습니다.", Toast.LENGTH_LONG).show();
                        doorControlSwitch.toggle();
                    }
                }
            }
        });
        ConnectServer.getInstance().execute();
    }

    private void sendVideoInfoToServer(final int myWidth, final int myHeight) {

        ConnectServer.getInstance().setAsncTask(new AsyncTask<String, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(String... params) {

                URL obj = null;
                try {
                    obj = new URL("http://165.194.104.19:5000/setting_video");
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    //implement below code if token is send to server
                    con = ConnectServer.getInstance().setHeader(con);

                    con.setDoOutput(true);


                    String parameter = URLEncoder.encode("width", "UTF-8") + "=" + URLEncoder.encode(myWidth + "", "UTF-8");
                    parameter += "&" + URLEncoder.encode("height", "UTF-8") + "=" + URLEncoder.encode(myHeight + "", "UTF-8");

                    OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                    wr.write(parameter);
                    wr.flush();

                    BufferedReader rd = null;

                    if (con.getResponseCode() == VIDEO_PERMITTED) {
                        // 비디오 셋팅 성공

                        rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                        String token = rd.readLine();
                        ConnectServer.getInstance().setToken(token);

                        isVideoPermitted = VIDEO_PERMITTED + "";
                        Log.d("---- video success ----", String.valueOf(rd.readLine()));
                    } else {
                        // 비디오 셋팅 실패
                        rd = new BufferedReader(new InputStreamReader(con.getErrorStream(), "UTF-8"));
                        isVideoPermitted = rd.readLine();
                        Log.d("---- video failed ----", String.valueOf(rd.readLine()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if (isVideoPermitted == VIDEO_PERMITTED + "") {
                    if(VIDEO_FOCUS.compareTo("videoView")==0) {
                        loadVideo();
                        VIDEO_FOCUS="fullScreen";
                    }
                    else {
                        VIDEO_FOCUS="videoView";
                        Intent intent = new Intent(getApplicationContext(), FullscreenActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_FULLSCREEN);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "서버로부터 영상을 받아오는데 실패했습니다.", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    private void loadVideo() {
        videoView.getSettings().setJavaScriptEnabled(true);
        videoView.loadUrl("http://165.194.104.19:8080/stream");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            //intent.putExtra("noticeMethod",0);
            startActivityForResult(intent, REQUEST_CODE_SETTING);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestcode, int resultcode, Intent data) {
        super.onActivityResult(requestcode, resultcode, data);

        if (requestcode == REQUEST_CODE_SETTING) {
            if (resultcode == RESULT_CODE1) {

                Toast.makeText(getApplicationContext(), "popup이 선택됨", Toast.LENGTH_LONG).show();
            } else if (resultcode == RESULT_CODE2) {
                Toast.makeText(getApplicationContext(), "execution이 선택됨", Toast.LENGTH_LONG).show();

            } else {

            }
        }
    }

}
