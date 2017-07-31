package com.example.gamrian.anonymeet.FireBaseChat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.Tapp.Anonymeet1.R;

public class MessagesFragment extends Fragment implements MyListener {

    public MessagesFragment() {

    }

    RecyclerView recyclerView;
    static UsersAdapter usersAdapter;
    static Context ctx;
    SharedPreferences preferences;
    FrameLayout fragmentContainer;
    RelativeLayout contactsLayout;

    @Override
    public void onResume() {
        super.onResume();
        syncContacts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.activity_messages, container, false);


        ctx = getActivity();

        preferences = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycle);
        contactsLayout = (RelativeLayout) view.findViewById(R.id.contacts_layout);


        fragmentContainer = (FrameLayout) view.findViewById(R.id.frame);

        usersAdapter = new UsersAdapter(getContext(), getActivity(), this, fragmentContainer, recyclerView, contactsLayout);

        recyclerView.setAdapter(usersAdapter);
        CustomRecyclerViewLayoutManager layoutManager = new CustomRecyclerViewLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);





        return view;
    }

    @Override
    public void onItemClick(View view, int position, String name) {
        Intent myintent = new Intent(ctx, ChatActivity.class).putExtra("usernameTo", usersAdapter.contacts.get(position).name);
        startActivity(myintent);
    }

    public void syncContacts() {
        Log.d("MYLOG", "fragment");

        usersAdapter.syncContacts();

    }

    public void onClickMainLayout(View v) {
        Log.i("hiiiiiiiiiii", "happens");
        if (usersAdapter.contactMenuPosition != -1) {
            getFragmentManager().beginTransaction().remove(getFragmentManager().getFragment(null, "contact_menu")).commit();

            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                recyclerView.getChildAt(i).setEnabled(true);
            }
        }

    }

    public void itemInsertedIn(int position) {
        Log.d("MYLOG", "fragment");

        usersAdapter.itemInsertedIn(position);

    }
}
