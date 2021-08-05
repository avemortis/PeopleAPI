package com.example.googlepeopleapi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.services.people.v1.model.Person;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class ContactHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private static final String TAG = "holder";

    Person contact;

    ImageView avatar = null;
    TextView name;
    TextView number;


    FragmentManager mFragmentManager;

    public ContactHolder(LayoutInflater inflater, ViewGroup parent) {
        super(inflater.inflate(R.layout.contact_item, parent, false));
        itemView.setOnClickListener(this);
        avatar = (ImageView) itemView.findViewById(R.id.contact_avatar_imageview);
        name = (TextView) itemView.findViewById(R.id.contact_name_textview);
        number = (TextView) itemView.findViewById(R.id.contact_phone_number_textview);
    }

    public void bind(Person person, FragmentManager manager){
        mFragmentManager = manager;
        contact = person;
        String urlString = null;
        URL url = null;

        if (person.getPhotos()!=null){
            urlString = person.getPhotos().get(0).getUrl();
        }

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e){
            e.printStackTrace();
        }

        new AsyncRequest().execute(url);

        name.setText(person.getNames().get(0).getDisplayName());
        if (person.getPhoneNumbers()!=null){
            number.setText(person.getPhoneNumbers().get(0).getCanonicalForm());
        }
    }

    @Override
    public void onClick(View v) {
        ContactLookFragment dialog = ContactLookFragment.newInstance(contact);
        dialog.show(mFragmentManager, "tag");
    }

    class AsyncRequest extends AsyncTask<URL, Void, Bitmap> {

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
            avatar.setImageBitmap(b);
        }
    }
}