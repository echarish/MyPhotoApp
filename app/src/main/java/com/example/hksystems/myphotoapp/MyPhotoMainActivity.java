package com.example.hksystems.myphotoapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyPhotoMainActivity extends ActionBarActivity {

    private static final String TAG = "com.example.hksystems";
    private static final String MY_PHOTO_APP_NAME = "My-Phot-App-";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_SELECT = 2;

    ImageView myPhotoAppImageView;
    Bitmap originalClickedPicture;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_photo_main);

        Button takePhoto = (Button) findViewById(R.id.takePhotoButton);
        myPhotoAppImageView = (ImageView) findViewById(R.id.myPhotoAppImageView);

        if (!hasCamera()) {
            takePhoto.setEnabled(false);
        }
    }

    private boolean hasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    public void launchCamera(View view) {
        Intent cameraLaunchIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraLaunchIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult requestCode-" + requestCode + " , resultCode-" + resultCode);
        Bitmap imageForView = null;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageForView = (Bitmap) extras.get("data");
            myPhotoAppImageView.setImageBitmap(imageForView);
        } else
            if (requestCode == REQUEST_IMAGE_SELECT && resultCode == RESULT_OK) {
                doGalleryPhotoSelect(data.getData());
            }

    }

    public void doGalleryPhotoSelect(final Uri data){
        Runnable photoSelectRunnable=new Runnable() {
            @Override public void run() {
                Uri selectedImage = data;
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap bmp = null;
                try {
                    bmp = getBitmapFromUri(selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Message message=Message.obtain();
                message.obj=bmp;
                myPhotoAppImageViewHandler.sendMessage(message);
            }
        };
        Thread photoSelectThread=new Thread(photoSelectRunnable);
        photoSelectThread.start();
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        Log.d(TAG,"URI "+uri);
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my_photo_main, menu);
        return true;
    }

    public Bitmap getBitMapFromImageView() {
        if (myPhotoAppImageView.getDrawable() instanceof BitmapDrawable) {
            originalClickedPicture = ((BitmapDrawable) myPhotoAppImageView.getDrawable()).getBitmap();
        } else
            if (myPhotoAppImageView.getDrawable() instanceof LayerDrawable) {
                int width = myPhotoAppImageView.getDrawable().getIntrinsicWidth();
                int height = myPhotoAppImageView.getDrawable().getIntrinsicHeight();
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                myPhotoAppImageView.getDrawable().setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                myPhotoAppImageView.getDrawable().draw(canvas);
                originalClickedPicture = bitmap;
            }


        return originalClickedPicture;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
        case R.id.overlay_photo:
            doPhotoOverlay(getBitMapFromImageView());
            return true;
        case R.id.invert_photo:
            doPhotoInvert(getBitMapFromImageView());
            return true;
        case R.id.save_photo:
            doPhotoSave(getBitMapFromImageView(), "", "");
            return true;
        case R.id.undo_photo_effects:
            myPhotoAppImageView.setImageBitmap(originalClickedPicture);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doPhotoSave(Bitmap photoToBeSaved, String photoTitle, String photoDescription) {
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        Log.d(TAG, "Current date time in save" + currentDateTime);
        if (photoTitle.isEmpty()) {
            photoTitle = MY_PHOTO_APP_NAME + currentDateTime;
        }
        if (photoDescription.isEmpty()) {
            photoDescription = MY_PHOTO_APP_NAME + currentDateTime;
        }
        fixMediaDir();
        MediaStore.Images.Media.insertImage(MyPhotoMainActivity.this.getContentResolver(), photoToBeSaved, photoTitle, photoDescription);
    }

    void fixMediaDir() {
        File sdcard = Environment.getExternalStorageDirectory();
        Log.d(TAG,"SDCARD "+sdcard);
        if (sdcard != null) {
            File mediaDir = new File(sdcard, "//media/external/images/media");
            if (!mediaDir.exists()) {
                Log.d(TAG,"DIR NOT EXIST creating now ");
                mediaDir.mkdirs();
            }
        }
    }


    private void doPhotoOverlay(Bitmap original) {
        Drawable[] layers = new Drawable[2];
        layers[0] = new BitmapDrawable(getResources(), original);
        layers[1] = getResources().getDrawable(R.drawable.dirty);
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        myPhotoAppImageView.setImageDrawable(layerDrawable);
    }


    Handler myPhotoAppImageViewHandler =new Handler(){

        @Override public void handleMessage(Message msg) {
            myPhotoAppImageView.setImageBitmap((Bitmap)msg.obj);
        }
    };

    private void doPhotoInvert(final Bitmap original) {
        Runnable invertImageRunnable=new Runnable() {
            @Override public void run() {
                Bitmap finalImage = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());
                int A, R, G, B;
                int pixelColor;
                int height = original.getHeight();
                int width = original.getWidth();

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        pixelColor = original.getPixel(x, y);
                        A = Color.alpha(pixelColor);
                        R = 255 - Color.red(pixelColor);
                        G = 255 - Color.green(pixelColor);
                        B = 255 - Color.blue(pixelColor);
                        finalImage.setPixel(x, y, Color.argb(A, R, G, B));
                    }
                }
                Message invertImageMessage=Message.obtain();
                invertImageMessage.obj=finalImage;
                myPhotoAppImageViewHandler.sendMessage(invertImageMessage);
            }
        };
        Thread invertImageThread=new Thread(invertImageRunnable);
        invertImageThread.start();

        Toast.makeText(this,"Started to make your Image Invert...",Toast.LENGTH_LONG).show();

    }

    public void chooseFromGallery(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_SELECT);

    }
}
