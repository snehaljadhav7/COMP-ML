package com.tanyayuferova.mnist;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.byox.drawview.views.DrawView;
import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import static com.byox.drawview.enums.DrawingCapture.BITMAP;

public class MainActivity extends AppCompatActivity {

    private DrawView drawView;
    private TextView phoneNumberView;
    private TextView guess1;
    private TextView guess2;
    private TextView guess3;
    private Toolbar toolbar;
    private MnistClassifier model;

    private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("US");
    private String phoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        model = new MnistClassifier(this);

        phoneNumberView = findViewById(R.id.phone_number_view);
        toolbar = findViewById(R.id.toolbar);
        guess1 = findViewById(R.id.guess_1);
        guess2 = findViewById(R.id.guess_2);
        guess3 = findViewById(R.id.guess_3);
        toolbar = findViewById(R.id.toolbar);
        drawView = findViewById(R.id.draw_view);

        drawView.setOnDrawViewListener(new EmptyOnDrawViewListener() {
            @Override
            public void onEndDrawing() {
                classifyDrawing();
            }
        });
        View.OnClickListener digitListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replaceLastDigit(((TextView) view).getText().toString());
            }
        };
        guess1.setOnClickListener(digitListener);
        guess2.setOnClickListener(digitListener);
        guess3.setOnClickListener(digitListener);
        model.initialize();

        toolbar.inflateMenu(R.menu.main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_clear:
                        clear();
                        return true;
                    default:
                        // If we got here, the user's action was not recognized.
                        // Invoke the superclass to handle it.
                        return false;
                }
            }
        });
        clear();
    }

    private void classifyDrawing() {
        Bitmap bitmap = (Bitmap) drawView.createCapture(BITMAP)[0];
        String[] prediction = model.classify(bitmap);
        phoneNumber += prediction[0];
        guess1.setText(prediction[1]);
        guess2.setText(prediction[2]);
        guess3.setText(prediction[3]);
        updatePhoneNumber();
        drawView.restartDrawing();
    }

    private void clear() {
        phoneNumber = "";
        updatePhoneNumber();
        phoneNumberView.setText("Draw a digit");
        guess1.setText("");
        guess2.setText("");
        guess3.setText("");
    }

    private void onBackSpaceClick() {
        phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1);
        updatePhoneNumber();
    }

    private void replaceLastDigit(String newDigit) {
        phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1);
        phoneNumber += newDigit;
        updatePhoneNumber();
    }

    private void updatePhoneNumber() {
        formatter.clear();
        String formatted = "";
        for (char c : phoneNumber.toCharArray()) {
            formatted = formatter.inputDigit(c);
        }
        phoneNumberView.setText(formatted);
    }

    @Override
    protected void onDestroy() {
        model.close();
        super.onDestroy();
    }
}
