package com.example.gamrian.anonymeet.FireBaseChat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.Tapp.Anonymeet1.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Or on 18/01/2016.
 */
class Contact {
    int photo;
    String name;
    String gender;
    long date;

    public Contact(String name, String gender, int avatar, long date){

        this.gender = gender;
        this.date = date;
        this.name = name;



        if(gender.equals("male")) {
            switch (avatar) {
                case 0:
                    this.photo = R.drawable.boy3;
                    break;
                case 1:
                    this.photo = R.drawable.boy4;
                    break;
                case 2:
                    this.photo = R.drawable.boy5;
                    break;
                case 3:
                    this.photo = R.drawable.boy6;
                    break;
                case 4:
                    this.photo = R.drawable.boy7;
                    break;

            }
        }
        else {

                switch (avatar) {
                    case 0:
                        this.photo = R.drawable.girl3;
                        break;
                    case 1:
                        this.photo = R.drawable.girl4;
                        break;
                    case 2:
                        this.photo = R.drawable.girl5;
                        break;
                    case 3:
                        this.photo = R.drawable.girl6;
                        break;
                    case 4:
                        this.photo = R.drawable.girl7;
                        break;

                }

        }


    }

}

public class UsersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<Contact> contacts;
    LayoutInflater inflater;
    MyListener mItemClickListener;
    Context context;
    UsersAdapter adapter = this;
    HelperDB db;
    SharedPreferences preferences;
    Activity activity;
    FrameLayout fragmentContainer;
    RecyclerView recyclerView;
    RelativeLayout contactsLayout;
    ArrayList<RelativeLayout> viewLayouts;
    int contactMenuPosition;
    boolean animationSetted;
    RelativeLayout r1;
    RelativeLayout r2;
    MyViewHolder optionsHolder;




    public UsersAdapter(Context con, Activity a, MyListener myListener, FrameLayout f, RecyclerView r, RelativeLayout rl){
        setHasStableIds(true);
        viewLayouts = new ArrayList<>();
        preferences = con.getSharedPreferences("data", Context.MODE_PRIVATE);
        this.mItemClickListener = myListener;
        context = con;
        activity = a;
        animationSetted = false;
        contactMenuPosition = -1;
        inflater = LayoutInflater.from(context);
        this.db = new HelperDB(con);
        contacts = db.getContacts();
        fragmentContainer = f;
        recyclerView = r;
        contactsLayout = rl;
        Log.i("hiiiiiiiiiii", "contacts: " + contacts.size());

    }

    public void syncContacts(){
        contacts = db.getContacts();
        Log.i("hiiiiiiiiiiiiiiiii", "contacts: " + contacts.size());
        notifyDataSetChanged();


    }

    public void itemInsertedIn(int position) {
        contacts = db.getContacts();
        notifyItemInserted(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == 0){
            v = inflater.inflate(R.layout.item_recycle_view, parent, false);
            MyViewHolder viewHolder = new MyViewHolder(v);
            return viewHolder;
        }
        else{
            v = inflater.inflate(R.layout.contact_menu_fragment, parent, false);
            ContactMenuViewHolder viewHolder = new ContactMenuViewHolder(v);
            return viewHolder;
        }


    }

    @Override
    public int getItemViewType(int position) {
        if (position == contactMenuPosition) return 1;
        else return 0;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {


        if (position == contactMenuPosition) {

            setAnimation(viewHolder.itemView);
            ContactMenuViewHolder holder = (ContactMenuViewHolder) viewHolder;
            holder.setDeleteClick(position - 1);
            holder.setCancelClick(position - 1);
            holder.setEditClick(position - 1);
        }
        else {

            MyViewHolder holder = (MyViewHolder) viewHolder;
            int pos = position;
            if (position + 1 == contactMenuPosition){
                holder.removeDivider();
                optionsHolder = holder;
                holder.view.setEnabled(false);
            }
            else if (contactMenuPosition != -1) {
                if(position > contactMenuPosition) pos = pos - 1;

            }
            holder.view.bringToFront();
            final Contact c = contacts.get(pos);
            holder.image.setImageResource(c.photo);
            holder.name.setText(c.name);
            Log.i("hiiiiiiiiii", "last: " + holder.checkIfLast(pos));

            if (holder.checkIfLast(pos)) holder.removeDivider();
            holder.setTheLongClickListener(pos);
            holder.setHour(c.date);
            holder.lastMessage.setText(db.getLastMessageWith(c.name));
            preferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
            int num = preferences.getInt("user " + c.name, 0);
            if(num > 0){
                holder.alert.setText(""+num);
                holder.alert.setVisibility(View.VISIBLE);
            } else{
                holder.alert.setVisibility(View.INVISIBLE);
            }

        }

    }


    private void setAnimation(View viewToAnimate)
    {
        // If the bound view wasn't previously displayed on screen, it's animated


            Animation a = AnimationUtils.loadAnimation(context, R.anim.in_from_left);
            a.setInterpolator(context, android.R.interpolator.linear);
            a.setDuration(150);

            viewToAnimate.setAnimation(a);


    }

    public void setUpOptionsOpen(final MyViewHolder holder, final int position, boolean scrolled) {

        View view = holder.view;




        r1 = new RelativeLayout(context);
        r2 = new RelativeLayout(context);

        if(!scrolled) {
            ((CustomRecyclerViewLayoutManager)recyclerView.getLayoutManager()).scrollEnabled = false;
            int y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, context.getResources().getDisplayMetrics());

            r1.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)view.getY()));
            r2.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            r1.setX(0);
            r1.setY(0);
            r1.setBackgroundColor(Color.parseColor("#212121"));


            r2.setX(0);
            int num;
            if(holder.checkIfLast(position))
                num = 0;
            else
                num = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, context.getResources().getDisplayMetrics());
            r2.setY((int)view.getY() + view.getHeight() + y - num);
            r2.setBackgroundColor(Color.parseColor("#212121"));

            contactsLayout.addView(r1);
            contactsLayout.addView(r2);
            r1.setAlpha(0.3f);
            r2.setAlpha(0.3f);
            r1.bringToFront();
            r2.bringToFront();

            r1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeContactMenu(holder, holder.checkIfLast(position));
                }
            });

            r2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeContactMenu(holder, holder.checkIfLast(position));
                }
            });

        }
        else {
            int y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, context.getResources().getDisplayMetrics());

            r1.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, contactsLayout.getHeight() - y - view.getHeight()));
            r1.setX(0);
            r1.setY(0);
            r1.setBackgroundColor(Color.parseColor("#212121"));
            contactsLayout.addView(r1);
            r1.setAlpha(0.3f);
            r1.bringToFront();
            recyclerView.scrollToPosition(contactMenuPosition);
            ((CustomRecyclerViewLayoutManager)recyclerView.getLayoutManager()).scrollEnabled = false;

            r1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int num = contactMenuPosition;
                    contactMenuPosition = -1;
                    contactsLayout.removeView(r1);
                    r1.setOnClickListener(null);
                    if(!holder.checkIfLast(position)) {
                        View contactDivider = inflater.inflate(R.layout.contacts_divider, null, false);
                        optionsHolder.layout.addView(contactDivider);
                    }
                    holder.view.setEnabled(true);
                    notifyItemRemoved(num);
                    ((CustomRecyclerViewLayoutManager)recyclerView.getLayoutManager()).scrollEnabled = true;


                }
            });

        }

    }

    public void removeContactMenu(MyViewHolder holder, boolean isLast){
        int num = contactMenuPosition;
        contactMenuPosition = -1;
        contactsLayout.removeView(r1);
        contactsLayout.removeView(r2);
        r1.setOnClickListener(null);
        r2.setOnClickListener(null);
        if(!isLast) {
            View contactDivider = inflater.inflate(R.layout.contacts_divider, null, false);
            holder.layout.addView(contactDivider);
        }
        holder.view.setEnabled(true);
        notifyItemRemoved(num);
        ((CustomRecyclerViewLayoutManager)recyclerView.getLayoutManager()).scrollEnabled = true;


    }

    @Override
    public int getItemCount() {
        int num = 0;
        if (contactMenuPosition != -1) num = 1;
        return this.contacts.size() + num;
    }



    public void delete(int position) {

        String contactName = contacts.get(position).name;
        db.deleteUser(contactName);
        contacts.remove(position);
        if (contacts.size() == 1) {
            notifyDataSetChanged();
        } else {
            notifyItemRemoved(position);
        }
    }

    class ContactMenuViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout changeName;
        RelativeLayout delete;
        RelativeLayout cancel;

        public ContactMenuViewHolder(View v) {
            super(v);

            changeName = (RelativeLayout)v.findViewById(R.id.change_name_button);
            delete = (RelativeLayout)v.findViewById(R.id.delete_contact_button);
            cancel = (RelativeLayout)v.findViewById(R.id.cancel_button);

        }

        public void setDeleteClick(final int positionOfContact) {
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Delete Contact");
                    builder.setMessage("Are you sure you want to delete your contact? You will lose your chat history too.");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.delete(positionOfContact);
                            removeContactMenu(optionsHolder, optionsHolder.checkIfLast(positionOfContact));
                        }
                    });

                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    Dialog dialog = builder.create();
                    dialog.show();

                }
            });
        }

        public void setCancelClick(final int positionOfContact){
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeContactMenu(optionsHolder, optionsHolder.checkIfLast(positionOfContact));

                }
            });
        }

        public void setEditClick(int positionOfContact) {
            changeName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChangeNameDialogFragment fragment = new ChangeNameDialogFragment();
                    activity.getFragmentManager().beginTransaction().add(fragment, "change_name").commit();
                }
            });
        }


    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView image;
        TextView name;
        TextView alert;
        TextView hour;
        TextView lastMessage;
        View divider;
        LinearLayout layout;
        RelativeLayout layout2;
        View view;
        private MyViewHolder me;

        public MyViewHolder(View v){
            super(v);
            me = this;
            view = v;
            image = (ImageView) v.findViewById(R.id.contactImage);
            name = (TextView) v.findViewById(R.id.contactName);
            alert = (TextView)v.findViewById(R.id.alert);
            lastMessage = (TextView)v.findViewById(R.id.lastMessage);
            divider = v.findViewById(R.id.users_divider);
            layout = (LinearLayout)v.findViewById(R.id.main_contact_layout);
            layout2 = (RelativeLayout) v.findViewById(R.id.layout2);
            hour = (TextView) v.findViewById(R.id.contact_hour);
            viewLayouts.add(layout2);


            v.setOnClickListener(this);


        }

        public void setHour(long date) {
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTimeInMillis(date);



            if (System.currentTimeMillis() - date < TimeUnit.DAYS.toMillis(1)) {
                String minute = cal.get(Calendar.MINUTE) + "";
                if (minute.length() == 1) {
                    minute = "0" + minute;
                }
                hour.setText(cal.get(Calendar.HOUR_OF_DAY) + ":" + minute);
            }
            else {
                String[] months = {"January", "February", "March", "April", "May", "June", "July",
                        "August", "September", "October", "November", "December"};
                hour.setText( months[cal.get(Calendar.MONTH)] + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR));
            }


        }

        public void setTheLongClickListener(final int position) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {


                    contactMenuPosition = position + 1;

                    notifyDataSetChanged();

                    int y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, context.getResources().getDisplayMetrics());

                    boolean scrolled = false;

                    if ( (contactsLayout.getHeight() - view.getHeight() * (position + 1) - y) < 0 ) scrolled = true;

                    Log.i("hiiiiiiiiiii", "position1: " + position);

                    setUpOptionsOpen(me, position, scrolled);




                    return true;
                }
            });
        }



        public boolean checkIfLast(int position) {
            if(position == contacts.size()-1) {
                return true;
            }
            return false;
        }

        public void removeDivider(){
            if(layout.getChildCount() > 1) {
                layout.removeViewAt(layout.getChildCount() - 1);
            }

        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getPosition(), name.getText().toString());
        }
    }

}


interface MyListener {

 public void onItemClick(View view, int position, String name);
}

