cd PdfiumAndroid/src/
JAVAPATH=~/android/sdk/platforms/android-19/android.jar:.

JAVAH="javah -classpath .:com/kirrupt/pdfiumandroid/:$JAVAPATH -jni"

javac com/kirrupt/pdfiumandroid/PDFReader.java -classpath $JAVAPATH
javac com/kirrupt/pdfiumandroid/PDFDocument.java -classpath $JAVAPATH
$JAVAH com.kirrupt.pdfiumandroid.PDFReader
$JAVAH com.kirrupt.pdfiumandroid.PDFDocument

mv *.h ../../native/
