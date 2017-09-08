package com.example.sfreyman.djam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.CurrentUserRequest;
import com.wrapper.spotify.models.AuthorizationCodeCredentials;
import com.wrapper.spotify.models.Image;
import com.wrapper.spotify.models.User;

import java.util.List;


public class SignInActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "c0cb1b7bf2b949eca66e7e58389bc79e";
    private static final String REDIRECT_URI = "djamspotify://callback";
    private static final String CLIENT_SECRET = "a4db4cafe4814eddba2a5d9a2450986f";
    private static final int REQUEST_CODE = 1337;

    Api api = Api.builder()
            .clientId(CLIENT_ID)
            .clientSecret(CLIENT_SECRET)
            .redirectURI(REDIRECT_URI)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Sign In");

        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    // On Spotify button click, logs the user in
    public void login(View view) {
        final AuthenticationRequest request = getAuthenticationRequest(AuthenticationResponse.Type.CODE);
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult", "Got something");

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            final SharedPreferences sharedPref = this.getSharedPreferences("api", Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = sharedPref.edit();

            switch (response.getType()) {
                // Response was successful and contains code
                case CODE:
                    final String code =  response.getCode();
                    Log.d("onActivityResult", "Got a code! " + code);

                    // Now pass code back to get auth token and refresh token
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                final AuthorizationCodeCredentials authorizationCodeCredentials = api.authorizationCodeGrant(response.getCode()).build().get();
                                final String accessToken = authorizationCodeCredentials.getAccessToken();
                                Log.d("onActivityResult", "Got a access Token! " + accessToken);
                                final String refreshToken = authorizationCodeCredentials.getRefreshToken();
                                Log.d("onActivityResult", "Got a refresh Token! " + refreshToken);

                                api = Api.builder()
                                        .clientId("c0cb1b7bf2b949eca66e7e58389bc79e")
                                        .clientSecret("a4db4cafe4814eddba2a5d9a2450986f")
                                        .redirectURI("djamspotify://callback")
                                        .accessToken(accessToken)
                                        .refreshToken(refreshToken)
                                        .build();

                                final CurrentUserRequest request = api.getMe().build();

                                // Retrieve a future for user
                                SettableFuture<User> userFuture = request.getAsync();

                                // Create callbacks in case of success or failure
                                Futures.addCallback(userFuture, new FutureCallback<User>() {

                                    // function on user grab success
                                    public void onSuccess(User user) {
                                        // Store the username
                                        String username = user.getId();
                                        List<Image> profilePictures = user.getImages();
                                        String profilePicture;

                                        if (profilePictures.size() > 0) {
                                            profilePicture = profilePictures.get(0).getUrl();
                                        } else {
                                            profilePicture = "";
                                        }

                                        editor.putString(getString(R.string.username), username);
                                        editor.putString(getString(R.string.profilePicture), profilePicture);
                                    }

                                    // In case of failure
                                    public void onFailure(Throwable thrown) {
                                    }
                                });

                                // Store our tokens in shared prefs
                                editor.putString(getString(R.string.accessToken), accessToken);
                                editor.putString(getString(R.string.refreshToken), refreshToken);
                                editor.apply();
                            } catch (Exception e) {
                            }
                        }
                    }).start();

                    editor.putString(getString(R.string.accessCode), code);
                    editor.apply();

                    // Jump to next screen
                    Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                    startActivity(intent);
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d("onActivityResult", "Got an error");
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }

    // Create our authentication request
    private AuthenticationRequest getAuthenticationRequest(AuthenticationResponse.Type type) {
        return new AuthenticationRequest.Builder(CLIENT_ID, type, REDIRECT_URI)
                .setShowDialog(true)
                // Define out scope, we only need access to streaming, suggestions, and public data
                .setScopes(new String[]{"streaming", "user-top-read", "user-read-private", "user-read-email"})
                .build();
    }
}
