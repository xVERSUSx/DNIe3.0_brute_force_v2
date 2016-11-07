package app.unizar.dnie30_brute_force.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DistortImage {

    private static final String IMAGE_PATH = "/sdcard/DNIe3_bf/firma.jpeg";

    public static void pixelateImage (int pixelation, Logger logger) {

        Bitmap originalBitmap = BitmapFactory.decodeFile(IMAGE_PATH);

        Bitmap bmOut = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getConfig());
        int pixelationAmount = pixelation; //you can change it!!
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        int avR,avB,avG; // store average of rgb
        int pixel;

        for(int x = 0; x < width; x+= pixelationAmount) { // do the whole image
            for(int y = 0; y < height; y+= pixelationAmount) {
                avR = 0; avG = 0; avB =0;

                int bx = x + pixelationAmount;
                int by = y + pixelationAmount;
                if(by >= height) by = height;
                if(bx >= width)bx = width;
                for(int xx =x; xx < bx;xx++){// YOU WILL WANT TO PUT SOME OUT OF BOUNDS CHECKING HERE
                    for(int yy= y; yy < by;yy++){ // this is scanning the colors

                        pixel = originalBitmap.getPixel(xx, yy);
                        avR += (int) (Color.red(pixel));
                        avG+= (int) (Color.green(pixel));
                        avB += (int) (Color.blue(pixel));
                    }
                }
                avR/= pixelationAmount^2; //divide all by the amount of samples taken to get an average
                avG/= pixelationAmount^2;
                avB/= pixelationAmount^2;

                for(int xx =x; xx < bx;xx++)// YOU WILL WANT TO PUYT SOME OUT OF BOUNDS CHECKING HERE
                    for(int yy= y; yy <by;yy++){ // this is going back over the block
                        bmOut.setPixel(xx, yy, Color.argb(255, avR, avG,avB)); //sets the block to the average color
                    }

            }

        }


        //Save image again
        try {
            FileOutputStream fos = new FileOutputStream(new File(IMAGE_PATH));
            bmOut.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        }catch (IOException e) {
            logger.log(Level.ALL, "Exception in photoCallback", e);
            e.printStackTrace();
        }

    }

}
