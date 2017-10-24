package com.example.admin.newsupdate;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.Date;


public class ApproveEvent extends AppCompatActivity {

    public static final int PICK_IMAGE = 100;
    private Intent eventimagedata = null;
    private boolean imageChangedforApprovedEvent = false;
    private EditText eventTitle;
    private ImageView eventImage;
    private EditText eventDescription;
    private Button submitEvent;
    private Button approveEvent;

    private String eventtitle;
    private String eventdescription;
    private String eventimageurl;
    private String eventsubmitter;

    private String key_to_remove_event;

    private NewEvent newEvent;

    private StorageReference mStorageRef;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private UploadTask uploadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_approve_event);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().hide();
        initialize_variable();

        eventImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });
        load_unapprovedevent();
        approveEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                approveevent();
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
                Glide.with(getApplicationContext()).load(data.getData()).fitCenter().into(eventImage);
                eventimagedata = (Intent) data.clone();
                imageChangedforApprovedEvent = true;

            } else {
                Toast.makeText(this, "Hey pick your image first",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }

    private void load_unapprovedevent() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("unapprovedevent");
        databaseReference.limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot newEventSnapshot : dataSnapshot.getChildren()) {
                    newEvent = newEventSnapshot.getValue(NewEvent.class);
                    key_to_remove_event = newEventSnapshot.getKey();
                }
                Glide.with(getApplicationContext()).load(newEvent.getImg_url()).fitCenter().into(eventImage);
                eventTitle.setText(newEvent.getTitle());
                eventDescription.setText(newEvent.getDescription());
                eventsubmitter = newEvent.getUser();
                eventimageurl = newEvent.getImg_url();
                //imagedata.setData(Uri.parse(an.getImg_url()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveevent() {
        if (eventTitle.getText() != null && eventDescription.getText() != null ) {

            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Confirm Approval of event ?");
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    eventtitle = eventTitle.getText().toString().trim();
                    eventdescription = eventDescription.getText().toString().trim();
                    Date date = new Date();
                    final ProgressDialog progressDialog = new ProgressDialog(ApproveEvent.this);
                    progressDialog.setMax(100);
                    progressDialog.setMessage("Uploading...");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                    progressDialog.setCancelable(false);
                    if (imageChangedforApprovedEvent) {
                        mStorageRef = FirebaseStorage.getInstance().getReference();
                        uploadTask = mStorageRef.child("approvedevent").child(date.toString()).putFile(eventimagedata.getData());
                        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                //sets and increments value of progressbar
                                progressDialog.incrementProgressBy((int) progress);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                                progressDialog.dismiss();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                Toast.makeText(getApplicationContext(), "Image Upload successful", Toast.LENGTH_SHORT).show();
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                newEvent = new NewEvent(eventdescription, downloadUrl.toString(), "All", eventtitle, eventsubmitter);
                                databaseReference = FirebaseDatabase.getInstance().getReference();
                                databaseReference.child("approvedevent").push().setValue(newEvent).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        databaseReference = FirebaseDatabase.getInstance().getReference();
                                        databaseReference.child("unapproved").child(key_to_remove_event).setValue(null);
                                        progressDialog.dismiss();
                                        finish();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                        progressDialog.dismiss();
                                    }
                                });
                            }
                        });
                    } else {
                        newEvent = new NewEvent(eventdescription, eventimageurl, "All", eventtitle, eventsubmitter);
                        databaseReference = FirebaseDatabase.getInstance().getReference();
                        databaseReference.child("approvedevent").push().setValue(newEvent).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                progressDialog.dismiss();
                                deleteevent();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        });
                    }
                }
            });
            adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            adb.show();
        } else
            Toast.makeText(getApplicationContext(), "Please Enter Title, Description and also upload Image", Toast.LENGTH_LONG).show();
    }

    private void deleteevent() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("unapprovedevent").child(key_to_remove_event).setValue(null).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplicationContext(), "Unapproved event Deleted", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void initialize_variable() {
        eventTitle = (EditText) findViewById(R.id.event_title);
        eventImage = (ImageView) findViewById(R.id.event_image);
        eventDescription = (EditText) findViewById(R.id.event_description);
        submitEvent = (Button) findViewById(R.id.submit_event);
        submitEvent.setVisibility(View.GONE);
        approveEvent = (Button) findViewById(R.id.approve_event);
        approveEvent.setVisibility(View.VISIBLE);
        mAuth = FirebaseAuth.getInstance();
        eventsubmitter = mAuth.getCurrentUser().getDisplayName();
    }
}