package com.example.googlepeopleapi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AddContactFragment extends DialogFragment {

    public static final int RESULT_OK = 5001;

    DialogResult mDialogResult;

    private String name;

    EditText mFirstName;
    EditText mLastName;
    EditText mPhones1;
    EditText mPhones2;
    EditText mPhones3;

/*    @SuppressLint("ValidFragment")
    public AddContactFragment(Context context, String name){
        super();
        this.name = name;
    }*/

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.add_contact, null);

        mFirstName = v.findViewById(R.id.add_contact_firstname_edittext);
        mLastName = v.findViewById(R.id.add_contact_lastname_edittext);
        mPhones1 = v.findViewById(R.id.add_contact_phones_edittext);
        mPhones2 = v.findViewById(R.id.add_contact_phones_edittext2);
        mPhones3 = v.findViewById(R.id.add_contact_phones_edittext3);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity()).setView(v);
        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mDialogResult!=null){
                    ArrayList<String> result = new ArrayList<>();
                    result.add(mFirstName.getText().toString());
                    result.add(mLastName.getText().toString());
                    result.add(mPhones1.getText().toString());
                    result.add(mPhones2.getText().toString());
                    result.add(mPhones3.getText().toString());
                    mDialogResult.finish(result);
                }
                AddContactFragment.this.dismiss();
            }
        });
        dialog.setTitle(getString(R.string.add_contact));
        return dialog.create();
    }

    void setResult(DialogResult dialogResult){
        mDialogResult = dialogResult;
    }

    public interface DialogResult{
        void finish(ArrayList<String> result);
    }
}
