package com.example.googlepeopleapi;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Google API";
    private static final int RC_SIGN_IN = 5009;
    private static final int RC_RECOVERABLE = 5010;

    private static final String CONTACTS_SCOPE = "https://www.googleapis.com/auth/contacts";
    private static final String KEY_ACCOUNT = "key_account";

    private GoogleSignInClient mSignInClient;

    public String mServerAuthCode;
    private Account mAccount;

    public GoogleOauthHelper.Services mServices;

    ImageView mAddContactImageView;
    RecyclerView mContactsRecyclerView;
    ContactAdapter mContactAdapter;

    List<Person> mContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (savedInstanceState != null) {
            mAccount = savedInstanceState.getParcelable(KEY_ACCOUNT);
        }

        validateServerClientID();
        GoogleSignInOptions mGSO = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(
                        new Scope(PeopleServiceScopes.CONTACTS_READONLY),
                        new Scope(PeopleServiceScopes.USERINFO_PROFILE),
                        new Scope(PeopleServiceScopes.USER_EMAILS_READ),
                        new Scope(PeopleServiceScopes.CONTACTS),
                        new Scope(PeopleServiceScopes.CONTACTS_OTHER_READONLY))
                .requestServerAuthCode(getString(R.string.GOOGLE_CLIENT_ID))
                .requestEmail()
                .build();
        mSignInClient = GoogleSignIn.getClient(this, mGSO);

        mAddContactImageView = findViewById(R.id.add_new_contact_button);
        mAddContactImageView.setOnClickListener(this);
        mContactsRecyclerView = findViewById(R.id.contacts_list_recyclerview);
        mContactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (GoogleSignIn.hasPermissions(account, new Scope(CONTACTS_SCOPE))) {
            signIn();
            //updateUI(account);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if the user is already signed in and all required scopes are granted
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (GoogleSignIn.hasPermissions(account, new Scope(CONTACTS_SCOPE))) {
            //updateUI(account);
        } else {
            //updateUI(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.add_new_contact_button:
                addContact();
                break;

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.sign_in_item:
                signIn();
                return true;
            case R.id.sign_out_item:
                signOut();
                return true;
            case R.id.info_item:
                revokeAccess();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void signIn() {
        Intent signInIntent = mSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        Log.d(TAG, "sign in");
    }

    private void signOut() {
        mSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //updateUI(null);
            }
        });
    }

    private void revokeAccess() {
        mSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //updateUI(null);
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ACCOUNT, mAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            mServerAuthCode = account.getServerAuthCode();

            mAccount = account.getAccount();
            getContacts();
            //updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.

            mAccount = null;

            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            //updateUI(null);
        }
    }

    private void validateServerClientID() {
        String serverClientId = this.getString(R.string.GOOGLE_CLIENT_ID);
        String suffix = ".apps.googleusercontent.com";
        if (!serverClientId.trim().endsWith(suffix)) {
            String message = "Invalid server client ID in strings.xml, must end with " + suffix;

            Log.w(TAG, message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void updateUI(){
/*        for (int i = 0; i < mContacts.size(); i++) {
            Log.d(TAG, mContacts.get(i).getNames().get(0).getDisplayName());
            Log.d(TAG, mContacts.get(i).getPhoneNumbers().get(0).getCanonicalForm());
        }*/
        mContactAdapter = new ContactAdapter(mContacts);
        mContactsRecyclerView.setAdapter(mContactAdapter);
    }

    private void getContacts() {
        if (mAccount == null) {
            Log.w(TAG, "getContacts: null account");
            return;
        }

        if (mServerAuthCode == null) {
            Log.w(TAG, "getContacts: null server auth code");
            return;
        }

        new GetContactsTask(this).execute(mAccount);
    }

    private void addContact(){
        AddContactFragment dialog = new AddContactFragment();
        dialog.setResult(new AddContactFragment.DialogResult() {
            @Override
            public void finish(ArrayList<String> result) {
                Person contactToCreate = new Person();
                ArrayList <Name> names = new ArrayList<>();
                ArrayList <PhoneNumber> phones = new ArrayList<>();
                if (result.get(0).isEmpty()){
                    Toast.makeText(getApplicationContext(), getString(R.string.contact_name_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                names.add(new Name().setGivenName(result.get(0)).setFamilyName(result.get(1)));
                for (int i = 2; i < result.size(); i++){
                    if (!result.get(i).isEmpty()){
                        phones.add(new PhoneNumber().setValue(result.get(i)));
                    }
                }
                contactToCreate.setNames(names);
                contactToCreate.setPhoneNumbers(phones);

                new AddContactTask().execute(contactToCreate);
                getContacts();
                updateUI();
            }
        });
        dialog.show(getFragmentManager(), TAG);
    }

    private void deleteContact(String resourceName){
    }

    protected void onConnectionsLoadFinished(@Nullable List<Person> connections) {

        if (connections == null) {
            Log.d(TAG, "getContacts:connections: null");
            //mDetailTextView.setText(getString(R.string.connections_fmt, "None"));
            return;
        }

        Log.d(TAG, "getContacts:connections: size=" + connections.size());

        mContacts = connections;
        updateUI();

       /* // Get names of all connections
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < connections.size(); i++) {
            Person person = connections.get(i);
            if (person.getNames() != null && person.getNames().size() > 0) {
                msg.append(person.getNames().get(0).getDisplayName());

                if (i < connections.size() - 1) {
                    msg.append(",");
                }
            }
        }*/

        // Display names
        //mDetailTextView.setText(getString(R.string.connections_fmt, msg.toString()));
    }

    protected void onRecoverableAuthException(UserRecoverableAuthIOException recoverableException) {
        Log.w(TAG, "onRecoverableAuthException", recoverableException);
        startActivityForResult(recoverableException.getIntent(), RC_RECOVERABLE);
    }

    private static class GetContactsTask extends AsyncTask<Account, Void, List<Person>> {

        private final WeakReference<MainActivity> mActivityRef;

        public GetContactsTask(MainActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<Person> doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }

            String mServerAuthCode = mActivityRef.get().mServerAuthCode;
            if (TextUtils.isEmpty(mServerAuthCode)) {
                return null;
            }

            Context context = mActivityRef.get().getApplicationContext();
            try {

                GoogleOauthHelper.Services services;
                if (mActivityRef.get().mServices == null) {
                    Log.d("Applog", "Getting new services");
                    services = GoogleOauthHelper.INSTANCE.setUp(context, mServerAuthCode);
                    mActivityRef.get().mServices = services;
                } else {
                    Log.d("Applog", "Using old services");
                    services = mActivityRef.get().mServices;
                }
                ListConnectionsResponse connectionsResponse = services
                        .getPeopleService()
                        .connections()
                        .list("people/me")
                        .setRequestMaskIncludeField("person.names,person.phoneNumbers,person.emailAddresses,person.photos")
                        .execute();

                return connectionsResponse.getConnections();

            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "getContacts:exception", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Person> people) {
            super.onPostExecute(people);
            if (mActivityRef.get() != null) {
                mActivityRef.get().onConnectionsLoadFinished(people);
            }
        }
    }

    private class AddContactTask extends AsyncTask<Person, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Person... people) {
            try {
                mServices.getPeopleService().createContact(people.clone()[0]).execute();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private class DeleteContactTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            for (String s : strings){
                try {
                    mServices.getPeopleService().deleteContact(s).execute();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    public class ContactAdapter extends RecyclerView.Adapter<ContactHolder> {

        private List<Person> mContacts;

        public ContactAdapter(List<Person> personList){
            mContacts = personList;
        }

        @NonNull
        @NotNull
        @Override
        public ContactHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            return new ContactHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull ContactHolder holder, int position) {
            Person person = mContacts.get(position);
            holder.bind(person);
        }

        @Override
        public int getItemCount() {
            return mContacts.size();
        }
    }

    public class ContactHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private static final String TAG = "holder";

        Person contact;

        ImageView avatar = null;
        TextView name;
        TextView number;


        public ContactHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.contact_item, parent, false));
            itemView.setOnClickListener(this);
            avatar = (ImageView) itemView.findViewById(R.id.contact_avatar_imageview);
            name = (TextView) itemView.findViewById(R.id.contact_name_textview);
            number = (TextView) itemView.findViewById(R.id.contact_phone_number_textview);
        }

        public void bind(Person person){
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
            dialog.setResult(new ContactLookFragment.DialogResult() {
                                 @Override
                                 public void finish(String result) {
                                     new DeleteContactTask().execute(result);
                                     getContacts();
                                     updateUI();
                                 }
                             });
            dialog.show(getSupportFragmentManager(), TAG);

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

}