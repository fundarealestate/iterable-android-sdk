package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableNotificationBuilder extends NotificationCompat.Builder {
    static final String TAG = "IterableNotification";
    private boolean isGhostPush;
    private String imageUrl;
    private String expandedContent;
    int requestCode;
    IterableNotificationData iterableNotificationData;

    /**
     * Creates a custom Notification builder
     * @param context
     * @param channelId
     */
    protected IterableNotificationBuilder(Context context, String channelId) {
        super(context, channelId);
    }

    /**
     * Sets the image url
     * @param imageUrl
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Sets the expanded content used for backwards compatibility up to Android API 23
     * @param content
     */
    public void setExpandedContent(String content) {
        this.expandedContent = content;
    }

    /**
     * Combine all of the options that have been set and return a new {@link Notification}
     * object.
     * Download any optional images
     */
    public Notification build() {
        NotificationCompat.Style style = null;

        if (this.imageUrl != null) {
            try {
                URL url = new URL(this.imageUrl);
                URLConnection connection = url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                Bitmap notificationImage = BitmapFactory.decodeStream(connection.getInputStream());
                if (notificationImage != null) {
                    style = new NotificationCompat.BigPictureStyle()
                            .bigPicture(notificationImage)
                            .setSummaryText(expandedContent);
                } else {
                    IterableLogger.e(TAG, "Notification image could not be loaded from url: " + this.imageUrl);
                }
            } catch (MalformedURLException e) {
                IterableLogger.e(TAG, e.toString());
            } catch (IOException e) {
                IterableLogger.e(TAG, e.toString());
            }
        }

        //Sets the default BigTextStyle if the imageUrl isn't set or cannot be loaded.
        if (style == null) {
            style = new NotificationCompat.BigTextStyle().bigText(expandedContent);
        }

        this.setStyle(style);

        return super.build();
    }

    /**
     * Creates and returns an instance of IterableNotification.
     *
     * @param context
     * @param extras
     * @param classToOpen
     * @return Returns null if the intent comes from an Iterable ghostPush
     */
    public static IterableNotificationBuilder createNotification(Context context, Bundle extras, Class classToOpen) {
        int stringId = context.getApplicationInfo().labelRes;
        String applicationName = context.getString(stringId);
        String title = null;
        String notificationBody = null;
        String soundName = null;
        String messageId = null;
        String pushImage = null;
        //TODO: When backend supports channels, these strings needs to change (channelName, channelId, channelDescription).
        String channelName = "iterable channel";
        String channelId = context.getPackageName();
        String channelDescription = "";

        registerChannelIfEmpty(context, channelId, channelName, channelDescription);
        IterableNotificationBuilder notificationBuilder = new IterableNotificationBuilder(context, context.getPackageName());
        if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
            title = extras.getString(IterableConstants.ITERABLE_DATA_TITLE, applicationName);
            notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY);
            soundName = extras.getString(IterableConstants.ITERABLE_DATA_SOUND);

            String iterableData = extras.getString(IterableConstants.ITERABLE_DATA_KEY);
            notificationBuilder.iterableNotificationData = new IterableNotificationData(iterableData);
            messageId = notificationBuilder.iterableNotificationData.getMessageId();

            try {
                JSONObject iterableJson = new JSONObject(iterableData);
                if (iterableJson.has(IterableConstants.ITERABLE_DATA_PUSH_IMAGE)) {
                    pushImage = iterableJson.getString(IterableConstants.ITERABLE_DATA_PUSH_IMAGE);
                }
            } catch (JSONException e) {
                IterableLogger.w(TAG, e.toString());
            }
        }

        Intent mainIntentWithExtras = new Intent(IterableConstants.ACTION_NOTIF_OPENED);
        mainIntentWithExtras.setClass(context, classToOpen);
        mainIntentWithExtras.putExtras(extras);
        mainIntentWithExtras.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Notification notifPermissions = new Notification();
        notifPermissions.defaults |= Notification.DEFAULT_LIGHTS;

        notificationBuilder
                .setSmallIcon(getIconId(context))
                .setTicker(applicationName).setWhen(0)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentText(notificationBody);
        notificationBuilder.setImageUrl(pushImage);
        notificationBuilder.setExpandedContent(notificationBody);

        if (soundName != null) {
            //Removes the file type from the name
            String[] soundFile = soundName.split("\\.");
            soundName = soundFile[0];

            if (!soundName.equalsIgnoreCase(IterableConstants.DEFAULT_SOUND)) {
                int soundID = context.getResources().getIdentifier(soundName, IterableConstants.SOUND_FOLDER_IDENTIFIER, context.getPackageName());
                Uri soundUri = Uri.parse(IterableConstants.ANDROID_RESOURCE_PATH + context.getPackageName() + "/" + soundID);
                notificationBuilder.setSound(soundUri);
            } else {
                notifPermissions.defaults |= Notification.DEFAULT_SOUND;
            }

        } else {
            notifPermissions.defaults |= Notification.DEFAULT_SOUND;
        }

        notificationBuilder.requestCode = (int) System.currentTimeMillis();
        if (messageId != null) {
            notificationBuilder.requestCode = messageId.hashCode();
        }
        PendingIntent notificationClickedIntent = PendingIntent.getActivity(context, notificationBuilder.requestCode,
                mainIntentWithExtras, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(notificationClickedIntent);
        notificationBuilder.isGhostPush = isGhostPush(extras);

        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            notificationBuilder.setColor(info.metaData.getInt(IterableConstants.NOTIFICATION_COLOR));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        PackageManager pm = context.getPackageManager();
        if (pm.checkPermission(android.Manifest.permission.VIBRATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            notifPermissions.defaults |= Notification.DEFAULT_VIBRATE;
        }

        notificationBuilder.setDefaults(notifPermissions.defaults);

        return notificationBuilder;
    }

    /**
     * Posts the notification on device.
     * Only sets the notification if it is not a ghostPush/null iterableNotification.
     *
     * @param context
     * @param iterableNotificationBuilder Function assumes that the iterableNotification is a ghostPush
     *                             if the IterableNotification passed in is null.
     */
    public static void postNotificationOnDevice(Context context, IterableNotificationBuilder iterableNotificationBuilder) {
        if (!iterableNotificationBuilder.isGhostPush) {
            NotificationManager mNotificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(iterableNotificationBuilder.requestCode, iterableNotificationBuilder.build());
        }
    }

    /**
     * Creates the notification channel on device.
     * Only creates the notification channel if application does not have notification channel created.
     *
     * @param context
     * @param channelId          Determines the channel Id. This distinguishes if the app has different channel or not.
     * @param channelName        Sets the channel name that is shown to the user.
     * @param channelDescription Sets the channel description that is shown to the user.
     */
    private static void registerChannelIfEmpty(Context context, String channelId, String channelName, String channelDescription) {
        NotificationManager mNotificationManager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                && mNotificationManager != null
                && mNotificationManager.getNotificationChannel(channelId) == null) {
            IterableLogger.d(TAG, "Creating notification: channelId = " + channelId + " channelName = "
                    + channelName + " channelDescription = " + channelDescription);
            mNotificationManager.createNotificationChannel(createNotificationChannel(channelId, channelName, channelDescription));
        }
    }

    private static NotificationChannel createNotificationChannel(String channelId, String channelName, String channelDescription) {
        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(channelDescription);
            notificationChannel.enableLights(true);
        }
        return notificationChannel;
    }

    /**
     * Returns the iconId from potential resource locations
     *
     * @param context
     * @return
     */
    private static int getIconId(Context context) {
        int iconId = 0;

        //Get the iconId set in the AndroidManifest.xml
        if (iconId == 0) {
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                iconId = info.metaData.getInt(IterableConstants.NOTIFICATION_ICON_NAME, 0);
                IterableLogger.d(TAG, "iconID: " + info.metaData.get(IterableConstants.NOTIFICATION_ICON_NAME));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        //Get the iconId set in code
        if (iconId == 0) {
            iconId = context.getResources().getIdentifier(
                    IterableApi.getNotificationIcon(context),
                    IterableConstants.ICON_FOLDER_IDENTIFIER,
                    context.getPackageName());
        }

        //Get id from the default app settings
        if (iconId == 0) {
            if (context.getApplicationInfo().icon != 0) {
                IterableLogger.d(TAG, "No Notification Icon defined - defaulting to app icon");
                iconId = context.getApplicationInfo().icon;
            } else {
                IterableLogger.w(TAG, "No Notification Icon defined - push notifications will not be displayed");
            }
        }

        return iconId;
    }

    /**
     * Returns if the given notification is a ghost/silent push notification
     *
     * @param extras
     * @return
     */
    static boolean isGhostPush(Bundle extras) {
        boolean isGhostPush = false;
        if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
            String iterableData = extras.getString(IterableConstants.ITERABLE_DATA_KEY);
            IterableNotificationData data = new IterableNotificationData(iterableData);
            isGhostPush = data.getIsGhostPush();
        }

        return isGhostPush;
    }

    /**
     * Returns if the given notification has an empty body
     * @param extras
     * @return
     */
    static boolean isEmptyBody(Bundle extras) {
        String notificationBody = "";
        if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
            notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY, "");
        }

        return notificationBody.isEmpty();
    }
}
