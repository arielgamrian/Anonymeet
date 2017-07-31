package com.example.gamrian.anonymeet.FireBaseChat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.Tapp.Anonymeet1.R;
import com.example.gamrian.anonymeet.GPS.FindPeopleActivity;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class MyService extends Service implements ChildEventListener {


    HelperDB db;
    DatabaseReference myFirebaseChat;
    DatabaseReference myFirebaseUsers;
    NotificationManager nm;
    SharedPreferences preferences;
    SharedPreferences.Editor se;
    String gender;
    String message;
    public static boolean isActive;

    public MyService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("hiiiiiiiiiiii", "onCreate");
        isActive = true;
        db = new HelperDB(getApplicationContext());
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        SharedPreferences preferences = getSharedPreferences("data", MODE_PRIVATE);
        String myNickname = preferences.getString("nickname", "");
        myFirebaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        myFirebaseChat = FirebaseDatabase.getInstance().getReference().child("Users");
        myFirebaseChat = myFirebaseChat.child(myNickname);
        myFirebaseChat.addChildEventListener(this);
        preferences = getSharedPreferences("data", MODE_PRIVATE);
        se = preferences.edit();

        se.commit();
    }


    @Override
    public void onDestroy() {
        Log.i("hiiiiiiiiiiii", "onDestroy");
        myFirebaseChat.removeEventListener(this);
        isActive = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        preferences = getSharedPreferences("data", MODE_PRIVATE);
        se = preferences.edit();
        return super.onStartCommand(intent, flags, startId);
    }




    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {


        final String userWith = dataSnapshot.getKey().toString();

        if (dataSnapshot.child("message").exists()){
            myFirebaseChat.child(userWith).child("arrived").setValue("true");
            message = dataSnapshot.child("message").getValue().toString();

        }

    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        final String userWith = dataSnapshot.getKey().toString();

        if (preferences.getInt("numOfNoti", -1) == -1 && preferences.getString("LastMessage", "").equals("")) {
            se.putInt("numOfNoti", 0);
            se.putString(userWith + "LastMessage", dataSnapshot.child("message").getValue().toString());
            se.commit();

        }
        else {


            //checking if it was really the message child which was changed

            if (!(preferences.getString(userWith + "LastMessage", "").equals(dataSnapshot.child("message").getValue().toString()))) {

                Log.i("hiiiiiiiiii", "a message has been received: " + dataSnapshot.child("message").getValue().toString());

                message = dataSnapshot.child("message").getValue().toString();
                se.putString(userWith + "LastMessage", message);
                se.commit();

                if (!db.userExists(userWith)){

                    myFirebaseUsers.child(userWith).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            gender = dataSnapshot.child("gender").getValue().toString();
                            db.insertUser(dataSnapshot.getKey().toString(), gender, System.currentTimeMillis());
                            db.insertMessage(dataSnapshot.getKey().toString(), cleanCode(message), false);

                            if (FindPeopleActivity.isRunning()) {

                                FindPeopleActivity.getF2().recyclerView.invalidate();
                                FindPeopleActivity.getF2().itemInsertedIn(0);


                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
                else {
                    db.updateDateOfUser(userWith, System.currentTimeMillis());
                    db.insertMessage(dataSnapshot.getKey().toString(), cleanCode(message), false);
                    if (FindPeopleActivity.isRunning()) {
                        FindPeopleActivity.getF2().syncContacts();
                    }
                }

                if (ChatActivity.isActive() && ChatActivity.userWith.equals(dataSnapshot.getKey().toString())) {
                    ChatActivity.recyclerAdapter.syncMessages();
                    ChatActivity.scrollDown();
                } else {
                    preferences = getSharedPreferences("data", MODE_PRIVATE);
                    se = preferences.edit();
                    int num = preferences.getInt("user " + userWith, 0);
                    se.putInt("user " + userWith, 1 + num);
                    se.putInt("numOfNoti", preferences.getInt("numOfNoti", 0) + 1);
                    se.commit();

                    if (preferences.getInt("numOfNoti", 0) == 1)
                        notifyOne(dataSnapshot.getKey().toString(), cleanCode(dataSnapshot.child("message").getValue().toString()));
                    else
                        notifyFew();

                }

                myFirebaseChat.child(userWith).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.getKey().toString().equals("message")) {
                            Random rnd = new Random();
                            myFirebaseChat.child(userWith).child("arrived").setValue("true" + rnd.nextInt(1000000));
                            myFirebaseChat.child(userWith).removeEventListener(this);
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.getKey().toString().equals("arrived")) {
                            Random rnd = new Random();

                            myFirebaseChat.child(userWith).child("arrived").setValue("true" + rnd.nextInt(1000000));
                            myFirebaseChat.child(userWith).removeEventListener(this);
                        }
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
        }
    }

    public String cleanCode(String m) {
        if (m.length() > 36 && m.substring(0, 36).equals("cbd9b0a2-d183-45ee-9582-27df3020ff65")) {
            m = m.substring(36);
        }
        return m;
    }

    public void notifyOne(String sender, String m){
        Notification.Builder n = new Notification.Builder(getApplicationContext())
                .setContentTitle(sender + " sent a message")
                .setContentText(m)
                .setSmallIcon(R.drawable.contact)
                .setAutoCancel(true)
                .setTicker("New anonymous message")
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND);
        TaskStackBuilder t = TaskStackBuilder.create(getApplicationContext());

        Intent i1 = new Intent(getApplicationContext(), ChatActivity.class);

        i1.putExtra("usernameTo", sender);

        t.addNextIntentWithParentStack(i1);

        PendingIntent pendingIntent = t.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        n.setContentIntent(pendingIntent);
        nm.notify(0, n.build());
    }

    public void notifyFew(){
        Notification.Builder n = new Notification.Builder(getApplicationContext())
                .setContentTitle("You have " + preferences.getInt("numOfNoti", 0) + " new messages")
                .setSmallIcon(R.drawable.contact)
                .setAutoCancel(true)
                .setTicker("New anonymous message")
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND);
        TaskStackBuilder t = TaskStackBuilder.create(getApplicationContext());
        Intent i = new Intent(getApplicationContext(), FindPeopleActivity.class);
        i.putExtra("fromNoti", true);
        t.addParentStack(FindPeopleActivity.class);
        t.addNextIntent(i);
        PendingIntent pendingIntent = t.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        n.setContentIntent(pendingIntent);
        nm.notify(0, n.build());
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
    }
}
