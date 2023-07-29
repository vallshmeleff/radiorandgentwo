package com.example.radiostreamsms;

import static java.util.Collections.binarySearch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.Date;

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
// The application sends the encoded text "to itself" via SMS, receives and decodes. In fact, this is version 0.0 of the communication application. It is enough to indicate the recipient's
// phone number with the same application and you can send important information. For real use, it is necessary to obtain the exact time from a special server or from the GSM Network so that
// the transmitting and receiving parties select the same encryption key from the stream. You can add to the receiving methods to store several keys in a row and apply them sequentially to decode the text.
// Such an application will never be as convenient for sending secure messages as applications using AES or RSA encryption. However, theoretically, it can have absolute cryptographic reliability.
// To obtain absolute cryptographic strength, external hardware random digital stream generators, the use of stream splitting, or digital-to-analog synthesis are required.
//
// Email:
// 3f9d45e41863fa14dac44196ff1401a4c6aa230951f0924db95925409c2ac23b30bd22bcb39be1adca6074f3b315939075272c6093d5f70fb8
// Key:
// 6fef2a836a029779bfb66db69a7a66cda8cf467b7dd0fb23cf3c4b34f358e20610cb43d0dfee8cdea20d119fd673f5d0124a4d09fffb9460d5
//
//---------------------------------------------------
// (c) by Valery Shmelev
// https://www.linkedin.com/in/valery-shmelev-479206227/
// https://github.com/vallshmeleff
//---------------------------------------------------
public class MainActivity extends AppCompatActivity {
    // We will look for this sequence of bytes in the streaming data buffer
    public static byte[] efind = {00,01,05,03,07,03,06,12,04}; // We are looking for an array of bytes in the buffer. If found - write to Log
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

    public static String eBufferPOSITION = ""; // Number - Position in Stream Buffer
    public static String eSurceTEXT = ""; // Source Text for Ecoding
    public static String eVernamHEX = ""; // Vernam Code Page in HEX
    public static String eVernamENCODE = ""; // Encode Text
    public static String eVernamDECODE = ""; // Decoded Text
    public static String codedtexthex = ""; // Coded Text in HEX (for SMS sending)
    public static String vernampagehex = ""; // Vernam Key in HEX

    public static int eeh = 0;
    public static EditText NumInStream;
    public static EditText SourceText;
    public static EditText VernamCodePage;
    public static EditText EncodedText;
    public static EditText DecodedText;
    public static Context Maincontext;
    //public static StringBuilder vernamhex = new StringBuilder();
    public static int smstrigger = 0; // SMS has not been sent yet

    private final static int SEND_SMS_PERMISSION_REQ=1;
    public static Integer stt = 0; // SMS trigger. 0 - no encoded text, 1 - have encoded text
    public static byte[] Vernam;
    public static int KeyFind = 0; // Trigger. =0 - Vernam Key not find in stream. =1 - Key find

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_REQ);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS}, PackageManager.PERMISSION_GRANTED);



        Maincontext = getApplicationContext(); //To work with context
        NumInStream = (EditText) findViewById(R.id.NumInStream); // We are looking for an array of bytes in the buffer. If found - write to Log
        SourceText = (EditText) findViewById(R.id.SourceText); // Source text for Encode
        VernamCodePage = (EditText) findViewById(R.id.VernamCodePage); // Encryption Key (Code page)
        EncodedText = (EditText) findViewById(R.id.EncodedText);
        DecodedText = (EditText) findViewById(R.id.DecodedText);



        SourceText.setText(VernamText); // Source Text (None crypted)


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Do not enter SLEEP mode

        ReadStream(); // Reading an Internet radio stream into a buffer

        //////= super.finish(); // Hide application

    } // OnCreate

    private void requestReadAndSendSmsPermission() {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_SMS},1);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS},1);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_RESPOND_VIA_MESSAGE}, 1);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS}, 1);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, 1);

    }


    protected void ReadStream() {
        byte[] b = new byte[1000]; // Buffer for data from the Internet stream
        byte[] ee = new byte[3000]; // Here we will copy the contents of three buffers to search for given numbers
        int ev = 0; // The variable ee fits 3 buffers b. This is a counter from 0 to 2 - what part of ee is filled from buffer b
        new Thread(new Runnable()  {
            @Override
            public void run() {
                try {
                    URL radioUrl = new URL("https://opera-stream.wqxr.org/operavore"); // Internet-Radio
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

                        FindDigit(); // We are looking for a given array of bytes in the buffer (array of bytes)
                        TwoBuffer(); // Create a double buffer. If the specified sequence is found, then read the encryption key on the right side

                        handler.sendEmptyMessage(0); // Write to Edit Text

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
        if (KeyFind == 0) { // Show DOUBLE Buffer in HEX codes (debugging only)
            for (int y = 0; y < twobuffercopy.length - 1; y++) { // efind start to end
                efindenn.append(String.format("%02x", twobuffercopy[y])); // Convert to HEX
                // efindenn.append(Integer.toString(twobuffercopy[y], 16)); // Convert to HEX 2
            }
            Log.i("== VERNAM CODE HEX =", " == == twobuffercopy == == " + efindenn.toString());

            // Let's select the Encryption Key from the double buffer and write it to vernamhex in HEX codes
            String vernamhex = "";

            StringBuilder codedvernamhex = new StringBuilder(); // Ciphertext in HEX codes
            String restoredvernamtext = ""; // Decoded text

            Vernam = new byte[VernamText.length()]; // VernamText - original (unencrypted) text
            int et = eeh + 1; // The next position in the double buffer. Beginning of Encryption Key
            int myNum = 0; // Convert Source Text one character at a time to Int
            int tx = 0; // myNum after XOR with encryption key
            int dtx = 0; // myNum after re-XOR (decoding)
            int x = 0; // Counter
            // Encoding process
            for (int y = et; y < et + VernamText.length(); y++) { // Read the double buffer from the next position =et (where the Encryption Key starts)
                Vernam[y - et] = twobuffercopy[y]; // Write the Encryption Key from the double buffer to a separate Vernam buffer
                vernamhex = vernamhex + (String.format("%02x", Vernam[y - et])); // Key. Encryption Key (String) in HEX codes
                // ------- Perform an XOR operation between each byte VernamText and Vernam[y-et] --------
                String substr = VernamText.substring(y - et, y - et + 1); // VernamText - Original text. Choose ONE character

                try {
                    myNum = Integer.valueOf((int) substr.charAt(0));  // Text. One character of the source text is converted to Int
                            String subVernPage = String.valueOf(vernamhex).substring(x,x+2); // Key. Selected ONE HEX code from the Encryption Key
                            tx = myNum ^ Math.abs(Integer.parseInt(subVernPage, 16));  // Coding. myNum XOR Vernam_key_byte. Math.abs - only > 0
                    dtx = tx ^ Math.abs(Integer.parseInt(subVernPage, 16));  // Decoding. XOR and XOR (restored chars)
                    codedvernamhex.append(String.format("%02x", tx)); // Convert Encoded text to HEX
                    restoredvernamtext = restoredvernamtext + String.valueOf((char) dtx); // Decoded text
                } catch (NumberFormatException nfe) {
                    System.out.println("Could not parse " + nfe);
                }
                Log.i("== VERNAM CODE HEX =", " == == CONVERT == == " + substr + " " + String.valueOf(tx) + " tx=" + String.format("%02x", tx) + " dtx=" + String.format("%02x", dtx) + " char=" + (char) dtx);
                x++;
                x++;
            }

        MainActivity.eSurceTEXT = "SourceText: " + VernamText; // Soutce text for encode

        Log.i("== VERNAM CODE HEX =", " == == VERNAM CODE HEX == == " + vernamhex.toString());
        MainActivity.eVernamHEX = "Crypto Notes Page: " + String.valueOf(vernamhex); //vernamhex.toString(); // Crypto Notes Page
        vernampagehex =  String.valueOf(vernamhex); // Vernam Key in HEX
        Log.i("== VERNAM CODE HEX =", " == == VARIABLE == == " + MainActivity.eVernamHEX);


        Log.i("== VERNAM CODE HEX =", " == == ENCODED TEXT in HEX == == " + codedvernamhex.toString()); // Encoded text in HEX
        MainActivity.eVernamENCODE = "Encoded Text: " + codedvernamhex.toString(); // Crypted Text (XOR)
        MainActivity.codedtexthex =  codedvernamhex.toString(); // Crypted Text (XOR)- for SMS Sending

        Log.i("== VERNAM CODE HEX =", " == == Vernam key start position = eh+1 == == " + String.valueOf(et));
        MainActivity.eBufferPOSITION = "Looking for a number in the stream: " + String.valueOf(et); // Position in Stream Buffer

        Log.i("== VERNAM CODE HEX =", " == == Decoded Vernam Text == == " + restoredvernamtext.toString());
        // // == == MainActivity.eVernamDECODE = "Decoded Text from SMS: " + restoredvernamtext.toString(); // Decoded Text (Next XOR)
        stt = 1; //SMS send

            KeyFind = 1; // Vernam Key find
        } // End If


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
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        if (epattern.length > earray.length) {
            return -1;
        }
        int j = 0; // Counter in epattern
        // int lg = 0; // Not find
        for (i = 0; i <= epr - eptt; i++) { // Buffer start to end
            if (earray[i] == epattern[j]) {

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



    Handler handler = new Handler(Looper.getMainLooper()) { // Print Text on screen
        @Override
        public void handleMessage(Message msg) {

            EditText sxtv = findViewById(R.id.NumInStream); // Position in Stream Buffer
            sxtv.setText(MainActivity.eBufferPOSITION);
            EditText cxtv = findViewById(R.id.SourceText); // Source Text to Encode
            cxtv.setText(MainActivity.eSurceTEXT);
            EditText txtv = findViewById(R.id.VernamCodePage); // Crypto Page (encryption key)
            txtv.setText(MainActivity.eVernamHEX);
            EditText extv = findViewById(R.id.EncodedText); // Encoded Text
            extv.setText(MainActivity.eVernamENCODE);  // Crypted Text
            EditText gxtv = findViewById(R.id.DecodedText); // Decoded Text
            gxtv.setText(MainActivity.eVernamDECODE);

            if (stt == 1) { // If the key is found, the text is encrypted, then
                dial(); // SMS send
                stt=0; // SMS trigger - disable position
            }
        }
    };

    //-----------------------------------------------------
    // SMS Send
    //-----------------------------------------------------

    public void dial() { // Send SMS

        String numberText = "89299626501"; // Sending to ourselves
        String messageText = codedtexthex; // SMS Text - encoded source text
        //// smstrigger = 1; // For Debug not send SMS
        if (smstrigger == 0) {
            SmsManager.getDefault()
                    .sendTextMessage(numberText, null, messageText.toString(), null, null);
            smstrigger = 1; // Do not send SMS anymore
        }
    }


    //-----------------------------------------------------
    // Decode SMS
    //-----------------------------------------------------
    public void  DecodeSMS (String SMSMessage) {
        //////// SmsReceiver rsagente = new SmsReceiver(); // Class instance SmsReceiver
        // eVernamENCODE; // SMS Text - encoded source text
        // byte[] Vernam - vernam code page (crypto key)

        String DebugText = "";
        String restoredvernamtext = ""; // Decoded text
        String subste = "";
        int x = 0; // Counter in HEX code page VernamCodePageHEX

        Log.i("== Decode SMS =", " == == ||||| SMS substr ||||| == == " + SMSMessage.length());
        Log.i("== Decode SMS =", " == == ||||| SMSMessage Crypted Text ||||| == == " + SMSMessage);
        Log.i("== Decode SMS =", " == == ||||| Crypted Text inside APP ||||| == == " + codedtexthex);

        for (int y = 0; y < codedtexthex.length(); y++) {
            subste = codedtexthex.substring(y,y+2); // Text in HEX. Crypted Text from SMS
            String subsiye = vernampagehex.substring(y,y+2); // Key in HEX. Key from Vernam Page
            // subVernPage = VernamCodePageHEX.substring(x,x+2); // Vernam Code Page in HEX (Key)
            //// Log.i("== Decode SMS =", " == == ||||| == == SMS substr == == ||||| == == " + subste + subsiye);
            // DebugText = DebugText + subste;
            try {
                //== int myNum = Integer.valueOf((int) substr.charAt(0));  // Select 1 String symbol, convert to char and convert to Int
                int myNum = Integer.decode("0x"+ subste);  // Select 1 String symbol, convert to char and convert to Int
                int dtx = myNum ^ Math.abs(Integer.parseInt(subsiye, 16));  // myNum XOR Vernam - decode. Math.abs - only > 0
                restoredvernamtext = restoredvernamtext + String.valueOf((char)dtx); // Decoded text

            } catch(NumberFormatException nfe) {
                System.out.println("Could not parse " + nfe);
            }
            y++;

        }


        Log.i("== Decode SMS =", " == == SMS SMSMessage == == " + eVernamENCODE);

        Log.i("== Decode SMS =", " == == SMS Debug Text == == " + DebugText);

        //// Log.i("== Decode SMS =", " == == SMS Decoded Text == == " + SMSMessage.substring(14,16));

        MainActivity.eVernamDECODE = "SMS Decoded Text: " + restoredvernamtext; // Decoded Text from SMS
        Log.i("== Decode SMS =", " == ==== == >> SMS Decoded Text == ==== == " + "SMS Decoded Text: " + restoredvernamtext);


    }


    //-----------------------------------------------------
    // Exit
    //-----------------------------------------------------
    public void onButtonExit(View arg0) {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);

    }





} // MainActivity

