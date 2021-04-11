package com.example.smartvendingmachine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

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
import java.io.InputStreamReader;
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

    //네이버 로그인
    private FloatingActionButton mButtonNaver;
    private static OAuthLogin mOAuthLoginModule;
    private static Context mContext;

    //구글 로그인
    private FloatingActionButton mButtonFacebook;

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
        updateKakaoLoginUi();
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
                            //AccessToken으로 회원정보 가져오기
                            String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                            NaverTask task = new NaverTask();
                            task.execute(accessToken);
                        }
                        else {
                            String errorCode = mOAuthLoginModule
                                    .getLastErrorCode(mContext).getCode();
                            String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                            Toast.makeText(mContext, "errorCode:" + errorCode
                                    + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
                        }
                    };
                };
                mOAuthLoginModule.startOauthLoginActivity(LoginActivity.this, mOAuthLoginHandler);
                finish();
                Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                startActivity(intent);
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
        mButtonFacebook = (FloatingActionButton) findViewById(R.id.mButtonFacebook);
        mButtonFacebook.setOnClickListener(this);


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
    //네이버 유저정보 가져오기
    class NaverTask extends AsyncTask<String, Void, String> {
        String result;

        @Override
        protected String doInBackground(String... strings) {
            String token = strings[0];// 네이버 로그인 접근 토큰;
            String header = "Bearer " + token; // Bearer 다음에 공백 추가
            try {
                String apiURL = "https://openapi.naver.com/v1/nid/me";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
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

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
                            Toast.makeText(getApplicationContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                            Log.i("아이디 확인", account.getId()); //기본키
                            Log.i("이메일 확인", account.getEmail());
                            Log.i("아이디 토큰 확인", account.getIdToken());
                            Log.i("닉네임 확인", account.getGivenName()); //닉네임
                        } else{ //로그인이 실패했으면...
                            Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}