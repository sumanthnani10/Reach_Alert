package com.ismartapps.reachalert;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import static android.content.Context.MODE_PRIVATE;

public class ConfirmationDialog extends Dialog implements View.OnClickListener {

    private String place,address;
    private TextView header,description,confirm,no;
    private Context context;
    private Activity activity;
    private TargetDetails targetDetails;
    private boolean dark;

    public ConfirmationDialog(@NonNull Context context,String place,String address,TargetDetails targetDetails,Activity activity) {
        super(context);
        this.place = place;
        this.address = address;
        this.context = context;
        this.targetDetails = targetDetails;
        this.activity=activity;
    }

    public ConfirmationDialog(@NonNull Context context,String place,String address,Activity activity) {
        super(context);
        this.place = place;
        this.address = address;
        this.context = context;
        this.activity=activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SharedPreferences settings = context.getSharedPreferences("settings", MODE_PRIVATE);
        dark = settings.getBoolean("dark",false);
        if(dark)
        {
            setContentView(R.layout.custom_dialog_dark);
        }
        else
        {
            setContentView(R.layout.custom_dialog);
        }

        header = (TextView)findViewById(R.id.dialog_header);
        description = (TextView)findViewById(R.id.dialog_description);
        header.setText(place);
        description.setText(address);
        confirm = (TextView) findViewById(R.id.confirm_button);
        no = (TextView) findViewById(R.id.no_button);
        confirm.setOnClickListener(this);
        no.setOnClickListener(this);
        LinearLayout adContainer = findViewById(R.id.adBannerDialog);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void onClick(View view) {
    }
}
