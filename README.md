# radiorandgentwo
This application, based on the radiorandgen project, extracts a block of random numbers from the Internet radio digital stream, which are used as an encryption key for the Vernam algorithm in commercial encryption.
If you choose the length of the byte array to search so that there are 1-2 values per day, add an SMS sending block, you will get a super secure messenger.<br>
/There is a "bug" in the application's Java code that is used when searching for a given combination of numbers in a stream. This "bug" makes the application possible./<br>
Application under development<br>
*** A separate interesting project is the transformation of digital streams into "analogue" digital streams. When on several "sides" we are synchronous in time (but without a synchronization channel) we get identical "analogue" curves. <br>
This app layout works. Selects an encryption key in the digital stream, encrypts the text, sends an SMS to itself, receives an SMS , decodes the text and displays the application (lower TextView)<br>
This is enough to create a complete messaging application using the Vernam cipher.<br>
In the next Streamsplitter project, we will consider splitting digital streams for "stream" encryption with the Vernam code and the use of speech technologies in strong encryption without the use of synchronization channels (key exchange channels)<br>
WORKING App
