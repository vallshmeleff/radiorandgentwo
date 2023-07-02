package com.example.radiostream;

import static java.util.Collections.binarySearch;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

//---------------------------------------------------
//
// RadioSTREAM Application
// We use an Internet radio stream to obtain the encryption (and decoding) key for the Vernam algorithm.
// We receive the digital data stream of any Internet radio at the same time at two points - the sender of the data (for encryption) and the recipient of the encrypted data.
// Write the data stream to the buffer. We are looking for a predetermined sequence of numbers in the buffer, which is known to the sender and recipient.
// After the given sequence is found, the next N numbers are read from the buffer. Where N= number of message characters to encrypt
// Because the sequence specified for waiting may fall on the buffer boundary (not completely fit in the buffer), we will accumulate the buffer data in a variable of a larger size (3-5)
// of the buffer size. Re-seek the sequence in the variable, shift the data by the size of the buffer, add a new buffer, and search again.
// Unlike asymmetric encryption (RSA, ECC), with this key derivation technology, the sender and recipient of information do not exchange public keys and encryption is faster.
// There are many opportunities to improve this technology.
//
//
// Read the Internet radio stream into the buffer and look for an array byte[] efind in the buffer
// The more bytes you specify to search, the longer you have to wait for a match. In this example - approximately 1-2 matches in 5 minutes
// Because the launch of the application for searching for keys in the radio stream is performed at the sender and receiver independently and not synchronously, for use in data
// encryption (commercial) it is necessary to create a storage of found random keys. It takes several attempts to decode a message with several keys.
//
// The application found 168 matches in 1 hour (writes a message about this in the LOG)
//
//---------------------------------------------------
// (c) by Valery Shmelev
// https://www.linkedin.com/in/valery-shmelev-479206227/
// https://github.com/vallshmeleff
//---------------------------------------------------
public class MainActivity extends AppCompatActivity {
    // We will look for this sequence of bytes in the streaming data buffer
    public static byte[] efind = {00,01,05,03,07,03,06,12,04,03,05,14}; // We are looking for an array of bytes in the buffer. If found - write to Log
    // If this sequence is found, then we read as many bytes (random bytes) as needed to encrypt using the Vernam algorithm
    // The longer the byte[] efind array to wait, the longer it will wait in the Internet radio data stream
    public static byte[] buffercopy = new byte[1000]; // Public copy of buffer
    public static byte[] twobuffercopy = new byte[2000]; // We write 2 buffers in a row here. When the next buffer arrives, a "left"
                                                         // shift occurs - the older buffer is replaced by the newer one. The new buffer
                                                         // is always written to the right half.
    public static int k = 0; // Counter
    public static StringBuilder nbuilder = new StringBuilder();
    public static StringBuilder efindhex = new StringBuilder();
    public static String VernamText = "We generate encryption keys from an Internet radio stream"; // Text to encryption
    public static int eeh = 0;
    public static EditText NumInStream;
    public static EditText SourceText;
    public static EditText VernamCodePage;
    public static EditText EncodedText;
    public static EditText DecodedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NumInStream = (EditText) findViewById(R.id.NumInStream); // We are looking for an array of bytes in the buffer. If found - write to Log
        SourceText = (EditText) findViewById(R.id.SourceText); // Source text for Encode
        VernamCodePage = (EditText) findViewById(R.id.VernamCodePage); // Encryption Key (Code page)
        EncodedText = (EditText) findViewById(R.id.EncodedText);
        DecodedText = (EditText) findViewById(R.id.DecodedText);

        SourceText.setText(VernamText); // Source Text


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Do not enter SLEEP mode


        ReadStream(); // Reading an Internet radio stream into a buffer
        //////= super.finish(); // Hide application

    } // OnCreate

    protected void ReadStream() {
    byte[] b = new byte[1000]; // Buffer for data from the Internet stream
         byte[] ee = new byte[3000]; // Here we will copy the contents of three buffers to search for given numbers
        int ev = 0; // The variable ee fits 3 buffers b. This is a counter from 0 to 2 - what part of ee is filled from buffer b
        new Thread(new Runnable()  {
            @Override
            public void run() {
                try {
                    URL radioUrl = new URL("https://opera-stream.wqxr.org/operavore");
                    // https://icecast-vgtrk.cdnvideo.ru/vestifm_aac_64kbps
                    InputStream in = radioUrl.openStream();
                    InputStream is = new BufferedInputStream(in);
                    is.read(b);
                    Log.i("== Stream =", " == == Stream DATA to Buffer == == ");

                    int count;
                    is.read(b); // == First READ Stream ==
                    buffercopy = b; // Copy buffer b to buffercopy
                    FirstTwoBuffer(); // Copy buffer to right twobuffercopy

                    while ((count = is.read(b)) != -1) { // == Next READ Stream ==
                        is.read(b);
                        buffercopy = b; // Copy buffer b to buffercopy

                        //= String doc = new String(b, "UTF-8"); // For Debug
                        //= Log.i("== Stream =", " == == Stream DATA == == " + doc); // Buffer to LOG Screen
                        FindDigit(); // We are looking for a given array of bytes in the buffer (array of bytes)
                        TwoBuffer(); // Create a double buffer. If the specified sequence is found, then read the encryption key on the right side
                    }
                } catch (FileNotFoundException e) {
                    e.getMessage();
                } catch (IOException e) {
                    e.getMessage();
                }
                //return null;
            }

            }).start(); // Thread


    } // ReadStream

    public void FindDigit() { // We are looking for a given sequence of digits in the buffer. If found - write to Log
        int eh = searchBytePattern(buffercopy,efind); // Find efind in buffercopy. eh - index in buffercopy
    } // FindDigit


    public static void GetVernamKey() { // Read the following after the index "eh" N bytes - the key for encryption

        // Convert efind to HEX
        StringBuilder efindenn = new StringBuilder();
        for (int y = 0; y < twobuffercopy.length-1; y++) { // efind start to end
            efindenn.append(String.format("%02x", twobuffercopy[y])); // Convert to HEX
        }
        Log.i("== VERNAM CODE HEX =", " == == twobuffercopy == == " + efindenn.toString());

        // Let's write the selected encryption key in HEX codes
        StringBuilder vernamhex = new StringBuilder();
        StringBuilder codedvernamhex = new StringBuilder(); // Encoded text in HEX
        String restoredvernamtext = ""; // Decoded text

        byte[] Vernam = new byte[VernamText.length()];
        int et = eeh+1; // Next position in buffer
        int myNum = 0; // Convert String to Int
        int tx =0; // After XOR
        int dtx =0; // After XOR and XOR (decode)
        for (int y = et; y < et+VernamText.length(); y++) { // Buffer start to end
            Vernam[y-et] = twobuffercopy[y]; // Write from twobuffercopy[2000] to Vernam bytes array
            vernamhex.append(String.format("%02x", Vernam[y-et])); // Convert to HEX
            // ------- Perform an XOR operation between each byte VernamText and Vernam[y-et] --------
            String substr=VernamText.substring(y-et,y-et+1);

            try {
                myNum = Integer.valueOf((int) substr.charAt(0));  // Select 1 String symbol, convert to char and convert to Int
                // int tx = twobuffercopy[1] ^ twobuffercopy[2];
                tx = myNum ^ Math.abs(twobuffercopy[y]);  // myNum XOR Vernam_key_byte. Math.abs - only > 0
                dtx = tx ^ Math.abs(twobuffercopy[y]);  // Decode - XOR and XOR (restored chars)
                codedvernamhex.append(String.format("%02x", tx)); // Convert Encoded text to HEX
                restoredvernamtext = restoredvernamtext + String.valueOf((char)dtx); // Decoded text
            } catch(NumberFormatException nfe) {
                System.out.println("Could not parse " + nfe);
            }
            Log.i("== VERNAM CODE HEX =", " == == CONVERT == == " + substr + " " + String.valueOf(tx) + " tx=" + String.format("%02x", tx) + " dtx=" + String.format("%02x", dtx) + " char=" + (char)dtx);

        }
        Log.i("== VERNAM CODE HEX =", " == == VERNAM CODE HEX == == " + vernamhex.toString());
        VernamCodePage.setText(vernamhex.toString());
        Log.i("== VERNAM CODE HEX =", " == == ENCODED TEXT in HEX == == " + codedvernamhex.toString()); // Encoded text in HEX
        EncodedText.setText(codedvernamhex.toString());
        Log.i("== VERNAM CODE HEX =", " == == Vernam key start position = eh+1 == == " + String.valueOf(et));
        Log.i("== VERNAM CODE HEX =", " == == Decoded Vernam Text == == " + restoredvernamtext.toString());
        DecodedText.setText(restoredvernamtext.toString());

    } // GetVernamKey



    //-----------------------------------------------------
    //
    // We make a copy of the digital stream buffer and look for a sequence of bytes in it efind
    // but the elements of the array of whites efind are NOT searched in a row! When a match is found for element efind[0]
    // then the element efind[1] is searched for, etc.
    //
    //-----------------------------------------------------
    public static int searchBytePattern(byte[] earray, byte[] epattern) {
        int epr = earray.length;
        int eptt = epattern.length;
        int i = 0;
        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());

        if (epattern.length > earray.length) {
            return -1;
        }
        int j = 0; // Counter in epattern
        // int lg = 0; // Not find
        for (i = 0; i <= epr - eptt; i++) { // Buffer start to end
            if (earray[i] == epattern[j]) {
                //= Log.i("== Stream FIND =", " == == earray[i] in HEX  == == " + String.format("%02x", earray[i]) + " i= " + String.valueOf(i));
                //= Log.i("== Stream FIND =", " == == epattern[j] in HEX  == == " + String.format("%02x", epattern[j]) + " j= " + String.valueOf(j));

                if (j < eptt-1){
                    j++;

                } else { // If the given sequence efind is encountered in the stream
                    k++;
                    eeh = i;
                    Log.i("== Stream FIND =", " == == Given array efind found in data stream == == !!!!!!!!!!!!!!!!!!" + String.valueOf(k) + " Time: " + currentDateTimeString.toString());
                    Log.i("== Stream FIND =", " == == Given array efind found in data stream in I position == == " + " I= " + String.valueOf(i));
                    GetVernamKey();

                    // Convert earray to HEX
                    for (int h = 0; h <  earray.length; h++) { // efind start to end
                        nbuilder.append(String.format("%02x", earray[h])); // Convert to HEX
                    }
                    for (int h = 0; h <  epattern.length; h++) { // efind start to end
                        nbuilder.append(String.format("%02x", epattern[h])); // Convert to HEX
                    }
                    //= Log.i("== Stream FIND =", " == == Buffer in HEX  == == " + nbuilder.toString());
                    //= Log.i("== Stream FIND =", " == == EFIND in HEX  == == " + efindhex.toString());
                j=0;
                }

            } // if (earray[i] == epattern[j])
              // It may seem that the "else" construct is missing here. But this "mistake" is made on purpose.
              // If you show in LOG how indexes i and j change, you will see that it is not an array of consecutive
              // numbers efind = {00,01,05,03,07,03,06,12,04,03} that is being searched in the stream. Everything
              // is much more complicated. Specially made "bugs" of programming sometimes give the desired result.



        } // for (i = 0; i <= epr - eptt; i++)
        return i;
    }





public void FirstTwoBuffer(){
    StringBuilder builder = new StringBuilder();
    for (int y = 0; y < 1000; y++) { // Buffer start to end
        twobuffercopy[y+1000] = buffercopy[y]; // Write new buffer to right half
        builder.append(String.format("%02x", buffercopy[y])); // Convert to HEX
    }
// Binary to String
    Log.i("== StringBuilder =", " == == Buffer in HEX == == " + builder.toString());
    } // TwoBuffer


    public void TwoBuffer(){
        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();
        for (int y = 0; y < 1000; y++) { // Buffer start to end
            twobuffercopy[y] = twobuffercopy[y+1000]; // Rewrite the right half to the left (older)
            builder.append(String.format("%02x", twobuffercopy[y])); // Convert to HEX
        }
        for (int y = 0; y < 1000; y++) { // Buffer start to end
            twobuffercopy[y+1000] = buffercopy[y]; // Write new buffer to right half
            builder2.append(String.format("%02x", buffercopy[y])); // Convert to HEX
        }
        // How the right side of the double buffer is copied to the left side and the right side is filled with new data from the stream
        //= Log.i("== StringBuilder =", " == == Buffer in HEX 1  == == " + builder.toString());
        //= Log.i("== StringBuilder =", " == == Buffer in HEX 2 == == " + builder2.toString());
    } // TwoBuffer




} // MainActivity