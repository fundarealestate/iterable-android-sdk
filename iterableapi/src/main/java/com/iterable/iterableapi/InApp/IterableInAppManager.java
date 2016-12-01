package com.iterable.iterableapi.InApp;

import android.app.Dialog;
import android.content.Context;

import android.graphics.Color;

import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableLogger;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {
    static final String TAG = "IterableInAppManager";

    public static void showNotification(Context context, JSONObject dialogOptions, IterableInAppActionListener.IterableOnClick clickCallback) {
        showFullScreenDialog(context, dialogOptions, clickCallback);
    }

    public static void showNotificationDialog(Context context, JSONObject dialogParameters, IterableInAppActionListener.IterableOnClick clickCallback) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_NoActionBar); //Theme_Material_NoActionBar_Overscan
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = .8f;
        lp.gravity = Gravity.TOP;
        window.setAttributes(lp);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);

        String backgroundColorParam = dialogParameters.optString(IterableConstants.ITERABLE_IN_APP_BACKGROUND_COLOR);
        int backgroundColor = Color.WHITE;
        if (!backgroundColorParam.isEmpty()) {
            backgroundColor = Color.parseColor(backgroundColorParam);
        }
        verticalLayout.setBackgroundColor(backgroundColor);

        Point screenSize = getScreenSize(context);
        int fontConstant = getFontConstant(screenSize);

        JSONObject titleJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_TITLE);
        if (titleJson != null) {
            //Title Text
            TextView title = new TextView(context);
            title.setText("Iterable");
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 24);
            title.setGravity(Gravity.CENTER);
            //TODO: update padding to be orientation relative
            title.setPadding(screenSize.x / 30, screenSize.y / 30, screenSize.x / 30, 0);

            int titleColor = Color.BLACK;
            String titleColorParam = titleJson.optString(IterableConstants.ITERABLE_IN_APP_COLOR);
            if (!titleColorParam.isEmpty()) {
                titleColor = Color.parseColor(titleColorParam);
            }
            title.setTextColor(titleColor);
            verticalLayout.addView(title);
        }

        JSONObject bodyJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_BODY);
        if (bodyJson != null) {
            //Body Text
            TextView bodyText = new TextView(context);
            bodyText.setText(bodyJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 36);
            bodyText.setGravity(Gravity.CENTER);
            bodyText.setTextColor(Color.parseColor(bodyJson.optString(IterableConstants.ITERABLE_IN_APP_COLOR)));
            bodyText.setPadding(screenSize.x/60,0,screenSize.x/60,screenSize.y/60);
            verticalLayout.addView(bodyText);
        }

        JSONArray buttonJson = dialogParameters.optJSONArray(IterableConstants.ITERABLE_IN_APP_BUTTONS);
        if (buttonJson != null) {
            verticalLayout.addView(createButtons(context, buttonJson, clickCallback));
        }

        dialog.setContentView(verticalLayout);

        //TODO: track notif displayed here, or when it gets removed from the queue.

        dialog.show();
    }

    public static void showFullScreenDialog(Context context, JSONObject dialogParameters, IterableInAppActionListener.IterableOnClick clickCallback) {

        Dialog dialog = new Dialog(context, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);

        int colorCol = Color.parseColor(dialogParameters.optString(IterableConstants.ITERABLE_IN_APP_BACKGROUND_COLOR));

        verticalLayout.setBackgroundColor(colorCol);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        verticalLayout.setLayoutParams(linearLayoutParams);

        LinearLayout.LayoutParams equalParamHeight = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParamHeight.weight = 1;
        equalParamHeight.height = 0;

        Point size = getScreenSize(context);
        int dialogWidth = size.x;
        int dialogHeight = size.y;
        int fontConstant = getFontConstant(size);

        JSONObject titleJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_TITLE);
        if (titleJson != null) {
            TextView title = new TextView(context);
            title.setText(titleJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 24);
            title.setGravity(Gravity.CENTER);
            title.setTextColor(Color.parseColor((titleJson.optString(IterableConstants.ITERABLE_IN_APP_COLOR))));
            verticalLayout.addView(title, equalParamHeight);
        }

        ImageView imageView = new ImageView(context);
        verticalLayout.addView(imageView);
        try {
            Class picassoClass = Class.forName(IterableConstants.PICASSO_CLASS);
            if (picassoClass != null) {

                Picasso.
                        with(context.getApplicationContext()).
                        load(dialogParameters.optString(IterableConstants.ITERABLE_IN_APP_MAIN_IMAGE)).
                        resize(dialogWidth, dialogHeight/2).
                        centerInside().
                        into(imageView);
            }
        } catch (ClassNotFoundException e) {
            IterableLogger.e(TAG, "ClassNotFoundException: Check that picasso is added " +
                    "to the build dependencies", e);
        }

        //Body Text
        JSONObject bodyJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_BODY);
        if (bodyJson != null) {
            TextView bodyText = new TextView(context);
            bodyText.setText(bodyJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 36);
            bodyText.setGravity(Gravity.CENTER);
            bodyText.setTextColor(Color.parseColor((bodyJson.optString(IterableConstants.ITERABLE_IN_APP_COLOR))));
            verticalLayout.addView(bodyText, equalParamHeight);
        }

        //Buttons
        JSONArray buttonJson = dialogParameters.optJSONArray(IterableConstants.ITERABLE_IN_APP_BUTTONS);
        if (buttonJson != null) {
            View bottomButtons = createButtons(context, buttonJson, clickCallback);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.height = dialogHeight / 10;
            verticalLayout.addView(bottomButtons, buttonParams);
        }

        dialog.setContentView(verticalLayout);
        //dialog.setCancelable(false);
        dialog.show();
    }

    public static View createButtons(Context context, JSONArray buttons, IterableInAppActionListener.IterableOnClick clickCallback) {
        RelativeLayout bottomButtons = new RelativeLayout(context);
        LinearLayout.LayoutParams equalParamWidth = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParamWidth.weight = 1;
        equalParamWidth.width = 0;

        LinearLayout linearlayout = new LinearLayout(context);
        linearlayout.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < buttons.length(); i++) {
            JSONObject buttonJson = buttons.optJSONObject(i);
            if (buttonJson != null) {
                final Button button = new Button(context);
                button.setBackgroundColor(getIntColorFromJson(buttonJson, IterableConstants.ITERABLE_IN_APP_BACKGROUND_COLOR, Color.LTGRAY));

                button.setOnClickListener(new IterableInAppActionListener(i, "string", clickCallback));

                //TODO: separate out content
                button.setTextColor(getIntColorFromJson(buttonJson, IterableConstants.ITERABLE_IN_APP_COLOR, Color.BLACK));
                button.setText("CLOSE");


                linearlayout.addView(button, equalParamWidth);
            }
        }
        return linearlayout;
    }

    public static JSONObject getNextMessageFromPayload(String s) {
        JSONObject mainObject = null;
        try {
            mainObject = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        JSONObject message = jsonArray.optJSONObject(0);
        return message.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
    }

    private static int getFontConstant(Point size) {
        int fontConstant = (size.x > size.y) ? size.x : size.y;
        return fontConstant;
    }

    private static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private static int getIntColorFromJson(JSONObject json, String key, int defaultColor) {
        String backgroundColorParam = json.optString(key);
        int backgroundColor = defaultColor;
        if (!backgroundColorParam.isEmpty()) {
            backgroundColor = Color.parseColor(backgroundColorParam);
        }
        return backgroundColor;
    }
}