package com.example.smartvendingmachine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.se.omapi.Session;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smartvendingmachine.ui.board.BoardWriteActivity;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.kakao.sdk.auth.LoginClient;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.User;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {


    //카카오 로그인
    private FloatingActionButton mButtonKakao;
    private ViewPager2 mViewPager2;
    private SignInButton btn_google; //구글 로그인 버튼
    private FirebaseAuth auth; //파이어 베이스 인증 객체
    private GoogleApiClient googleApiClient; //구글 API 클라이언트 객체
    private static final int REQ_SIGN_GOOGLE = 100; //구글 로그인 결과 코드

    //서버 IP
    private static String IP_ADDRESS = "59.14.35.61";
    private static String StrUSER_ID, StrUSER_NICKNAME,StrUSER_EMAIL;

    //네이버 로그인
    private FloatingActionButton mButtonNaver;
    public static OAuthLogin mOAuthLoginModule;
    private static Context mContext;

    //구글 로그인
    private FloatingActionButton mButtonGoogle;


    private SharedPreferences appData;


    private boolean saveLoginData;
    private String id;
    private String nickname;
    private String current_login;


    private static final String TAG = "MainActivity";
    Function2<OAuthToken, Throwable, Unit> callback = new Function2<OAuthToken, Throwable, Unit>() { //function2 형태가 로그인
        @Override
        public Unit invoke(OAuthToken oAuthToken, Throwable throwable) { //Token이 null이면 실패
            if (oAuthToken != null) {
                //TBD 로그인이 되었으니 수행할일 여기에 작성
                Log.i("로그인","로그인 되었습니다");
            }
            if (throwable != null) {
                //TBD 실패하면 오류 수행할일 여기에 작성
                Log.i("로그인","로그인에 실패하였습니다");
            }
            updateKakaoLoginUi();
            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mContext = getApplicationContext();
        init();

        appData = getSharedPreferences("appData", MODE_PRIVATE);

        //SharedPreferences 설정값 load ( saveLoginData, id, nickname )
        load();

        // 이전에 로그인 정보를 저장시킨 기록이 있다면
        if (saveLoginData) {
            Intent intent = new Intent(getApplicationContext() , MainActivity.class);
            intent.putExtra("nickname",nickname);
            startActivity(intent);
        }
        else{

        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mButtonKakao:
                Log.i("smartvendingmachine", "카카오 로그인");
                if (LoginClient.getInstance().isKakaoTalkLoginAvailable(LoginActivity.this)) { //카카오 설치되어있는지 확인
                    LoginClient.getInstance().loginWithKakaoTalk(LoginActivity.this, callback);
                } else { //카톡이 설치되어있지 않을시
                    LoginClient.getInstance().loginWithKakaoAccount(LoginActivity.this, callback);
                }
                break;

            case R.id.mButtonNaver:
                Log.i("smartvendingmachine", "네이버 로그인");
                mOAuthLoginModule = OAuthLogin.getInstance();
                mOAuthLoginModule.init(
                        mContext
                        ,getString(R.string.naver_client_id)
                        ,getString(R.string.naver_client_secret)
                        ,getString(R.string.naver_client_name)
                        //,OAUTH_CALLBACK_INTENT
                        // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
                );
                @SuppressLint("HandlerLeak")
                OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
                    @Override
                    public void run(boolean success) {
                        if (success) {
                            String accessToken = mOAuthLoginModule.getAccessToken(mContext);    // accessToken으로 사용자 정보 가져올 수 있음.
                            NaverTask task = new NaverTask();
                            task.execute(accessToken);              // accessToken으로 Logic 실행.
                        }
                        else {
                            String errorCode = mOAuthLoginModule
                                    .getLastErrorCode(mContext).getCode();
                            String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                            Toast.makeText(mContext, "errorCode:" + errorCode
                                    + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();   // 에러문 출력
                        }
                    };
                };
                mOAuthLoginModule.startOauthLoginActivity(LoginActivity.this, mOAuthLoginHandler);
                break;

            case R.id.mButtonFacebook:
                Log.i("smartvendingmachine", "구글 로그인");
                Intent gintent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(gintent, REQ_SIGN_GOOGLE);

            default:
                break;
        }
    }

    private void init() {

        //kakao
        mButtonKakao = (FloatingActionButton) findViewById(R.id.mButtonKakao);
        mButtonKakao.setOnClickListener(this);

        //naver
        mButtonNaver = (FloatingActionButton) findViewById(R.id.mButtonNaver);
        mButtonNaver.setOnClickListener(this);


        //facebook
        mButtonGoogle = (FloatingActionButton) findViewById(R.id.mButtonFacebook);
        mButtonGoogle.setOnClickListener(this);


        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        //프레그먼트 사용하면 this 부분 getcontext 사용하면 됨
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, LoginActivity.this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();

        auth = FirebaseAuth.getInstance(); // 파이어베이스 인증 객체 초기화
    }

    // 설정값을 저장하는 함수 ( 로그인 한적 있는지 , ID , 닉네임 )
    private void save(Boolean flag, String id, String nickname,String login) {
        // SharedPreferences 객체만으론 저장 불가능 Editor 사용
        SharedPreferences.Editor editor = appData.edit();

        // 에디터객체.put타입( 저장시킬 이름, 저장시킬 값 )
        // 저장시킬 이름이 이미 존재하면 덮어씌움
        editor.putBoolean("SAVE_LOGIN_DATA", flag);
        editor.putString("ID", id);
        editor.putString("NICKNAME", nickname);
        editor.putString("CURRENT_LOGIN",login);

        // apply, commit 을 안하면 변경된 내용이 저장되지 않음
        editor.apply();
    }

    // 설정값을 불러오는 함수 ( 로그인 한적 있는지 , ID , 닉네임 )
    private void load() {
        // SharedPreferences 객체.get타입( 저장된 이름, 기본값 )
        // 저장된 이름이 존재하지 않을 시 기본값
        saveLoginData = appData.getBoolean("SAVE_LOGIN_DATA", false);
        id = appData.getString("ID", "");
        nickname = appData.getString("NICKNAME", "");
        current_login = appData.getString("CURRENT_LOGIN","");
    }


    //네이버 로그인 작업
    class NaverTask extends AsyncTask<String, Void, String> {
        String result;

        @Override
        protected String doInBackground(String... strings) {
            String token = strings[0];// 네이버 로그인 접근 토큰;
            String header = "Bearer " + token; // Bearer 다음에 공백 추가
            try {
                String apiURL = "https://openapi.naver.com/v1/nid/me";      // apiurl 주소
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");        // GET메소드 사용
                con.setRequestProperty("Authorization", header);
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {  // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                result = response.toString();
                br.close();
                System.out.println(response.toString());
            } catch (Exception e) {
                System.out.println(e);
            }
            //result 값은 JSONObject 형태로 넘어옵니다.
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                //넘어온 result 값을 JSONObject 로 변환해주고, 값을 가져오면 되는데요.
                // result 를 Log에 찍어보면 어떻게 가져와야할 지 감이 오실거에요.
                JSONObject object = new JSONObject(result);
                if (object.getString("resultcode").equals("00")) {
                    JSONObject jsonObject = new JSONObject(object.getString("response"));
                    Log.d("jsonObject", jsonObject.toString());
                    Log.d("네아로_아이디", jsonObject.getString("id"));
                    Log.d("네아로_이메일", jsonObject.getString("email"));
                    Log.d("네아로_이름", jsonObject.getString("name"));
                    Log.d("네아로_닉네임", jsonObject.getString("nickname"));
                    Log.d("네아로_프로필사진", jsonObject.getString("profile_image"));

                    // accessToken으로 가져온 값 변수에 담기.
                    StrUSER_ID = jsonObject.getString("id");
                    StrUSER_NICKNAME = jsonObject.getString("nickname");
                    StrUSER_EMAIL = jsonObject.getString("email");

                    Log.d("USER_ID", StrUSER_ID);
                    Log.d("USER_NICKNAME", StrUSER_NICKNAME);

                    // 유저 정보 서버에 저장 InsertData
                    InsertData task = new InsertData();
                    task.execute("http://" + IP_ADDRESS + "/yongrun/svm/SIGNUP_ANDRIOD.php", StrUSER_ID, StrUSER_NICKNAME, StrUSER_EMAIL);

                    if ( ! (StrUSER_ID == null || StrUSER_NICKNAME == null || StrUSER_ID=="" || StrUSER_NICKNAME == "") )
                    {
                        save(true,StrUSER_ID,StrUSER_NICKNAME,"NAVER"); // SharedPreferences로 유저 아이디, 유저 닉네임 값 App내에 저장.
                        load();
                        Intent intent = new Intent(LoginActivity.this , MainActivity.class);
                        startActivity(intent);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // 회원 정보 서버 삽입
    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(LoginActivity.this, "Please Wait", null, true, true);
        }


        //정보 삽입 execute
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(TAG, "POST response  - " + result);
        }

        @Override
        protected String doInBackground(String... params) {

            String USER_ID = (String) params[1];
            String USER_NICKNAME = (String) params[2];
            String USER_EMAIL = (String) params[3];
            String serverURL = (String) params[0];

            String postParameters = "&USER_ID=" + USER_ID + "&USER_NICKNAME=" + USER_NICKNAME + "&USER_EMAIL=" + USER_EMAIL;


            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }


                bufferedReader.close();


                return sb.toString();


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }

    //카카오 로그인
    private void updateKakaoLoginUi() {
        UserApiClient.getInstance().me(new Function2<User, Throwable, Unit>() {
            @Override
            public Unit invoke(User user, Throwable throwable) {
                if (user != null) { //로그인이 되었을시

                    Log.i(TAG, "invoke: id=" + user.getId());
                    Log.i(TAG, "invoke: email=" + user.getKakaoAccount().getEmail());
                    Log.i(TAG, "invoke: nickname=" + user.getKakaoAccount().getProfile().getNickname());
                    Log.i(TAG, "invoke: gender=" + user.getKakaoAccount().getGender());
                    Log.i(TAG, "invoke: age=" + user.getKakaoAccount().getAgeRange());
                    Log.i(TAG, "invoke : profile = " +user.getKakaoAccount().getProfile());

                    StrUSER_ID = String.valueOf(user.getId());
                    StrUSER_NICKNAME = user.getKakaoAccount().getProfile().getNickname();
                    StrUSER_EMAIL = user.getKakaoAccount().getEmail();
                    String profileImageUrl = user.getKakaoAccount().getProfile().getProfileImageUrl();

                    //회원 정보 서버 삽입
                    InsertData task = new InsertData();
                    task.execute("http://" + IP_ADDRESS + "/yongrun/svm/SIGNUP_ANDRIOD.php", StrUSER_ID, StrUSER_NICKNAME, StrUSER_EMAIL);

                    if ( ! (StrUSER_ID == null || StrUSER_NICKNAME == null || StrUSER_ID=="" || StrUSER_NICKNAME == "") )
                    {
                        save(true,StrUSER_ID,StrUSER_NICKNAME,"KAKAO"); // SharedPreferences로 유저 아이디, 유저 닉네임 값 App내에 저장.
                        load();
                        Intent intent = new Intent(LoginActivity.this , MainActivity.class);
                        startActivity(intent);
                    }

                } else { //로그아웃 상태

                }
                return null;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // 구글 로그인 인증을 요청 했을 때 결과 값을 되돌려 받는 곳.
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQ_SIGN_GOOGLE){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()){ //인증 결과가 성공
                GoogleSignInAccount account = result.getSignInAccount(); //account 라는 데이터는 구글 로그인 정보를 담고있다. (닉네임, 프로필사진uri, 이메일주소...등)
                resultLogin(account); //로그인 결과 값 출력 수행하라는 메소드
            }
        }
    }
    private void resultLogin(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){ //로그인이 성공했으면...

                            Log.i("아이디 확인", account.getId()); //기본키
                            Log.i("이메일 확인", account.getEmail());
                            Log.i("아이디 토큰 확인", account.getIdToken());
                            Log.i("닉네임 확인", account.getGivenName()); //닉네임


                            //로그인 값 변수에 담기
                            StrUSER_ID = account.getId();
                            StrUSER_NICKNAME = account.getGivenName();
                            StrUSER_EMAIL = account.getEmail();

                            //회원 정보 서버 삽입
                            InsertData insert = new InsertData();
                            insert.execute("http://" + IP_ADDRESS + "/yongrun/svm/SIGNUP_ANDRIOD.php", StrUSER_ID, StrUSER_NICKNAME, StrUSER_EMAIL);

                            if ( ! (StrUSER_ID == null || StrUSER_NICKNAME == null || StrUSER_ID=="" || StrUSER_NICKNAME == "") )
                            {
                                save(true,StrUSER_ID,StrUSER_NICKNAME,"GOOGLE"); // SharedPreferences로 유저 아이디, 유저 닉네임 값 App내에 저장.
                                load();
                                Intent intent = new Intent(LoginActivity.this , MainActivity.class);
                                startActivity(intent);
                            }




                        }
                        else{ //로그인이 실패했으면...
                            Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}