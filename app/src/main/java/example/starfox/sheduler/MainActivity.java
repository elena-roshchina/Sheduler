package example.starfox.sheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import example.starfox.sheduler.api.IdentificationModel;
import example.starfox.sheduler.api.ScheduleModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String SHARED_PREF = "MY_SHARED_PREF";
    private static final String SHARED_PREF_LOG = "USER_LOGIN";
    private static final String SHARED_PREF_PASS = "USER_PASS";
    private static final String SHARED_PREF_SESSION = "SESSION";
    private static final String SHARED_SCHEDULE = "SCHEDULE";
    private static final String SHARED_LAST_UPDATE = "LAST_UPDATE";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private TextView debugMessage;

    private String strLogin;
    private String strPassword;
    private String session;
    private boolean status;

    RecyclerView recyclerView;
    List<ScheduleModel> schedule;
/*
    private void openSecondActivity(){
        Intent secondActivityIntent = new Intent(this,ForgetPasswordActivity.class);
        startActivity(secondActivityIntent);
    }
*/

    // записывает в SHARED_PREF_SESSION  номер сессии
    public void authRequest(String login, String password){
        App.getAuthApi().getData(login,password)
                .enqueue(new Callback<IdentificationModel>() {
                    @Override
                    public void onResponse(Call<IdentificationModel> call, Response<IdentificationModel> response) {
                        if (response.body() != null)  {
                            if (response.body().getStatus()){
                                String s = "PSWD CORRECT";
                                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                            } else {
                                String s = "PASSWORD WRONG";
                                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String s = "No response";
                            Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<IdentificationModel> call, Throwable t) {
                        Toast.makeText(MainActivity.this,"Sorry, no response",
                                Toast.LENGTH_SHORT).show();
                    }
                });

    } // end of authRequest


    public void sheduleRequest(String session){
        App.getScheduleApi().getData(session).enqueue(new Callback<List<ScheduleModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<ScheduleModel>> call, Response<List<ScheduleModel>> response) {
                if (response.body() != null) {
                    String str = response.body().get(0).getDate();
                    if (schedule.size() != 0){
                        schedule.clear();
                    }
                    schedule.addAll(response.body());
                    Toast.makeText(MainActivity.this,"str",
                            Toast.LENGTH_SHORT).show();
                    sharedPref = getSharedPreferences(SHARED_PREF,MODE_PRIVATE);
                    editor = sharedPref.edit();

                    Gson gson =  new Gson();
                    String schedJson = gson.toJson(schedule);
                    editor.putString(SHARED_SCHEDULE,schedJson);
                    editor.apply();


                    Type listType = new TypeToken<List<ScheduleModel>>() {}.getType();
                    List<ScheduleModel> restoredSchedule = gson.fromJson(schedJson,listType);

                    ScheduleAdapter adapter = new ScheduleAdapter(restoredSchedule);
                    recyclerView.setAdapter(adapter);
                    recyclerView.getAdapter().notifyDataSetChanged();


                    if (sharedPref.contains(SHARED_SCHEDULE)){

                        Toast.makeText(MainActivity.this, "schedule saved", Toast.LENGTH_SHORT).show();
                    }


                } else {
                    String s = "RESPONSE NULL";
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ScheduleModel>> call, Throwable t) {
                String s = "FAIL";
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    } // end of scheduleRequest


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Расписание ЦПСМИ");

        schedule = new ArrayList<>();
        recyclerView = findViewById(R.id.schedule_recycle_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);




        TextView mainTitle = findViewById(R.id.main_title);

        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        mainTitle.setText(currentDateTimeString);
        debugMessage = findViewById(R.id.debug_text);
        debugMessage.setText("");

        // проверить нет ли расписания в шаред преференсиес
        sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        status = false;

        if (sharedPref.contains(SHARED_SCHEDULE)){
            Gson gson =  new Gson();
            Type listType = new TypeToken<List<ScheduleModel>>() {}.getType();
            List<ScheduleModel> restoredSchedule = gson.fromJson(sharedPref.getString(SHARED_SCHEDULE,null),listType);

            if (restoredSchedule != null){
                ScheduleAdapter adapter = new ScheduleAdapter(restoredSchedule);
                recyclerView.setAdapter(adapter);
                recyclerView.getAdapter().notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Schedule found and shown",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Schedule found and null",
                        Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(MainActivity.this, "Schedule not saved",
                    Toast.LENGTH_SHORT).show();
        }

        if (sharedPref.contains(SHARED_PREF_LOG) && sharedPref.contains(SHARED_PREF_PASS)) {
            // если есть сохраненные пароль и логин
            // - постучать на сервер получить номер сессии, записать его
            // после чего скачать расписание и прочие данные
            strLogin = sharedPref.getString(SHARED_PREF_LOG,"");
            strPassword = sharedPref.getString(SHARED_PREF_PASS,"");
            String s = " Login and password are " + strLogin + " " + strPassword;
            debugMessage.append(s);
            strPassword = DateFormat.getDateTimeInstance().format(new Date());
            App.getAuthApi().getData(strLogin, strPassword)
                    .enqueue(new Callback<IdentificationModel>() {
                        @Override
                        public void onResponse(Call<IdentificationModel> call,
                                               @NonNull Response<IdentificationModel> response) {
                            if (response.body() != null) {
                                //authRequest(strLogin, strPassword);
                                session = response.body().getSession();
                                editor.putString(SHARED_PREF_SESSION,session);
                                editor.apply();
                                editor.commit();
                                String s = "SESSION SAVED";
                                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(MainActivity.this, "No response",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<IdentificationModel> call, Throwable t) {
                            Toast.makeText(MainActivity.this,"Sorry, no response",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            //
            String s = " Please Sign In";
            debugMessage.append(s);
            DialogSignIn signIn = new DialogSignIn();
            signIn.show(getSupportFragmentManager(),"SignIn");
        }

        Button signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sharedPref.contains(SHARED_PREF_LOG) || !sharedPref.contains(SHARED_PREF_PASS)){
                    // считать пароль-логин из диалогового окна
                    //  записать в шаред преференсы
                    DialogSignIn signIn = new DialogSignIn();
                    signIn.show(getSupportFragmentManager(),"SignIn");
                } else {
                    Toast.makeText(MainActivity.this,"You're signed already",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

       Button checkEmailButton = findViewById(R.id.check_button);
       checkEmailButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               if (sharedPref.contains(SHARED_PREF_LOG) && sharedPref.contains(SHARED_PREF_PASS)){
                   String login = sharedPref.getString(SHARED_PREF_LOG,"");
                   String pswd = sharedPref.getString(SHARED_PREF_PASS,"");

                   authRequest(login,pswd);
               } else {
                   Toast.makeText(MainActivity.this,"Sign In",
                           Toast.LENGTH_SHORT).show();
               }
           }
       });//end of checkEmailButton.setOnClickListener


        Button clearUserPswdButton = findViewById(R.id.clear_pswd);
        clearUserPswdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sharedPref.contains(SHARED_PREF_LOG) && sharedPref.contains(SHARED_PREF_PASS)){
                    editor.remove(SHARED_PREF_LOG);
                    editor.remove(SHARED_PREF_PASS);
                    editor.apply();
                    editor.commit();
                    debugMessage.append("Log and Pass are removed");
                } else {
                    debugMessage.append("Log and Pass are empty");
                }
            }
        });

        Button loadData = findViewById(R.id.load_data);
        loadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String param = sharedPref.getString(SHARED_PREF_SESSION,"");
                sheduleRequest(param);

            }
        });

    }






}
