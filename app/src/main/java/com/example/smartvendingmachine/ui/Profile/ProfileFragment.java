package com.example.smartvendingmachine.ui.Profile;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.smartvendingmachine.R;
import com.example.smartvendingmachine.ui.board.BoardAdapter;
import com.example.smartvendingmachine.ui.board.BoardData;
import com.example.smartvendingmachine.ui.board.BoardFragment;
import com.example.smartvendingmachine.ui.board.BoardMainFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private SharedPreferences appData;

    private String sharedNickname;      //App 사용자 닉네임
    private String user_id;             //App 사용자 ID
    private String getcl;               //포털 로그인종 확인

    private int BoardCount = 0;         //내가 쓴 게시글 개수
    private TextView txt_profile_name, txt_profile_board_number;
    private ImageView edit_nickname;

    private RecyclerView recyclerView;
    private BoardAdapter adapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<BoardData> mSearchData = new ArrayList<>();

    private static String IP_ADDRESS = "211.211.158.42/yongrun/svm";
    private static String TAG = "프로필 게시물";
    private String mJsonString;

    private Button btnlogout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_profile, container, false);
        appData = getActivity().getSharedPreferences("appData", MODE_PRIVATE);

        getSharedLoad();    // App 사용자 ID, App 사용자 닉네임 가져오기
        UserUpdate();       // 유저 정보 새로고침.

        BoardCount = 0;     // 게시글 개수 초기값 0

        txt_profile_name = rootView.findViewById(R.id.txt_profile_name);    // 닉네임
        txt_profile_board_number = rootView.findViewById(R.id.txt_profile_board_number);    //게시글 수

        edit_nickname = rootView.findViewById(R.id.edit_nickname);

        edit_nickname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NicknameEditActivity.class);
                intent.putExtra("nickname", txt_profile_name.getText().toString());
                startActivity(intent);
            }
        });


        txt_profile_name.setText(sharedNickname);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.rv_profile);
        recyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.scrollToPosition(0);
        adapter = new BoardAdapter(mSearchData);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        //커스텀 리스터 객체 생성 및 전달.
        adapter.setOnItemClickListener(new BoardAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                // TODO : 아이템 클릭 이벤트를 플레그먼트에서 처뤼
                Bundle arguments = new Bundle();

                arguments.putString("userid", mSearchData.get(position).getUserid());      // 글 작성자 ID
                arguments.putString("post_code", mSearchData.get(position).getCode());      // 게시글 코드

                arguments.putString("nickname", mSearchData.get(position).getNickname());  //작성자 닉네임
                arguments.putString("contents", mSearchData.get(position).getContents());  //작성자 글 내용
                arguments.putString("date", mSearchData.get(position).getDate());          //작성자 글 작성 날짜
                arguments.putString("title", mSearchData.get(position).getTitle());        //작성자 글 제목

                arguments.putString("answer", mSearchData.get(position).getAnswercontents()); // 관리자 답변
                arguments.putString("answerdate", mSearchData.get(position).getAnswerdate()); // 관리자 답변 작성 날짜

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                MyBoardFragment myBoardFragment = new MyBoardFragment();
                myBoardFragment.setArguments(arguments);        //내가 쓴 글 Fragment

                transaction.replace(R.id.nav_host_fragment, myBoardFragment).addToBackStack(null).commit();
            }
        });

        //logout
        btnlogout = rootView.findViewById(R.id.mButtonLogout);
        btnlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getcl.equals("naverLog")) {
                    FirebaseAuth.getInstance().signOut();
                } else if (getcl.equals("googleLog")) {

                } else if(getcl.equals("kakaoLog")){

                }
            }
        });


        return rootView;
    }

    private void getSharedLoad() {
        user_id = appData.getString("ID", "");
        sharedNickname = appData.getString("NICKNAME", "");
        getcl = appData.getString("CURRENT_LOGIN", "");
        Log.i("Id 가져오나 확인", "onCreateView : " + user_id);
        Log.i("Nickname 가져오나 확인", "onCreateView : " + sharedNickname);
        Log.i("로그인 확인", "네이버인가 구글인가 카카오인가 : " + getcl);
    }


    private class GetData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(getActivity(), "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            Log.d(TAG, "response - " + result);

            if (result == null) {

            } else {

                mJsonString = result;
                showResult();
            }
        }


        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];
            String postParameters = params[1];


            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();


            } catch (Exception e) {

                Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();

                return null;
            }

        }
    }

    private void showResult() {

        String TAG_POST = "POST_DATA";
        String TAG_CODE = "POST_CODE";
        String TAG_TITLE = "POST_TITLE";
        String TAG_NICKNAME = "POST_NICKNAME";
        String TAG_DATE = "POST_DATE";
        String TAG_MANAGER_COMMENT = "POST_MANAGER_COMMENT";
        String TAG_CONTENTS = "POST_CONTENTS";
        String TAG_ANSWER_CONTENTS = "POST_ANSWER_CONTENTS";
        String TAG_ANSWER_DATE = "POST_ANSWER_DATE";
        String TAG_POST_ID = "POST_ID";


        try {
            JSONObject jsonObject = new JSONObject(mJsonString);

            JSONArray MyPost = jsonObject.getJSONArray(TAG_POST);

            for (int i = MyPost.length() - 1; i >= 0; i--) {

                JSONObject item = MyPost.getJSONObject(i);

                String POST_ID = item.getString(TAG_POST_ID);

                if (POST_ID.equals(user_id)) {            // 내가 쓴 글 가져오기 위해, 게시글 작성자 ID와 App사용자 ID를 비교.
                    BoardCount++;                           // 게시글 한 개 씩 가져오면서 게시글 개수 1 증가.
                    String POST_CODE = item.getString(TAG_CODE);
                    String POST_TITLE = item.getString(TAG_TITLE);
                    String POST_NICKNAME = item.getString(TAG_NICKNAME);
                    String POST_DATE = item.getString(TAG_DATE);
                    String POST_MANAGER_COMMENT = item.getString(TAG_MANAGER_COMMENT);
                    String POST_CONTENTS = item.getString(TAG_CONTENTS);
                    String POST_ANSWER_CONTENTS = item.getString(TAG_ANSWER_CONTENTS);
                    String POST_ANSWER_DATE = item.getString(TAG_ANSWER_DATE);

                    BoardData boardData = new BoardData();

                    if (POST_MANAGER_COMMENT.equals("0")) {
                        POST_MANAGER_COMMENT = "확인안함";
                    } else {
                        POST_MANAGER_COMMENT = "확인됨";
                    }

                    boardData.setCode(POST_CODE); // 게시글 코드
                    boardData.setTitle(POST_TITLE); // 게시글 제목
                    boardData.setNickname(POST_NICKNAME); // 게시글 작성자
                    boardData.setDate(POST_DATE); // 게시글 작성 날짜
                    boardData.setContents(POST_CONTENTS); // 게시글 내용
                    boardData.setUserid(POST_ID);

                    boardData.setManagercomment(POST_MANAGER_COMMENT); // 건의 확인 여부
                    boardData.setAnswercontents(POST_ANSWER_CONTENTS); // 답변 내용
                    boardData.setAnswerdate(POST_ANSWER_DATE); // 답변 작성 날짜

                    mSearchData.add(boardData);

                    adapter.notifyDataSetChanged();

                }
            }
            txt_profile_board_number.setText(String.valueOf(BoardCount));


        } catch (JSONException e) {

            Log.d(TAG, "showResult : ", e);
        }
    }


    private class UserData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(getActivity(), "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            Log.d(TAG, "response - " + result);

            if (result == null) {

            } else {

                mJsonString = result;
                showUser();                 //ShowUser() 메소드
            }
        }


        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];
            String postParameters = params[1];


            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();


            } catch (Exception e) {

                Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();

                return null;
            }

        }
    }

    private void showUser() {
        //유저 닉네임 정보 보여주기 위함
        String TAG_USER = "USER_DATA";
        String TAG_USER_NICKNAME = "USER_NICKNAME";
        String TAG_USER_ID = "USER_ID";

        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray User = jsonObject.getJSONArray(TAG_USER);

            for (int i = 0; i < User.length(); i++) {

                JSONObject user = User.getJSONObject(i);

                String USER_ID = user.getString(TAG_USER_ID);


                if (USER_ID.equals(user_id)) {
                    String USER_NICKNAME = user.getString(TAG_USER_NICKNAME);
                    save(USER_NICKNAME);
                    getSharedLoad();
                    txt_profile_name.setText(sharedNickname);
                }
            }


        } catch (JSONException e) {

            Log.d(TAG, "showResult : ", e);
        }
    }

    // 설정값을 저장하는 함수 ( 닉네임 )
    private void save(String nickname) {
        // SharedPreferences 객체만으론 저장 불가능 Editor 사용
        SharedPreferences.Editor editor = appData.edit();

        editor.putString("NICKNAME", nickname);

        // apply, commit 을 안하면 변경된 내용이 저장되지 않음
        editor.apply();
    }

    public void UserUpdate() {
        UserData task = new UserData();
        task.execute("http://" + IP_ADDRESS + "/USER.php", "");

    }

    public void PostUpdate() {
        BoardCount = 0;
        mSearchData.clear();
        GetData task = new GetData();
        task.execute("http://" + IP_ADDRESS + "/POST.php", "");
    }

    @Override
    public void onResume() {
        super.onResume();
        getSharedLoad();        // Shared값 변경되었을 수 있으니 최신화하기
        UserUpdate();           // User 정보 최신화
        PostUpdate();           // 내 게시글 정보 최신화
    }

}