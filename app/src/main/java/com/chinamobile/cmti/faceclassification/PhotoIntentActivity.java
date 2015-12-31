package com.chinamobile.cmti.faceclassification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.chinamobile.faceClassification.server.FaceClassification;
import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PhotoIntentActivity extends AppCompatActivity {

    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final int ACTION_TAKE_PHOTO_S = 2;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private ImageView mImageView;
    private Bitmap mImageBitmap;
    private TextView mTextView;

    private String mCurrentPhotoPath;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private Bitmap thumbnail = null;

    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

    // Kairos related params
    private static final String app_id = "faaa8087";
    private static final String api_key = "0eee0b3a727cf973c499fbe1170ae230";
    private static final String galleryId = "employees";
    private static final String selector = "FULL";
    private static final String threshold = "0.75";
    private static final String minHeadScale = "0.25";
    private static final String maxNumResults = "2";
    private static final String TAG_ERRORS = "Errors";
    private static final String TAG_IMAGES = "images";
    private static final String TAG_TRANSACTION = "transaction";
    private static final String TAG_CANDIDATES = "candidates";
    private static final String TAG_SUBJECT = "subject";
    private static final String TAG_SUBJECTID = "subject_id";
    private static final String TAG_STATUS = "status";
    private static final String TAG_CONFIDENCE = "confidence";

    private KairosListener kairosListener = null;
    private Kairos myKairos = null;

    String[] employStrings = {"lisa", "charlie", "rui", "jian", "qingfeng"};
    HashSet<String> employees = new HashSet<>(Arrays.asList(employStrings));

    /* Photo album for this application */
    private String getAlbumName() {
        return getString(R.string.album_name);
    }


    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File setUpPhotoFile() throws IOException {

        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
//        thumbnail = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		/* Associate the Bitmap to the ImageView */
//        mImageView.setImageBitmap(thumbnail);
        mImageView.setImageBitmap(bitmap);
        mImageView.setVisibility(View.VISIBLE);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        switch (actionCode) {
            case ACTION_TAKE_PHOTO_B:
                File f = null;

                try {
                    f = setUpPhotoFile();
                    mCurrentPhotoPath = f.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                } catch (IOException e) {
                    e.printStackTrace();
                    f = null;
                    mCurrentPhotoPath = null;
                }
                break;

            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }

    private void handleSmallCameraPhoto(Intent intent) {
        Bundle extras = intent.getExtras();
        mImageBitmap = (Bitmap) extras.get("data");
//        FaceClassification faceClassification = FaceClassificationService.classifyImage("input.jpg", mImageBitmap);
        // classify the image just taken
        try {
            myKairos.recognize(mImageBitmap,
                    galleryId,
                    selector,
                    threshold,
                    minHeadScale,
                    maxNumResults,
                    kairosListener);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(View.VISIBLE);
//        mTextView.setText(faceClassification.toString());
//        mTextView.setVisibility(View.VISIBLE);
    }

    private void handleBigCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();
            thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(mCurrentPhotoPath), 32, 32);
//            FaceClassification faceClassification = FaceClassificationService.classifyImage("input.jpg", mImageBitmap);
//            mTextView.setText(faceClassification.toString());
//            mTextView.setVisibility(View.VISIBLE);
            mCurrentPhotoPath = null;
        }

    }

    Button.OnClickListener mTakePicSOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
                }
            };


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_intent);

        mImageView = (ImageView) findViewById(R.id.imageView1);
        mImageBitmap = null;

        Button picSBtn = (Button) findViewById(R.id.btnIntendS);
        setBtnListenerOrDisable(
                picSBtn,
                mTakePicSOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
        mTextView = (TextView) findViewById(R.id.textView);
        mTextView.setVisibility(View.INVISIBLE);

        try {
            // listener
            kairosListener = new KairosListener() {

                @Override
                public void onSuccess(String response) {
                    Log.d("Kairos response:", response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);

                        if (jsonObject.has(TAG_ERRORS)) {
                            // image error
                            // TODO: goes to webRTC session
                        } else {
                            JSONArray images = (JSONArray) jsonObject.getJSONArray(TAG_IMAGES);
                            if (((JSONObject) ((JSONObject) images.get(0)).get(TAG_TRANSACTION)).get(TAG_STATUS).equals("failure")) {
                                // no match
                                // TODO: goes to webRTC session
                            } else {
                                JSONObject transaction = (JSONObject) ((JSONObject) images.get(0)).get(TAG_TRANSACTION);
                                String name = transaction.has(TAG_SUBJECT) ? (String) transaction.get(TAG_SUBJECT) : (String) transaction.get(TAG_SUBJECTID);
                                int confidence = (int)(Double.parseDouble((String) transaction.get(TAG_CONFIDENCE)) * 100);
                                mTextView.setText("Photo matches with " + name + " with confidence: " + confidence + "%");

                                // post the photo to Kairos
                                myKairos.enroll(mImageBitmap,
                                        name,
                                        galleryId,
                                        selector,
                                        "false",
                                        minHeadScale,
                                        kairosListener);
                            }
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mTextView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFail(String response) {
                    Log.d("Kairos response:", response);
                }
            };


        /* * * instantiate a new kairos instance * * */
            myKairos = new Kairos();

        /* * * set authentication * * */
            myKairos.setAuthentication(this, app_id, api_key);

            // enroll coworkers photos once
//            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.charlie);
//            String selector = "FULL";
//            String multipleFaces = "false";
//            String minHeadScale = "0.25";
//            myKairos.enroll(bitmap,
//                    "charlie",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);
//
//            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rui);
//            myKairos.enroll(bitmap,
//                    "rui",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);
//
//            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.qingfeng);
//            myKairos.enroll(bitmap,
//                    "qingfeng",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);
//
//            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lisa);
//            myKairos.enroll(bitmap,
//                    "lisa",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);
//            mImageView.setImageBitmap(bitmap);
//            mImageView.setVisibility(View.VISIBLE);
            myKairos.listGalleries(kairosListener);

            //TODO: needs to add more photos of the individuals who might go to AT&T Hackathon
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {
                    handleBigCameraPhoto();
                }
                break;
            } // ACTION_TAKE_PHOTO_B

            case ACTION_TAKE_PHOTO_S: {
                if (resultCode == RESULT_OK) {
                    handleSmallCameraPhoto(data);
                }
                break;
            } // ACTION_TAKE_PHOTO_S
        } // switch
    }

    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(
                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );
        mTextView.setVisibility(View.INVISIBLE);
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void setBtnListenerOrDisable(
            Button btn,
            Button.OnClickListener onClickListener,
            String intentName
    ) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            btn.setText(
                    getText(R.string.cannot).toString() + " " + btn.getText());
            btn.setClickable(false);
        }
    }

}