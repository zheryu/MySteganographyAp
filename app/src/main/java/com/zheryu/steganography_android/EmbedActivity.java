package com.zheryu.steganography_android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.zheryu.steganography_android.ImageUtils.bitmapToByteArray;
import static com.zheryu.steganography_android.ImageUtils.byteArrToIntArr;
import static com.zheryu.steganography_android.ImageUtils.getScaledBitmap;
import static com.zheryu.steganography_android.ImageUtils.getUprightBitmap;
import static com.zheryu.steganography_android.ImageUtils.intArrToByteArr;

public class EmbedActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_DIMENSION = 768;


    //It's 2am. I'll implement character count later
    private Bitmap selectedBitmap = null;
    private Uri selectedUri = null;
    //private byte[] imageBytes = null;
    //private byte[] messageByteCount = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_embed);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_embed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent){
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode){
            case PICK_IMAGE_REQUEST:
                if(resultCode == RESULT_OK && imageReturnedIntent != null && imageReturnedIntent.getData() != null) {
                    selectedUri = imageReturnedIntent.getData();
                    ImageView iv = (ImageView) findViewById(R.id.inputImg);

                    try {
                        selectedBitmap = getUprightBitmap(this, selectedUri);

                        Bitmap scaledBitmap = getScaledBitmap(selectedBitmap, MAX_IMAGE_DIMENSION);

                        //hack to get the imageview to the right dimensions
                        iv.setMaxHeight(scaledBitmap.getHeight());
                        iv.setMinimumHeight(scaledBitmap.getHeight());
                        iv.setMaxWidth(scaledBitmap.getWidth());
                        iv.setMinimumWidth(scaledBitmap.getWidth());
                        iv.setImageBitmap(scaledBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    //action when pushing the imageButton in the view
    public void selectImageAction(View v){
        startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "Choose an image"), PICK_IMAGE_REQUEST);
    }

    public void embedAction(View v){
        if(selectedBitmap == null || selectedUri == null) return;
        Bitmap embeddedBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);
        //append the null character so we know when to stop.
        byte[] messageBytes = (((EditText)findViewById(R.id.messageInput)).getText().toString()+"\u0000").getBytes();
        //get the bytes. Store in imageBytes.
        int[] embeddedPixels = new int[embeddedBitmap.getWidth() * embeddedBitmap.getHeight()];

        embeddedBitmap.getPixels(embeddedPixels, 0, embeddedBitmap.getWidth(), 0, 0, embeddedBitmap.getWidth(), embeddedBitmap.getHeight());
        for(int i = 0; i < 4; i++)Log.d("1st", Integer.toBinaryString(embeddedBitmap.getPixel(0,0)));

        for(int i = 0; i < 4; i++)Log.d("1st", Integer.toBinaryString(embeddedPixels[i]));
        byte[] imageBytes = intArrToByteArr(embeddedPixels);

        embedBytes(imageBytes, messageBytes);
        for(int i = 0; i < 4; i++)Log.d("1st", Integer.toBinaryString(imageBytes[i]));

        embeddedPixels = byteArrToIntArr(imageBytes);
        for(int i = 0; i < 4; i++)Log.d("2nd", Integer.toBinaryString(embeddedPixels[i]));
        embeddedBitmap.setPixels(embeddedPixels, 0, embeddedBitmap.getWidth(), 0, 0, embeddedBitmap.getWidth(), embeddedBitmap.getHeight());
        int[] again = new int[embeddedBitmap.getWidth() * embeddedBitmap.getHeight()];
        embeddedBitmap.getPixels(again, 0, embeddedBitmap.getWidth(), 0, 0, embeddedBitmap.getWidth(), embeddedBitmap.getHeight());
        for(int i = 0; i < 4; i++)Log.d("again", Integer.toBinaryString(again[i]));

        saveBmpAsPNG(embeddedBitmap);
    }

    public void saveBmpAsPNG(Bitmap bmp){
        if(bmp == null)Log.d("Save", "no image");
        File imageRoot = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MySteganography");
        if(!imageRoot.isDirectory()) {
            imageRoot.mkdirs();
        }
        String originalUri = selectedUri.getPath();
        String finalFileName = originalUri.substring(originalUri.lastIndexOf('/') + 1);
        int dotIndex = finalFileName.lastIndexOf(".");
        if(dotIndex != -1){
            finalFileName = finalFileName.substring(0, dotIndex);
        }
        finalFileName += ".png";
        File image = new File(imageRoot, finalFileName);
        try{
            image.createNewFile();
            FileOutputStream fos = new FileOutputStream(image);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    //skips over alpha bits
    public void embedBytes(byte[] wrapperBytes, byte[] inBytes){
        int concentration = 2;  //implement quality later.
        if(     concentration < 1 ||
                concentration > 8 ||
                (concentration & (concentration - 1)) != 0 ||
                inBytes.length*8 > wrapperBytes.length*concentration){
            return;
        }
        if(inBytes.length == 0)return;

        int encodeBits = (1 << concentration) - 1;
        int eraseBits = 255 ^ encodeBits;
        int bytesPerInByte = 8 / concentration;
        /*for(int i = 0; i < inBytes.length; i++) {
            Log.d("string", Integer.toBinaryString(inBytes[i]));
            for (int j = 0; j < bytesPerInByte; j++){
                int index = (i*bytesPerInByte) + j;
                Log.d("pic", Integer.toBinaryString(wrapperBytes[index]));
                wrapperBytes[index] = (byte) ((wrapperBytes[index] & eraseBits) | (inBytes[i] >> (8 - (concentration * (j + 1))) & encodeBits));
                Log.d("pic", Integer.toBinaryString(wrapperBytes[index]));
            }
        }*/
        int k = 0;
        for(int i = 0; i < inBytes.length; i++) {
            int j = 0;
            Log.d("sting", Integer.toBinaryString(inBytes[i]));

            while(j < bytesPerInByte){
                Log.d("check", Integer.toBinaryString(wrapperBytes[k]));
                if(k % 4 != 0){
                    wrapperBytes[k] = (byte) ((wrapperBytes[k] & eraseBits) | (inBytes[i] >> (8 - (concentration * (j + 1))) & encodeBits));
                    j++;
                }
                Log.d("check", Integer.toBinaryString(wrapperBytes[k]));

                k++;
            }
        }
    }
}
