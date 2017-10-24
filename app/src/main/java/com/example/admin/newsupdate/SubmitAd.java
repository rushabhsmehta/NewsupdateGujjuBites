package com.example.admin.newsupdate;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.Date;

/**
 * Created by admin on 10/21/2017.
 */

public class SubmitAd extends AppCompatActivity {
    public static final int PICK_IMAGE = 100;
    private Intent adimagedata = null;
    private boolean imageChangedforApprovedNews = false;
    private EditText adTitle;
    private ImageView adImage;
    private EditText adDescription;
    private Button next;
    private FloatingActionButton fb;
    private String adtitle;
    private String addescription;
    private String adimageurl;
    private String adsubmitter;
    private LinearLayout linearLayout;
    private String key_to_remove_ad;

    private NewAd newAd;

    private StorageReference mStorageRef;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private UploadTask uploadTask;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_approve_ad);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().hide();
        initialize_variable();

        adImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adImage.setVisibility(View.VISIBLE);
                adTitle.setVisibility(View.GONE);
                adDescription.setVisibility(View.GONE);
                fb.setVisibility(View.VISIBLE);
                next.setVisibility(View.GONE);
                linearLayout.setVisibility(View.GONE);
            }
        });
        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                submitadUnApproved();
            }
        });

    }

    private void submitadUnApproved() {

        if (adTitle.getText().toString() != null && adDescription.getText().toString() != null &&
                adimagedata != null) {

            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Confirm Submission of ad ?");
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    adtitle = adTitle.getText().toString().trim();
                    addescription = adDescription.getText().toString().trim();


                    Date date = new Date();
                    final ProgressDialog progressDialog = new ProgressDialog(SubmitAd.this);
                    progressDialog.setMax(100);
                    progressDialog.setMessage("Uploading...");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                    progressDialog.setCancelable(false);
                    mStorageRef = FirebaseStorage.getInstance().getReference();
                    uploadTask = mStorageRef.child("unapprovedad").child(date.toString()).putFile(adimagedata.getData());
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
                            newAd = new NewAd(addescription, downloadUrl.toString(), "All", adtitle, adsubmitter);
                            databaseReference = FirebaseDatabase.getInstance().getReference().child("unapprovedad");
                            databaseReference.push().setValue(newAd).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getApplicationContext(), "ad Submitted", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                }
                            });
                        }
                    });
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

    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
                Glide.with(getApplicationContext()).load(data.getData()).fitCenter().into(adImage);
                adimagedata = (Intent) data.clone();
                imageChangedforApprovedNews = true;

            } else {
                Toast.makeText(this, "Hey pick your image first",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }
    private void initialize_variable() {
        adTitle = (EditText) findViewById(R.id.ad_title);
        adImage = (ImageView) findViewById(R.id.ad_image);
        adDescription = (EditText) findViewById(R.id.ad_description);
        next  = (Button) findViewById(R.id.next);
        fb = (FloatingActionButton) findViewById(R.id.floatingActionButtonAd);
        fb.setVisibility(View.GONE);
        linearLayout = (LinearLayout) findViewById(R.id.LinearLayoutAd);
        mAuth = FirebaseAuth.getInstance();
        adsubmitter = mAuth.getCurrentUser().getDisplayName();
    }
}