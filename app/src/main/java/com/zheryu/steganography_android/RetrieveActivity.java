package com.zheryu.steganography_android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;

import static com.zheryu.steganography_android.ImageUtils.byteArrToIntArr;
import static com.zheryu.steganography_android.ImageUtils.getScaledBitmap;
import static com.zheryu.steganography_android.ImageUtils.getUprightBitmap;
import static com.zheryu.steganography_android.ImageUtils.intArrToByteArr;

public class RetrieveActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_DIMENSION = 768;


    private Bitmap selectedBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retrieve);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_retrieve, menu);
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
                    Uri selectedUri = imageReturnedIntent.getData();
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

    public void retrieveAction(View v){
        if(selectedBitmap == null) return;
        int[] embeddedPixels = new int[selectedBitmap.getWidth() * selectedBitmap.getHeight()];
        selectedBitmap.getPixels(embeddedPixels, 0, selectedBitmap.getWidth(), 0, 0, selectedBitmap.getWidth(), selectedBitmap.getHeight());
        byte[] imageBytes = intArrToByteArr(embeddedPixels);
        for(int i = 0; i < 24; i++)Log.d("retrieved", Integer.toBinaryString(imageBytes[i]));

        byte[] extract = new byte[imageBytes.length * 3 / 4];
        byte curByte = 0x00;
        int i = 0; // index of extract
        int k = 0; // index of imageBytes
        do{
            curByte = 0x00;
            int j = 6; // how much to scoot bits by.
            while(j >= 0){
                Log.d("pop", Integer.toBinaryString(imageBytes[k]));
                if(k % 4 != 0){
                    curByte = (byte) (curByte | ((imageBytes[k] & 0x3) << j));
                    Log.d("processing", Integer.toBinaryString(curByte));
                    j -= 2;
                }

                k++;
            }
            extract[i] = curByte;
            Log.d("retrieved", Integer.toBinaryString(extract[i]));
            i++;
        }while(curByte != 0x00 && i < 64);//imageBytes.length);
        byte[] smallerArray = new byte[i-1];
        for(int j = 0; j < smallerArray.length; j++) smallerArray[j] = extract[j];
        Log.d("result", new String(smallerArray));
    }
}
