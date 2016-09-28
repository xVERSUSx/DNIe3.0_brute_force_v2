package app.unizar.dnie30_brute_force;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.unizar.dnie30_brute_force.tools.AsyncResponse;
import app.unizar.dnie30_brute_force.tools.J2kStreamDecoder;
import de.tsenger.androsmex.mrtd.DG11;
import de.tsenger.androsmex.mrtd.DG1_Dnie;
import de.tsenger.androsmex.mrtd.DG2;
import de.tsenger.androsmex.mrtd.DG7;
import de.tsenger.androsmex.mrtd.EF_COM;
import es.gob.jmulticard.jse.provider.DnieKeyStore;
import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.jse.provider.MrtdKeyStoreImpl;

/**
 * Created by victor on 8/08/16.
 */
public class DGLoader extends AsyncTask<Void, String, String> {

    private static Logger logger;
    private static Tag tag;
    private static String canNumber;
    public AsyncResponse delegate = null;
    private static long startTime;
    private static long endTime;
    private static double totalTime;
    private Context context;

    private static Bitmap loadedImage;
    private static Bitmap loadedSignature;

    public DGLoader(Logger logger, Tag tag, String canNumber, Context context) {
        this.logger = logger;
        this.tag = tag;
        this.canNumber = canNumber;
        this.context = context;
    }

    private static String loadDGs() {
        try {
            startTime = System.currentTimeMillis();
            // Activamos el modo rápido para agilizar la carga.
            System.setProperty("es.gob.jmulticard.fastmode", "true");

            // Se instancia el proveedor y se añade
            DnieProvider p = new DnieProvider();
            p.setProviderTag(tag);	/*(idch.getTag());*/    // Tag discovered by the activity
            p.setProviderCan(canNumber);    // DNIe’s Can number
            Security.insertProviderAt(p, 1);

            logger.log(Level.ALL, tag + "\nCAN: " + canNumber);
            //asLogger.log(Level.ALL, "IDCH " + idch.getTag());

            // Cargamos certificados y keyReferences
            KeyStoreSpi ksSpi = new MrtdKeyStoreImpl();
            DnieKeyStore ksUserDNIe = new DnieKeyStore(ksSpi, p, "MRTD");

            ksUserDNIe.load(null, null);
            //asLogger.log(Level.ALL, "KeyStore: " + ksUserDNIe.getKeyStore().toString());

            // Leemos el EF_COM para saber qué datos hay disponibles en el documento
            EF_COM m_efcom = ksUserDNIe.getEFCOM();
            byte[] tagList = m_efcom.getTagList();

            logger.log(Level.ALL, "\n================================================================\n" +
                    "INFORMACIÓN EXTRAIDA DEL DNIe3.0:" +
                    "\n================================================================\n");

            for (int idx = 0; idx < tagList.length; idx++) {
                switch (tagList[idx]) {
                    case 0x61:
                        // DG_1. Lo leemos siempre que esté disponible
                        //asLogger.log(Level.ALL, "DG1 disponible");
                        // Obtenemos la información personal de los DGs 1 y 11
                        DG1_Dnie m_dg1 = ksUserDNIe.getDatagroup1();
                        logger.log(Level.ALL, " -Apellidos, Nombre: " + m_dg1.getSurname() + ", " + m_dg1.getName());
                        logger.log(Level.ALL, " -Sexo: " + m_dg1.getSex());
                        logger.log(Level.ALL, " -Fecha de nacimiento: " + m_dg1.getDateOfBirth());
                        logger.log(Level.ALL, " -Nacionalidad: " + m_dg1.getNationality());
                        logger.log(Level.ALL, " -Número de soporte: " + m_dg1.getDocNumber());
                        logger.log(Level.ALL, " -Fecha de expiración: " + m_dg1.getDateOfExpiry());
                        break;

                    case 0x6B:
                        // DG_11. Lo leemos siempre que esté disponible
                        //asLogger.log(Level.ALL, "DG11 disponible");
                        DG11 m_dg11 = ksUserDNIe.getDatagroup11();
                        logger.log(Level.ALL, " -Número personal:" + m_dg11.getPersonalNumber());
                        logger.log(Level.ALL, " -Dirección: " + m_dg11.getAddress(1) + ", " + m_dg11.getAddress(2) + ", "
                                + m_dg11.getAddress(3));
                        break;
                    case 0x75:  //Imagen facial
                        // DG_2. Lo leemos si el usuario lo especificó
                        //asLogger.log(Level.ALL, "DG2 disponible");
                        // Obtenemos la imagen facial del ciudadano del DG2
                        try {
                            DG2 m_dg2 = ksUserDNIe.getDatagroup2();
                            byte[] imagen = m_dg2.getImageBytes();
                            J2kStreamDecoder j2k = new J2kStreamDecoder();
                            ByteArrayInputStream bis = new ByteArrayInputStream(imagen);
                            loadedImage = j2k.decode(bis);

                        }catch(Exception e)
                        {
                            e.printStackTrace();
                        }

                        File facePhoto = new File("/sdcard/DNIe3_bf/rostro.jpeg");

                        if (facePhoto.exists()) {
                            facePhoto.delete();
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(facePhoto.getPath());
                            loadedImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            logger.log(Level.ALL, " -Imagen facial guardada en '" + facePhoto.getPath() + "'");
                        }
                        catch (IOException e) {
                            logger.log(Level.ALL, "Exception in photoCallback", e);
                            e.printStackTrace();
                        }

                        break;
                    case 0x67:  //Imagen de la firma
                        try {
                            // DG_7. Lo leemos si el usuario lo especificó
                            //asLogger.log(Level.ALL, "DG7 disponible");
                            DG7 m_dg7 = ksUserDNIe.getDatagroup7();
                            byte[] imagen = m_dg7.getImageBytes();
                            J2kStreamDecoder j2k = new J2kStreamDecoder();
                            ByteArrayInputStream bis = new ByteArrayInputStream(imagen);
                            loadedSignature = j2k.decode(bis);

                        }catch(Exception e)
                        {
                            e.printStackTrace();
                        }

                        File signaturePhoto = new File("/sdcard/DNIe3_bf/firma.jpeg");

                        if (signaturePhoto.exists()) {
                            signaturePhoto.delete();
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(signaturePhoto.getPath());
                            loadedSignature.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            logger.log(Level.ALL, " -Imagen de la firma guardada en '" + signaturePhoto.getPath() + "'");
                        }
                        catch (IOException e) {
                            logger.log(Level.ALL, "Exception in photoCallback", e);
                            e.printStackTrace();
                        }
                        break;
                }
            }

        } catch (IOException e) {
            endTime = System.currentTimeMillis();
            totalTime = (endTime - startTime);
            totalTime = totalTime / 1000;
            return "" + totalTime;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            logger.log(Level.ALL, "NoSuchAlgorithmException");
        } catch (CertificateException e) {
            e.printStackTrace();
            logger.log(Level.ALL, "CertificateException");
        }
        return "CAN correcto";
    }

    @Override
    protected String doInBackground(Void... params) {
        return loadDGs();
    }

    @Override
    protected void onPostExecute(String result) {
        Intent intent = new Intent("pace_finished");
        intent.putExtra("message", result + "\nTime used: " + (endTime - startTime) + " ms");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        delegate.processFinish(result);
    }

}
