package com.example.gamrian.anonymeet.FireBaseChat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.Tapp.Anonymeet1.R;
import com.example.gamrian.anonymeet.GPS.FindPeopleActivity;
import com.example.gamrian.anonymeet.GPS.FindPeopleFragment;
import com.example.gamrian.anonymeet.GPS.LocationListenerService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class ChatActivity extends AppCompatActivity {

    static DatabaseReference myFirebaseRef;
    static String userWith;
    static String myNickname;
    TextView userTitle;
    TextView ageTitle;
    TextView genderTitle;
    EditText SendMessage;
    HelperDB db;
    ImageView isRead;
    SharedPreferences preferences;
    SharedPreferences.Editor se;
    String lastMessage;
    static boolean active = false;
    Context context;
    static RecyclerView recyclerView;
    static ChatAdapter recyclerAdapter;



    @Override
    protected void onStart() {
        active = true;
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        active = false;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        active = true;
        super.onResume();
    }

    @Override
    protected void onUserLeaveHint() {
        if (!FindPeopleActivity.isRunning())
            FindPeopleFragment.exit();
        super.onUserLeaveHint();
    }

    public static boolean isActive(){
        return active;
    }


    @Override
    protected void onStop() {
        active = false;
        if (FindPeopleActivity.isRunning() && FindPeopleActivity.showMyself() && LocationListenerService.providerEnabled)
            LocationListenerService.buildNotification();
        super.onStop();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        db = new HelperDB(this);
        context = this;
        userWith = getIntent().getStringExtra("usernameTo");
        recyclerView = (RecyclerView)findViewById(R.id.chatList);
        SendMessage = (EditText)findViewById(R.id.sendMessage);
        isRead = (ImageView)findViewById(R.id.read);
        userTitle = (TextView)findViewById(R.id.chat_user_title);
        ageTitle = (TextView)findViewById(R.id.chat_title_age);
        genderTitle = (TextView)findViewById(R.id.chat_title_gender);

        userTitle.setText(userWith);
        genderTitle.setText(db.getUserGender(userWith));
        if(genderTitle.getText().toString().equals("male")) genderTitle.setTextColor(Color.parseColor("#1acefc"));
        else genderTitle.setTextColor(Color.parseColor("#CB82FD"));

        preferences = getSharedPreferences("data", MODE_PRIVATE);
        se = preferences.edit();

        if(db.userExists(userWith)) {
            se.putInt("numOfNoti", preferences.getInt("numOfNoti", 0) - preferences.getInt("user " + userWith, 0));
            se.putInt("user " + userWith, 0);
            se.commit();

            setUpChatMessages();
        }

        lastMessage = preferences.getString("lastMessage", "");
        myNickname = preferences.getString("nickname", "");
        myFirebaseRef = FirebaseDatabase.getInstance().getReference().child("Chat");

        myFirebaseRef.child(myNickname).child(userWith).child("read").setValue("true");


        myFirebaseRef.child(userWith).child(myNickname).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("hiiiiiiii", "last message with: " + db.getMyLastMessageWith(userWith));

                if(db.getMyLastMessageWith(userWith).equals("")) {
                    isRead.setVisibility(View.INVISIBLE);
                }
                else if(!dataSnapshot.child("arrived").exists()) {
                    isRead.setImageResource(R.drawable.not_arrived);
                    isRead.setVisibility(View.VISIBLE);
                }
                else if(dataSnapshot.child("arrived").exists()){
                    if (dataSnapshot.child("read").exists()) isRead.setImageResource(R.drawable.read);

                    else isRead.setImageResource(R.drawable.unread);

                    isRead.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void setUpChatMessages() {
        recyclerAdapter = new ChatAdapter(this, userWith);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.getLayoutManager().scrollToPosition(recyclerAdapter.messages.size() - 1);
        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                scrollDown();
            }
        });
    }

    public void onClick(final View view){

        if(!db.userExists(userWith)) {
            db.insertUser(userWith, getIntent().getStringExtra("gender"), System.currentTimeMillis());
            setUpChatMessages();

        }
        myFirebaseRef.child(userWith).child(myNickname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.child("message").exists() || dataSnapshot.child("arrived").exists()){

                    if(!SendMessage.getText().toString().equals("")) {

                        myFirebaseRef.child(userWith).child(myNickname).child("read").removeValue();
                        myFirebaseRef.child(userWith).child(myNickname).child("arrived").removeValue();



                        String message;
                        lastMessage = preferences.getString("lastMessage", "");
                        db.updateDateOfUser(userWith, System.currentTimeMillis());
                        db.insertMessage(userWith, SendMessage.getText().toString(), true);
                        recyclerAdapter.syncMessages();
                        if (SendMessage.getText().toString().equals(lastMessage)) {
                            message = "cbd9b0a2-d183-45ee-9582-27df3020ff65" + SendMessage.getText().toString();

                        } else {
                            message = SendMessage.getText().toString();

                        }
                        myFirebaseRef.child(userWith).child(myNickname).child("message").setValue(message);
                        se.putString("lastMessage", message).commit();
                        SendMessage.setText("");
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                        scrollDown();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "You need to wait until the user gets the message in order to send another one", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    public static void scrollDown(){
        recyclerView.getLayoutManager().scrollToPosition(recyclerAdapter.getItemCount() - 1);
        myFirebaseRef.child(myNickname).child(userWith).child("read").setValue("true");
    }
}
