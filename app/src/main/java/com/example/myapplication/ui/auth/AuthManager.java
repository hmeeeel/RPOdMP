package com.example.myapplication.ui.auth;

import android.content.Context;

import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
public class AuthManager {
    private static AuthManager instance;
    private final FirebaseAuth auth;
    private Context appContext;

    public void init(Context context) {
        this.appContext = context.getApplicationContext();
    }
    private AuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    public static AuthManager getInstance() {
        if (instance == null) instance = new AuthManager();
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(callback::onError);
    }

    public void register(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(callback::onError);
    }

    public void signOut() {
        if (appContext != null) {
            WorkManager.getInstance(appContext).cancelAllWorkByTag("weekly_reminder");
        }
        auth.signOut();
    }

    public void reauthenticate(String email, String password, AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(new Exception("Not logged in")); return; }
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential)
                .addOnSuccessListener(v -> callback.onSuccess(user))
                .addOnFailureListener(callback::onError);
    }

    public void deleteAccount(AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(new Exception("Not logged in")); return; }
        if (appContext != null) {
            WorkManager.getInstance(appContext).cancelAllWorkByTag("weekly_reminder");
        }

        user.delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }




    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception e);
    }
}
