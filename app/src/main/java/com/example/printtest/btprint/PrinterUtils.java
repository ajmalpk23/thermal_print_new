package com.example.printtest.btprint;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.icu.lang.UCharacter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.example.printtest.POSUtils;
import com.example.printtest.SuperActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class PrinterUtils extends SuperActivity implements Runnable, PrinterCommands {
    private final String FULL_LINE_FOR_PRINT = "---------------------------------------------";
    protected static final String TAG = "TAG";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    Button mScan, mPrint, mDisc;
    private static PrinterUtils _self;
    BluetoothAdapter mBluetoothAdapter;
    private UUID applicationUUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ProgressDialog mBluetoothConnectProgressDialog;
    //    private BluetoothSocket mBluetoothSocket;
    BluetoothDevice mBluetoothDevice;
    private String imageString;
    private Bitmap bitmap;

    byte FONT_TYPE;
    private static BluetoothSocket btsocket;
    private static OutputStream outputStream;

    private final static char ESC_CHAR = 0x1B;
    private final static byte[] FEED_LINE = {10};
    public static byte[] SELECT_BIT_IMAGE_MODE = {0x1B, 0x2A, 33, (byte) 255, 3};
    private final static byte[] SET_LINE_SPACE_24 = new byte[]{ESC_CHAR, 0x33, 24};
    private final static byte[] SET_LINE_SPACE_30 = new byte[]{ESC_CHAR, 0x33, 30};


    private ArrayList<Bitmap> bs = new ArrayList<Bitmap>();

    @Override
    public void onCreate(Bundle mSavedInstanceState) {
        super.onCreate(mSavedInstanceState);
    }

    public static PrinterUtils getInstance() {
        if (_self == null) {
            _self = new PrinterUtils();
        }
        return _self;
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (Exception e) {
            Log.e("Tag", "Exe ", e);
        }
    }

    public void startPrint(Bitmap bitmap) {


        float aspectRatio = bitmap.getWidth() /
                (float) bitmap.getHeight();
        int width = 490;
        int height = Math.round(width / aspectRatio);

        print(bitmap);


//        sliceBitmap(Bitmap.createScaledBitmap(bitmap, 490, height, false));
//
//
//       ArrayList<Bitmap> sliceBitmapArray= splitImage(Bitmap.createScaledBitmap(bitmap, 490, height, false),10);
//        print(sliceBitmapArray);
//        printImageTest(bs);
    }


    private void print(Bitmap bitmap) {

        Thread t = new Thread() {
            public void run() {

                try {
                    OutputStream os = mBluetoothSocket
                            .getOutputStream();
                    try {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        outputStream = mBluetoothSocket.getOutputStream();
                        byte[] printformat = new byte[]{0x1B, 0x21, 0x03};
                        byte[] print = {0x1B, 0x74, 0x11};
                        outputStream.write(print);

                        printNewLine();
                        outputStream.write(ESC_ALIGN_CENTER);
                        printNewLine();

                  printPhoto(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } catch (Exception e) {

                    Log.e("MainActivity", "Exe ", e);
                }
            }
        };
        t.start();

    }


    public static byte intToByteArray(int value) {
        byte[] b = ByteBuffer.allocate(4).putInt(value).array();

        for (int k = 0; k < b.length; k++) {
            System.out.println("Selva  [" + k + "] = " + "0x"
                    + UnicodeFormatter.byteToHex(b[k]));
        }
        return b[3];
    }

    public byte[] sel(int val) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putInt(val);
        buffer.flip();
        return buffer.array();
    }


    public static byte[] printImage(Bitmap image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(SET_LINE_SPACE_24);
        for (int y = 0; y < image.getHeight(); y += 24) {
            baos.write(SELECT_BIT_IMAGE_MODE);// bit mode
            baos.write(new byte[]{(byte) (0x00ff & image.getWidth()), (byte) ((0xff00 & image.getWidth()) >> 8)});// width, low & high
            for (int x = 0; x < image.getWidth(); x++) {
                // For each vertical line/slice must collect 3 bytes (24 bytes)
                baos.write(collectSlice(y, x, image));
            }

            baos.write(FEED_LINE);
        }
        baos.write(SET_LINE_SPACE_30);

        return baos.toByteArray();
    }

    private static byte[] collectSlice(int y, int x, Bitmap image) {
        byte[] slices = new byte[]{0, 0, 0};
        for (int yy = y, i = 0; yy < y + 24 && i < 3; yy += 8, i++) {// va a hacer 3 ciclos
            byte slice = 0;
            for (int b = 0; b < 8; b++) {
                int yyy = yy + b;
                if (yyy >= image.getHeight()) {
                    continue;
                }
                int color = image.getPixel(x, yyy);
                boolean v = shouldPrintColor(color);
                slice |= (byte) ((v ? 1 : 0) << (7 - b));
            }
            slices[i] = slice;
        }

        return slices;
    }

    private static boolean shouldPrintColor(int color) {
        final int threshold = 127;
        int a, r, g, b, luminance;
        a = (color >> 24) & 0xff;
        if (a != 0xff) { // ignore pixels with alpha channel
            return false;
        }
        r = (color >> 16) & 0xff;
        g = (color >> 8) & 0xff;
        b = color & 0xff;

        luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);

        return luminance < threshold;
    }


    //print custom
    private void printCustom(String msg, int size, int align) {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B, 0x21, 0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
        try {
            switch (size) {
                case 0:
                    outputStream.write(cc);
                    break;
                case 1:
                    outputStream.write(bb);
                    break;
                case 2:
                    outputStream.write(bb2);
                    break;
                case 3:
                    outputStream.write(bb3);
                    break;
            }

            switch (align) {
                case 0:
                    //left align
                    outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                    break;
                case 1:
                    //center align
                    outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                    break;
                case 2:
                    //right align
                    outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                    break;
            }
            outputStream.write(msg.getBytes());
//            outputStream.write(PrinterCommands.LF);
            byte[] print = {0x1B, 0x74, 0x11};
            outputStream.write(print);
            //outputStream.write(cc);
            //printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //print photo
    public void printPhoto(int img) {
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    img);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    public void printPhoto(Bitmap img) {
        try {
            Bitmap bmp = img;
            if (bmp != null) {


                float aspectRatio = bmp.getWidth() /
                        (float) bmp.getHeight();
                int width = 490;
                int height = Math.round(width / aspectRatio);

                byte[] command = Utils.decodeBitmap(Bitmap.createScaledBitmap(bmp, 560, height, false));


//                byte[] command = getBitmapBytes(bmp);
//                byte[] command = getBitmapBytes((Bitmap.createScaledBitmap(bitmap, 150, 150, false)));
//                byte[] command=printDraw(Bitmap.createScaledBitmap(bmp, 300, height, false));
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    public void printPhoto11111111111(ArrayList<Bitmap> bs) {
        try {
            ArrayList<Bitmap> bmp = bs;
            if (bmp != null) {

                for (int i = 0; i < bmp.size(); i++) {
                    Bitmap bitmap = bmp.get(i);
                    byte[] command = Utils.decodeBitmap(bitmap);
                    outputStream.write(PrinterCommands.ESC_ALIGN_LEFT.length);
                    printText(command);
                }


//
//

            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    //print unicode
    public void printUnicode() {
        try {
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
            printText(Utils.UNICODE_TEXT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //print new line
    private void printNewLine() {
        try {
            outputStream.write(PrinterCommands.FEED_LINE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void resetPrint() {
        try {
            outputStream.write(PrinterCommands.ESC_FONT_COLOR_DEFAULT);
            outputStream.write(PrinterCommands.FS_FONT_ALIGN);
            outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
            outputStream.write(PrinterCommands.ESC_CANCEL_BOLD);
            outputStream.write(PrinterCommands.LF);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //print text
    private void printText(String msg) {
        try {
            // Print normal text
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //print byte[]
    private void printText(byte[] msg) {
        try {
            // Print normal text
            outputStream.write(msg);
            printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String leftRightAlign(String str1, String str2) {
        String ans = str1 + str2;
        if (ans.length() < 31) {
            int n = (31 - str1.length() + str2.length());
            ans = str1 + new String(new char[n]).replace("\0", " ") + str2;
        }
        return ans;
    }


    private String[] getDateTime() {
        final Calendar c = Calendar.getInstance();
        String dateTime[] = new String[2];
        dateTime[0] = c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.MONTH) + "/" + c.get(Calendar.YEAR);
        dateTime[1] = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
        return dateTime;
    }

    private byte[] getBitmapBytes(Bitmap bitmap) {
        int chunkNumbers = 10;
        int bitmapSize = bitmap.getRowBytes() * bitmap.getHeight();
        byte[] imageBytes = new byte[bitmapSize];
        int rows, cols;
        int chunkHeight, chunkWidth;
        rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = bitmap.getHeight() / rows;
        chunkWidth = bitmap.getWidth() / cols;

        int yCoord = 0;
        int bitmapsSizes = 0;

        for (int x = 0; x < rows; x++) {
            int xCoord = 0;
            for (int y = 0; y < cols; y++) {
                Bitmap bitmapChunk = Bitmap.createBitmap(bitmap, xCoord, yCoord, chunkWidth, chunkHeight);
                byte[] bitmapArray = getBytesFromBitmapChunk(bitmapChunk);
                System.arraycopy(bitmapArray, 0, imageBytes, bitmapsSizes, bitmapArray.length);
                bitmapsSizes = bitmapsSizes + bitmapArray.length;
                xCoord += chunkWidth;

                bitmapChunk.recycle();
                bitmapChunk = null;
            }
            yCoord += chunkHeight;
        }

        return imageBytes;
    }

    private byte[] getBytesFromBitmapChunk(Bitmap bitmap) {
        int bitmapSize = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmapSize);
        bitmap.copyPixelsToBuffer(byteBuffer);
        byteBuffer.rewind();
        return byteBuffer.array();
    }


    //testing


    public Bitmap resize(Bitmap bitmap) {
        ////// scale image to fix image ratio => 512 / (int) Math.ceil((512*h)/w)
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int dw = 512;
        int dh;
        if (w < h) {
            // dh = 512;
            dh = 342;
        } else {
            dh = (int) Math.ceil((512 * h) / w);
        }

        return Bitmap.createScaledBitmap(bitmap, dw, dh, false);
    }

    private int[][] getPixels(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                result[row][col] = getRGB(image, col, row);
            }
        }

        return result;
    }

    private int getRGB(Bitmap bmpOriginal, int col, int row) {
        // get one pixel color
        int pixel = bmpOriginal.getPixel(col, row);
        // retrieve color of all channels
        int R = Color.red(pixel);
        int G = Color.green(pixel);
        int B = Color.blue(pixel);
        return Color.rgb(R, G, B);
    }

    private byte[] printImage(int[][] pixels) {
        ByteArrayOutputStream printPort = new ByteArrayOutputStream();

        try {
            //// Set the line spacing at 24 (we'll print 24 dots high)
            printPort.write(SET_LINE_SPACE_24);
            for (int y = 0; y < pixels.length; y += 24) {
                //// when done sending data per line,
                //// the printer will resume to normal text printing
                printPort.write(SELECT_BIT_IMAGE_MODE);
                //// Set nL and nH based on the width of the image
                printPort.write(new byte[]{
                        (byte) (pixels[y].length), (byte) ((pixels[y].length) >> 8)
                });
                for (int x = 0; x < pixels[y].length; x++) {
                    //// for each stripe, recollect 3 bytes (3 bytes (8bits *3) => 24 bits)
                    printPort.write(recollectSlice(y, x, pixels));
                }
                printPort.write(UCharacter.LineBreak.LINE_FEED);
            }
            //// Set the line spacing back to 42 (default line spacing for normal text printing)
            printPort.write(SET_LINE_SPACE_24);
        } catch (Exception e) {
            e.getStackTrace();
        }

        return printPort.toByteArray();

    }

    private byte[] recollectSlice(int y, int x, int[][] img) {
        byte[] slices = new byte[]{
                0,
                0,
                0
        };
        for (int yy = y, i = 0; yy < y + 24 && i < 3; yy += 8, i++) {
            byte slice = 0;
            for (int b = 0; b < 8; b++) {
                int yyy = yy + b;
                if (yyy >= img.length) {
                    continue;
                }
                int col = img[yyy][x];
                boolean v = shouldPrintColor1(col);
                slice |= (byte) ((v ? 1 : 0) << (7 - b));
            }
            slices[i] = slice;
        }

        return slices;
    }

    private boolean shouldPrintColor1(int col) {
        final int threshold = 225; //// 0 (black) - 255 (white)
        int a, r, g, b, luminance;
        a = (col >> 24) & 0xff;
        if (a != 0xff) { //// Ignore transparencies
            return false;
        }
        r = (col >> 16) & 0xff;
        g = (col >> 8) & 0xff;
        b = col & 0xff;

        if (r > threshold && g > threshold && b > threshold) { //// when it is nearly white color
            return false;
        }

        luminance = (int) ((r + g + b) / 3); //// (int)(0.299 * r + 0.587 * g + 0.299 * b)

        return luminance < threshold;
    }


    private void printImageTest(int[][] pixels) {

        Thread t = new Thread() {
            public void run() {

                try {
                    OutputStream os = mBluetoothSocket
                            .getOutputStream();
                    try {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        outputStream = mBluetoothSocket.getOutputStream();
                        byte[] printformat = new byte[]{0x1B, 0x21, 0x03};
                        byte[] print = {0x1B, 0x74, 0x11};
                        outputStream.write(print);

                        //// Set the line spacing at 24 (we'll print 24 dots high)
                        outputStream.write(SET_LINE_SPACE_24);
                        for (int y = 0; y < pixels.length; y += 24) {
                            //// when done sending data per line,
                            //// the printer will resume to normal text printing
                            outputStream.write(SELECT_BIT_IMAGE_MODE);
                            //// Set nL and nH based on the width of the image
                            outputStream.write(new byte[]{
                                    (byte) (pixels[y].length), (byte) ((pixels[y].length) >> 8)
                            });
                            for (int x = 0; x < pixels[y].length; x++) {
                                //// for each stripe, recollect 3 bytes (3 bytes (8bits *3) => 24 bits)
                                outputStream.write(recollectSlice(y, x, pixels));
                            }
                            outputStream.write(UCharacter.LineBreak.LINE_FEED);
                        }
                        //// Set the line spacing back to 42 (default line spacing for normal text printing)
                        outputStream.write(SET_LINE_SPACE_24);


                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } catch (Exception e) {

                    Log.e("MainActivity", "Exe ", e);
                }
            }
        };
        t.start();

    }


    private void sliceBitmap(Bitmap bitmap) {


        Bitmap b = bitmap;
        divideImages(b);

    }

    private void divideImages(Bitmap b) {
// TODO Auto-generated method stub
        final int width = b.getWidth();
        final int height = b.getHeight();

        final int pixelByCol = width / 2;
        final int pixelByRow = height / 2;
//List<Bitmap> bs = new ArrayList<Bitmap>();
        for (int i = 0; i < 2; i++) {
            System.out.println("row no. " + i);
            for (int j = 0; j < 2; j++) {

                System.out.println("Column no." + j);
                int startx = pixelByCol * j;
                int starty = pixelByRow * i;
                Bitmap b1 = Bitmap.createBitmap(b, startx, starty, pixelByCol, pixelByRow);
                bs.add(b1);

                b1 = null;
            }

        }

    }


    private ArrayList<Bitmap> splitImage(Bitmap image, int chunkNumbers) {

        //For the number of rows and columns of the grid to be displayed
        int rows,cols;

        //For height and width of the small image chunks
        int chunkHeight,chunkWidth;

        //To store all the small image chunks in bitmap format in this list
        ArrayList<Bitmap> chunkedImages = new ArrayList<Bitmap>(chunkNumbers);

        //Getting the scaled bitmap of the source image

        Bitmap bitmap =image;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = bitmap.getHeight() / rows;
        chunkWidth = bitmap.getWidth() / cols;

        //xCoord and yCoord are the pixel positions of the image chunks
        int yCoord = 0;
        for(int x = 0; x < rows; x++) {
            int xCoord = 0;
            for(int y = 0; y < cols; y++) {
                chunkedImages.add(Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, chunkWidth, chunkHeight));
                xCoord += chunkWidth;
            }
            yCoord += chunkHeight;
        }

        /* Now the chunkedImages has all the small image chunks in the form of Bitmap class.
         * You can do what ever you want with this chunkedImages as per your requirement.
         * I pass it to a new Activity to show all small chunks in a grid for demo.
         * You can get the source code of this activity from my Google Drive Account.
         */
        //Start a new activity to show these chunks into a grid
        return chunkedImages;
    }


    public Canvas canvas = null;

    public Paint paint = null;

    public Bitmap bm = null;
    public int width;
    public float length = 0.0F;

    public byte[] bitbuf = null;
    public int getLength() {
        return (int) this.length + 20;
    }

    public byte[] printDraw(Bitmap bitmap) {
        Bitmap nbm = Bitmap
                .createBitmap(bitmap, 0, 0, 300, getLength());

        byte[] imgbuf = new byte[300 / 8 * getLength() + 8];

        int s = 0;

        // 打印光栅位图的指令
        imgbuf[0] = 29;// 十六进制0x1D
        imgbuf[1] = 118;// 十六进制0x76
        imgbuf[2] = 48;// 30
        imgbuf[3] = 0;// 位图模式 0,1,2,3
        // 表示水平方向位图字节数（xL+xH × 256）
        imgbuf[4] = (byte) (this.width / 8);
        imgbuf[5] = 0;
        // 表示垂直方向位图点数（ yL+ yH × 256）
        imgbuf[6] = (byte) (getLength() % 256);//
        imgbuf[7] = (byte) (getLength() / 256);

        s = 7;
        for (int i = 0; i < getLength(); i++) {// 循环位图的高度
            for (int k = 0; k < this.width / 8; k++) {// 循环位图的宽度
                int c0 = nbm.getPixel(k * 8 + 0, i);// 返回指定坐标的颜色
                int p0;
                if (c0 == -1)// 判断颜色是不是白色
                    p0 = 0;// 0,不打印该点
                else {
                    p0 = 1;// 1,打印该点
                }
                int c1 = nbm.getPixel(k * 8 + 1, i);
                int p1;
                if (c1 == -1)
                    p1 = 0;
                else {
                    p1 = 1;
                }
                int c2 = nbm.getPixel(k * 8 + 2, i);
                int p2;
                if (c2 == -1)
                    p2 = 0;
                else {
                    p2 = 1;
                }
                int c3 = nbm.getPixel(k * 8 + 3, i);
                int p3;
                if (c3 == -1)
                    p3 = 0;
                else {
                    p3 = 1;
                }
                int c4 = nbm.getPixel(k * 8 + 4, i);
                int p4;
                if (c4 == -1)
                    p4 = 0;
                else {
                    p4 = 1;
                }
                int c5 = nbm.getPixel(k * 8 + 5, i);
                int p5;
                if (c5 == -1)
                    p5 = 0;
                else {
                    p5 = 1;
                }
                int c6 = nbm.getPixel(k * 8 + 6, i);
                int p6;
                if (c6 == -1)
                    p6 = 0;
                else {
                    p6 = 1;
                }
                int c7 = nbm.getPixel(k * 8 + 7, i);
                int p7;
                if (c7 == -1)
                    p7 = 0;
                else {
                    p7 = 1;
                }
                int value = p0 * 128 + p1 * 64 + p2 * 32 + p3 * 16 + p4 * 8
                        + p5 * 4 + p6 * 2 + p7;
                this.bitbuf[k] = (byte) value;
            }

            for (int t = 0; t < this.width / 8; t++) {
                s++;
                imgbuf[s] = this.bitbuf[t];
            }
        }
        if (null != this.bm) {
            this.bm.recycle();
            this.bm = null;
        }

        return imgbuf;
    }

}
