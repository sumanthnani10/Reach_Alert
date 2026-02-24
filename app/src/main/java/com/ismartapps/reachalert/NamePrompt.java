package com.ismartapps.reachalert;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NamePrompt extends Activity {
    private static final String TAG = "NamePrompt";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.name_prompt);
        String TAG = "NP";
        Log.d(TAG, "onCreate");
        EditText editText = findViewById(R.id.name_text);
        Button button = findViewById(R.id.btn);
        button.setActivated(false);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().equals("")) {
                    button.setActivated(true);
                }

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editText.getText().toString().isEmpty()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    String name = editText.getText().toString();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("mail", "");
                    userData.put("phone", user.getPhoneNumber());
                    userData.put("login_type", "phone");
                    userData.put("created", FieldValue.serverTimestamp());
                    userData.put("last_opened", FieldValue.serverTimestamp());

                    FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(userData)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    SharedPreferences sharedPreferences = getSharedPreferences("userdetails", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.clear();
                                    editor.putString("name", name);
                                    editor.apply();
                                    button.setClickable(false);
                                    startMain();
                                } else {
                                    new AlertDialog.Builder(NamePrompt.this)
                                            .setTitle("Hey, " + user.getDisplayName())
                                            .setMessage("Please check your Internet Connection and try again.")
                                            .setPositiveButton("Ok", null)
                                            .show();
                                }
                            });
                } else {
                    editText.setError("Enter Name");
                    editText.requestFocus();
                }
            }
        });
    }

    public void startMain(){
        Log.d(TAG, "startMain");
        Intent intent = getIntent();
        Intent mainIntent = new Intent(this, MapsActivityPrimary.class);

        if(intent.getStringExtra("from").equals("SAN"))
        {
            String text = intent.getStringExtra("name");
            Log.d(TAG, "onCreate: "+text+" , "+intent.getExtras());
            double[] latLng = intent.getExtras().getDoubleArray("latlng");
            mainIntent.putExtra("name",text);
            mainIntent.putExtra("placeId",intent.getStringExtra("placeId"));
            mainIntent.putExtra("latlng",latLng);
        }

        else if(intent.getStringExtra("shared location")!=null)
        {
            Log.d(TAG, "startMain: ---------------");
            String s = intent.getStringExtra("shared location");
            mainIntent.putExtra("shared location",s);
        }
        startActivity(mainIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
    }
}
