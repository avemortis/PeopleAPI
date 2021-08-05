package com.example.googlepeopleapi;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.services.people.v1.model.Person;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

@SuppressLint("RestrictedApi")

public class ContactLookFragment extends DialogFragment implements PopupMenu.OnMenuItemClickListener {
    private static final String ARG_AVATAR = "avatar";
    private static final String ARG_NAME = "name";
    private static final String ARG_NUMBERS = "numbers";
    private static final String ARG_RESOURCE = "resource";

    public GoogleOauthHelper.Services mServices;

    public static final String TAG = "contact_look";

    private String mResourceName;

    DialogResult dialogResult;

    ImageView mAvatar;
    ImageView mPopUpMenu;
    TextView mName;
    RecyclerView mInfo;
    InfoAdapter mAdapter;

    public static ContactLookFragment newInstance(Person cnt){
        Bundle args = new Bundle();
        args.putString(ARG_RESOURCE, cnt.getResourceName());
        args.putString(ARG_AVATAR, cnt.getPhotos().get(0).getUrl());
        args.putString(ARG_NAME, cnt.getNames().get(0).getDisplayName());

        ArrayList<String> numbers = new ArrayList<>();
        if (cnt.getPhoneNumbers()!=null){
            for (int i = 0 ; i < cnt.getPhoneNumbers().size(); i++){
                numbers.add(cnt.getPhoneNumbers().get(i).getCanonicalForm());
            }
        }

        args.putStringArrayList(ARG_NUMBERS, numbers);
        ContactLookFragment fragment = new ContactLookFragment();

        //Log.d(TAG, numbers.get(0));

        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.contact_look, null);

        mAvatar = v.findViewById(R.id.contact_look_avatar);
        mPopUpMenu = v.findViewById(R.id.contact_look_popup);
        mName = v.findViewById(R.id.contact_look_name);
        mInfo = v.findViewById(R.id.contact_look_info_recyclerview);



        mResourceName = getArguments().getString(ARG_RESOURCE);
        String urlString = getArguments().getString(ARG_AVATAR);
        URL url = null;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e){
            e.printStackTrace();
        }

        mPopUpMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Deleted");
                showPopup(v);
            }
        });

        new SetAvatarRequest().execute(url);

        mName.setText(getArguments().getString(ARG_NAME));
        mInfo.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new InfoAdapter(getArguments().getStringArrayList(ARG_NUMBERS));
        mInfo.setAdapter(mAdapter);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity()).setView(v);
        dialog.setTitle(getString(R.string.contact_title));
        return dialog.create();
    }

    private void showPopup(View v){
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.contact_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete_contact_item:
                deleteContact();
                return true;
            default: return false;
        }
    }

    private void deleteContact() {
        if (dialogResult!=null){
            dialogResult.finish(mResourceName);
        }
        ContactLookFragment.this.dismiss();
    }


    public void setResult(DialogResult dialogResult) {
        this.dialogResult = dialogResult;
    }


    public interface DialogResult{
        void finish(String result);
    }

    static class InfoHolder extends RecyclerView.ViewHolder {

        TextView string;

        public InfoHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.contact_look_info_itemstring, parent, false));
            string = (TextView) itemView.findViewById(R.id.contact_look_infoitem_textview);
        }

        public void bind(String text){
            if (!text.isEmpty()){
                string.setText(text);
            }
        }
    }

    class InfoAdapter extends RecyclerView.Adapter<InfoHolder>{

        ArrayList<String> info;

        public InfoAdapter(ArrayList<String> phones){
            phones.add(0, getString(R.string.phone_numbers));
            info = phones;
        }

        @NonNull
        @NotNull
        @Override
        public InfoHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            return new InfoHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull InfoHolder holder, int position) {
            String string = info.get(position);
            holder.bind(string);
        }

        @Override
        public int getItemCount() {
            return info.size();
        }
    }

    class SetAvatarRequest extends AsyncTask<URL, Void, Bitmap> {

        Bitmap mBitmap = null;

        @Override
        protected Bitmap doInBackground(URL... urls) {
            try {
                mBitmap = BitmapFactory.decodeStream(urls[0].openConnection().getInputStream());
            } catch (IOException e){
                e.printStackTrace();
            }
            return mBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            super.onPostExecute(b);
            mAvatar.setImageBitmap(b);
        }
    }
}