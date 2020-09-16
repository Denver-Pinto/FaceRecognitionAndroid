package com.example.completeapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import java.util.Arrays;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
//import java.util.Base64;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class PopulateActivity extends AppCompatActivity implements View.OnClickListener {


    private Button takePictureButton;
    private Button choosePictureButton;
    private ImageView mainImage;
    private ImageView croppedface;
    private TextView populatetext;
    private Bitmap sourceBitmap;
    private Bitmap croppedBitmap;
    private Bitmap convertedBitmap;
    private Uri file;
    String currentPhotoPath;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_CHOICE = 2;
    String fname;
    private ImageView plusImageView, minusImageView;
    private TextView faceidTextView;
    private int maxFaces = -1;
    private int faceid = -1;
    private FirebaseFirestore db;
    SparseArray<Face> faces;
    Double[] faceID;
    RequestQueue requestQueue;  // This is our requests queue to process our HTTP requests.
    String baseUrl = "http://40.117.95.58:8501/v1/models/estimator_model:predict"; // This is the API base URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_populate);

        takePictureButton = (Button) findViewById(R.id.camerabutton);
        choosePictureButton = (Button) findViewById(R.id.gallerybutton);
        mainImage = (ImageView) findViewById(R.id.fullimage);
        croppedface = (ImageView) findViewById(R.id.croppedface);
        populatetext = (TextView) findViewById(R.id.populatetext);
        faceidTextView = findViewById(R.id.faceid);
        plusImageView = findViewById(R.id.plus);
        minusImageView = findViewById(R.id.minus);

        this.populatetext.setMovementMethod(new ScrollingMovementMethod());
        plusImageView.setOnClickListener(this);
        minusImageView.setOnClickListener(this);
        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.

        Log.v("Camera","1");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            takePictureButton.setEnabled(false);
            choosePictureButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE  }, 0);
        }
         db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.plus) {
            String threads = faceidTextView.getText().toString().trim();
            int numFaces = Integer.parseInt(threads);
            if (numFaces >=maxFaces-1) {
                return;
            }
            else {
                setNumThreads(++numFaces);
                clearPopulateText();
                faceidTextView.setText(String.valueOf(numFaces));
                faceProcessing(numFaces);
            }

        } else if (v.getId() == R.id.minus) {
            String threads = faceidTextView.getText().toString().trim();
            int numFaces = Integer.parseInt(threads);
            if (numFaces <= 0) {
                return;
            }
            else {
                setNumThreads(--numFaces);
                clearPopulateText();
                faceidTextView.setText(String.valueOf(numFaces));
                faceProcessing(numFaces);
            }
        }
    }

   public void onSubmit(View v) {
        String name = populatetext.getText().toString().trim();
        createToast(name);
        faceProcessing(Integer.parseInt(faceidTextView.getText().toString().trim()));

       Map<String, Object> docData = new HashMap<>();
       docData.put("name", name);
       docData.put("dateAdded", new Timestamp(new Date()));
       docData.put("faceid", Arrays.asList(faceID));

       db.collection("users").document(name)
               .set(docData)
               .addOnSuccessListener(new OnSuccessListener<Void>() {
                   @Override
                   public void onSuccess(Void aVoid) {
                       Log.d("TAG", "DocumentSnapshot successfully written!");
                   }
               })
               .addOnFailureListener(new OnFailureListener() {
                   @Override
                   public void onFailure(@NonNull Exception e) {
                       Log.w("TAG", "Error writing document", e);
                   }
               });


   }


    private void setNumThreads(int numThreads) {
        if (this.faceid != numThreads) {
            this.faceid = numThreads;
        }
    }

    private void createToast(String textmsg){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, textmsg, duration);
        toast.setGravity(Gravity.TOP| Gravity.LEFT, 250, 150);
        toast.show();
    }

    private void faceProcessing(int faceid) {
        //setPopulateText("Recognizing faces...");
        createToast("Creating FaceID");
        Face thisFace = faces.valueAt(faceid);
        float x1 = thisFace.getPosition().x;
        float y1 = thisFace.getPosition().y;

        int pad =30;
        croppedBitmap = Bitmap.createBitmap(sourceBitmap, Math.max((int)x1-pad,0), Math.max((int)y1-pad,0),(int)Math.min(thisFace.getWidth()+2*pad,sourceBitmap.getWidth()-(int)x1+pad), (int)Math.min(thisFace.getHeight()+2*pad,sourceBitmap.getHeight()-(int)y1+pad));
        croppedface.setImageBitmap(croppedBitmap);
        String s  = ConvertBitmapToString(croppedBitmap);
        JSONObject instance = new JSONObject();
        try {
            JSONObject b64 = new JSONObject();

            b64.put("b64",s);
            JSONObject bytes = new JSONObject();
            bytes.put("bytes", b64);
            JSONArray list = new JSONArray();
            list.put(bytes);

            instance.put("instances", list);
            Log.d("Instance",instance.toString());
        }catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest arrReq = new JsonObjectRequest
                (Request.Method.POST, baseUrl, instance, new Response.Listener<JSONObject>()  {
                    @Override
                    public void onResponse(JSONObject response) {
                        setPopulateTextResponse(response);
                    }
                },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // If there a HTTP error then add a note to our repo list.
                                //5setPopulateText("Error while calling REST API"); // needs to be a toast
                                createToast("Error while calling server");
                                Log.e("Volley", error.toString());
                            }
                        }
                );
        requestQueue.add(arrReq);// Add the request we just defined to our request queue.


    }

    private void setPopulateText(String str) {
        this.populatetext.setText(str);
    }

    private void setPopulateTextResponse(JSONObject response) {
        JSONArray prob,prob1;
        try {
            prob = response.getJSONArray("predictions");
            prob1 = prob.getJSONArray(0);

            Log.d("prob1 type",prob1.toString());
            //getTopKProbability(prob1);
            getFaceID(prob1);

        } catch (JSONException e) {
            Log.e("Volley", "Error processing JSON-setPopulateTextResponse."); // needs to be a toast
            createToast("Error processing server response");
        }
    }

    private void addPopulateText(String lastUpdated) {
        String currentText = this.populatetext.getText().toString();
        this.populatetext.setText(currentText + "\n" + lastUpdated);
    }

    private void clearPopulateText() {
        // This will clear (set it as a blank string).
        this.populatetext.setText("");
    }

    private void getFaceID(JSONArray prob) {
         faceID = new Double[128];
        try {
            for (int i = 0; i < 128; i++) {
                faceID[i] = (double)prob.get(i);
            }
        } catch (JSONException e) {
            Log.e("Volley", "Error processing JSON -getfaceid.");
            createToast("Error processing server response");

        }
        //addPopulateText(Arrays.toString(faceID));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                takePictureButton.setEnabled(true);
                choosePictureButton.setEnabled(true);
                Log.v("Camera","2");
            }
        }
    }

    public void takePicture(View view) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this,
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android22.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }

    public void choosePicture(View view) {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto , REQUEST_IMAGE_CHOICE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            File imgFile = new  File(currentPhotoPath);
            if(imgFile.exists())            {
                mainImage.setImageURI(Uri.fromFile(imgFile));
                sourceBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                /*comment the next section out if you want to show pictures from camera*/
                /*BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable=true;
                sourceBitmap = BitmapFactory.decodeResource(
                        getApplicationContext().getResources(),
                        R.drawable.abc,
                        options);


                /**/
                mainImage.setImageBitmap(sourceBitmap);
                faceDetection();
            }

        }
        if (requestCode == REQUEST_IMAGE_CHOICE && resultCode == RESULT_OK && data !=null) {
            Uri selectedImage =  data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            if (selectedImage != null) {
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    sourceBitmap = BitmapFactory.decodeFile(picturePath);
                    /*comment the next section out if you want to show pictures from gallery*/
                    /*
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable=true;
                    sourceBitmap = BitmapFactory.decodeResource(
                            getApplicationContext().getResources(),
                            R.drawable.abc,
                            options);
                    /**/
                    mainImage.setImageBitmap(sourceBitmap);
                    cursor.close();
                    faceDetection();
                }
            }
        }

    }

    public void faceDetection() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable=true;
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);
        Bitmap drawnBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(drawnBitmap);

        tempCanvas.drawBitmap(sourceBitmap, 0, 0, null);


        FaceDetector faceDetector = new
                FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                .build();
        if(!faceDetector.isOperational()){
            // new AlertDialog.Builder(v.getContext()).setMessage("Could not set up the face detector!").show();
            return;
        }
        Frame frame = new Frame.Builder().setBitmap(sourceBitmap).build();
        faces = faceDetector.detect(frame);


        maxFaces = faces.size();
        this.faceid = maxFaces>0?0:-1;
        faceidTextView.setText(String.valueOf(this.faceid));


        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;

            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            Log.d("value of x1 is ", String.valueOf((int)x1));
            Log.d("value of y1 is ", String.valueOf((int)y1));
            Log.d("value of x2 is ", String.valueOf((int)x2));
            Log.d("value of y2 is ", String.valueOf((int)y2));
            Log.d("value of bitmap height is ", String.valueOf(sourceBitmap.getHeight()));
            Log.d("value of bitmap width is ", String.valueOf(sourceBitmap.getWidth()));
            Log.d("value of faceheight is ", String.valueOf(thisFace.getHeight()));
            Log.d("value of face width is ", String.valueOf(thisFace.getWidth()));

            int pad =30;
            tempCanvas.drawRect(Math.max((int)x1-pad,0), Math.max((int)y1-pad,0),(int)Math.min((int)x2+pad,sourceBitmap.getWidth()), (int)Math.min((int)y2+pad,sourceBitmap.getHeight()), myRectPaint);

            croppedBitmap = Bitmap.createBitmap(sourceBitmap, Math.max((int)x1-pad,0), Math.max((int)y1-pad,0),(int)Math.min(thisFace.getWidth()+2*pad,sourceBitmap.getWidth()-(int)x1+pad), (int)Math.min(thisFace.getHeight()+2*pad,sourceBitmap.getHeight()-(int)y1+pad));
            //convertedBitmap = Bitmap.createBitmap(croppedBitmap.getWidth(), croppedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            String s  = ConvertBitmapToString(croppedBitmap);
            bitmap2file(croppedBitmap);
            //textView.setText(s);
            string2file(s);
            Log.d("value of CBx is ", String.valueOf(croppedBitmap.getWidth()));
            Log.d("value of CBy is ", String.valueOf(croppedBitmap.getHeight()));


        }
        mainImage.setImageBitmap(drawnBitmap);
        if(this.faceid!=-1)
        {
            faceProcessing(0);
        }
        faceDetector.release();


    }

    public  String ConvertBitmapToString(Bitmap bitmap){
        String encodedImage = "";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

        encodedImage= Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);


        return encodedImage;
    }

    public void bitmap2file(Bitmap bitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/req_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        fname = "Image-" + n;
        String filename = fname + ".jpg";
        File file = new File(myDir, filename);

        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void string2file(String sBody) {

        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, fname+".txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
