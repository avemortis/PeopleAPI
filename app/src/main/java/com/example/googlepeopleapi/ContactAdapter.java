package com.example.googlepeopleapi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.services.people.v1.model.Person;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactHolder> {

    private List<Person> mContacts;
    private Context mContext;
    private FragmentManager mFragmentManager;

    public ContactAdapter(List<Person> personList, Context context, FragmentManager manager){
        mContacts = personList;
        mContext = context;
        mFragmentManager = manager;
    }

    @NonNull
    @NotNull
    @Override
    public ContactHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        return new ContactHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ContactHolder holder, int position) {
        Person person = mContacts.get(position);
        holder.bind(person, mFragmentManager);
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }
}
