package com.example.newproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.newproject.network.ApiHandler;
import com.example.newproject.network.ErrorUtils;
import com.example.newproject.network.models.DataManager;
import com.example.newproject.network.models.LoginBody;
import com.example.newproject.network.models.LoginResponse;
import com.example.newproject.network.service.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private String password;
    private String email;
    private AlertDialog alertDialog;
    private AlertDialog.Builder alertDialogBuilder;
    private SharedPreferences localStorage;
    private SharedPreferences.Editor localStorageEditor;

    ApiService service = ApiHandler.getInstance().getService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        localStorage = getSharedPreferences("settings", MODE_PRIVATE);
        localStorageEditor = localStorage.edit();
        checkAuthUser();
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
    }

    //Валидация введённых данных пользователя в форме авторизации
    public void loginUser(View view) {
        password = passwordInput.getText().toString();
        email = emailInput.getText().toString();

        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
        if(email.length() == 0) {
            alertDialogBuilder
                .setMessage("Поле Email не должно быть пустым!");
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return;
        }
        if(password.length() == 0) {
            alertDialogBuilder
                    .setMessage("Поле Пароль не должно быть пустым!");
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            alertDialogBuilder
                    .setMessage("Поле Email заполнено неверно!");
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return;
        }
        doLogin();
    }

    public void moveToRegisterScreen(View view) {
        Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(i);
    }
    //Авторизация пользователя в системе
    private void doLogin(){
        AsyncTask.execute(() -> {
            service.doLogin(getLoginData()).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if(response.isSuccessful()){
                        //Сохранение токена в sharedPreferences
                        localStorageEditor.putString("token", response.body().getToken());
                        DataManager.token = (response.body().getToken());
                        //Переход на главную страницу
                        Intent i = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(i);
                    }
                    else if (response.code() == 401){
                        Toast.makeText(getApplicationContext(), "Такого пользователя не существует", Toast.LENGTH_SHORT).show();
                    } else {
                        String serverErrorMessage = ErrorUtils.parseError(response).message();
                        Toast.makeText(getApplicationContext(), "Прошла неизвестная ошибка попробуйте позже!", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), serverErrorMessage, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private LoginBody getLoginData(){
        return new LoginBody(email, password);
    }

    //Проверка авторизован ли пользователь или нет, по токену
    private void checkAuthUser(){
        String token = localStorage.getString("token", "");
        if(!token.equals("")){
            DataManager.token = token;
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        }
    }
}