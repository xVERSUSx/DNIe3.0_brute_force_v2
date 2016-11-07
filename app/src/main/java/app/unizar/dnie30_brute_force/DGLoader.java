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
import app.unizar.dnie30_brute_force.tools.DistortImage;
import app.unizar.dnie30_brute_force.tools.J2kStreamDecoder;
import de.tsenger.androsmex.mrtd.DG11;
import de.tsenger.androsmex.mrtd.DG1_Dnie;
import de.tsenger.androsmex.mrtd.DG2;
import de.tsenger.androsmex.mrtd.DG7;
import de.tsenger.androsmex.mrtd.EF_COM;
import de.tsenger.androsmex.tools.HexString;
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
    private static long startTime, endTime, time1, time2, errorTime;
    private static double totalTime;
    private Context context;

    private static Bitmap loadedImage;
    private static Bitmap loadedSignature;
    private static boolean censure;
    private static final String IMAGES_PATH = "/sdcard/DNIe3_bf/";

    public DGLoader(Logger logger, Tag tag, String canNumber, Context context, boolean censure) {
        this.logger = logger;
        this.tag = tag;
        this.canNumber = canNumber;
        this.context = context;
        this.censure = censure;
    }

    private static String loadDGs() {
        try {
            startTime = System.currentTimeMillis();
            // Activamos el modo rápido para agilizar la carga.
            //System.setProperty("es.gob.jmulticard.fastmode", "true");

            // Se instancia el proveedor y se añade
            DnieProvider p = new DnieProvider();
            p.setProviderTag(tag);	/*(idch.getTag());*/    // Tag discovered by the activity
            p.setProviderCan(canNumber);    // DNIe’s Can number
            Security.insertProviderAt(p, 1);

            endTime = System.currentTimeMillis();
            time1 = endTime - startTime;

            logger.log(Level.ALL, tag + "\nCAN: " + canNumber);

            startTime = System.currentTimeMillis();

            // Cargamos certificados y keyReferences
            KeyStoreSpi ksSpi = new MrtdKeyStoreImpl();
            DnieKeyStore ksUserDNIe = new DnieKeyStore(ksSpi, p, "MRTD");

            ksUserDNIe.load(null, null);

            // Leemos el EF_COM para saber qué datos hay disponibles en el documento
            EF_COM m_efcom = ksUserDNIe.getEFCOM();
            byte[] tagList = m_efcom.getTagList();

            endTime = System.currentTimeMillis();
            time2 = endTime - startTime;

            totalTime = time1 + time2;
            totalTime = totalTime / 1000;
            logger.log(Level.ALL, "Tiempo utilizado: " + totalTime + " segundos.");

            logger.log(Level.ALL, "\n================================================================\n" +
                    "INFORMACIÓN EXTRAIDA DEL DNIe3.0:" +
                    "\n================================================================\n");

            startTime = System.currentTimeMillis();

            for (int idx = 0; idx < tagList.length; idx++) {
                switch (tagList[idx]) {
                    case 0x61:
                        // Obtenemos la información personal del DGs 1
                        DG1_Dnie m_dg1 = ksUserDNIe.getDatagroup1();
                        logger.log(Level.ALL, " -Apellidos, Nombre: " + m_dg1.getSurname() + ", " + m_dg1.getName());
                        logger.log(Level.ALL, " -Sexo: " + m_dg1.getSex());
                        if (censure) {
                            logger.log(Level.ALL, " -Fecha de nacimiento: ** **** ***" + m_dg1.getDateOfBirth().substring(m_dg1.getDateOfBirth().length()-1));
                        }else {
                            logger.log(Level.ALL, " -Fecha de nacimiento: " + m_dg1.getDateOfBirth());
                        }
                        logger.log(Level.ALL, " -Nacionalidad: " + m_dg1.getNationality());
                        if (censure) {
                            logger.log(Level.ALL, " -Número de soporte: *******" + m_dg1.getDocNumber().substring(m_dg1.getDocNumber().length()-2));
                            logger.log(Level.ALL, " -Fecha de expiración: ** **** ***" + m_dg1.getDateOfExpiry().substring(m_dg1.getDateOfExpiry().length()-1));
                        }else {
                            logger.log(Level.ALL, " -Número de soporte: " + m_dg1.getDocNumber());
                            logger.log(Level.ALL, " -Fecha de expiración: " + m_dg1.getDateOfExpiry());
                        }

                        //Prueba DGs
                        /*logger.log(Level.ALL, "  -Prueba issuer:" + m_dg1.getIssuer());
                        logger.log(Level.ALL, "  -Prueba OptData: " + m_dg1.getOptData());
                        logger.log(Level.ALL, "  -Prueba DocType: " + m_dg1.getDocType());*/
                        break;

                    case 0x6B:
                        // DG_11. Lo leemos siempre que esté disponible
                        DG11 m_dg11 = ksUserDNIe.getDatagroup11();

                        if (censure) {
                            logger.log(Level.ALL, " -Número personal: *******" + m_dg11.getPersonalNumber().substring(m_dg11.getPersonalNumber().length()-3));
                            logger.log(Level.ALL, " -Dirección: **********, " + m_dg11.getAddress(2) + ", "
                                    + m_dg11.getAddress(3));
                        }else {
                            logger.log(Level.ALL, " -Número personal: " + m_dg11.getPersonalNumber());
                            logger.log(Level.ALL, " -Dirección: " + m_dg11.getAddress(1) + ", " + m_dg11.getAddress(2) + ", "
                                    + m_dg11.getAddress(3));
                        }

                        //Prueba DGs
                        /*logger.log(Level.ALL, "  -Prueba birthPlace: " + m_dg11.getBirthPlace());
                        logger.log(Level.ALL, "  -Prueba tlfno: " + m_dg11.getPhone());
                        logger.log(Level.ALL, "  -Prueba profesion: " + m_dg11.getProfession());
                        logger.log(Level.ALL, "  -Prueba title: " + m_dg11.getTitle());
                        logger.log(Level.ALL, "  -Prueba CustodyInfo: " + m_dg11.getCustodyInfo());
                        logger.log(Level.ALL, "  -Prueba ICAOName: " + m_dg11.getIcaoName());
                        logger.log(Level.ALL, "  -Prueba OtherInfo: " + m_dg11.getOtherInfo());
                        logger.log(Level.ALL, "  -Prueba summary: " + m_dg11.getSummary());*/
                        break;
                    case 0x75:  //Imagen facial
                        // Obtenemos la imagen facial del ciudadano del DG2
                        try {
                            DG2 m_dg2 = ksUserDNIe.getDatagroup2();
                            byte[] imagen = m_dg2.getImageBytes();
                            /*logger.log(Level.ALL, "Bytes rostro:");
                            File ff = new File("/sdcard/DNIe3_bf/image_bytes.raw");
                            FileOutputStream fs = new FileOutputStream(ff);
                            fs.write(imagen);
                            fs.close();

                            StringBuilder sb = new StringBuilder();
                            StringBuilder sb2 = new StringBuilder();
                            for( int ii = 0; ii < imagen.length; ii++)
                            {
                                sb.append(String.format(" 0x%02x", imagen[ii]));
                                sb2.append(String.format(" %c", imagen[ii] & 0xff));
                            }
                            logger.log(Level.ALL, sb.toString());
                            logger.log(Level.ALL, sb2.toString());*/
                            J2kStreamDecoder j2k = new J2kStreamDecoder();
                            ByteArrayInputStream bis = new ByteArrayInputStream(imagen);
                            loadedImage = j2k.decode(bis);

                        }catch(Exception e)
                        {
                            e.printStackTrace();
                        }

                        File facePhoto = new File(IMAGES_PATH + "rostro.jpeg");

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
                            DG7 m_dg7 = ksUserDNIe.getDatagroup7();
                            byte[] imagen = m_dg7.getImageBytes();
                            J2kStreamDecoder j2k = new J2kStreamDecoder();
                            ByteArrayInputStream bis = new ByteArrayInputStream(imagen);
                            loadedSignature = j2k.decode(bis);

                        }catch(Exception e)
                        {
                            e.printStackTrace();
                        }

                        File signaturePhoto = new File(IMAGES_PATH + "firma.jpeg");

                        if (signaturePhoto.exists()) {
                            signaturePhoto.delete();
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(signaturePhoto.getPath());
                            loadedSignature.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            if (censure) {
                                DistortImage.pixelateImage(8, logger);
                            }else {
                                logger.log(Level.ALL, " -Imagen de la firma guardada en '" + signaturePhoto.getPath() + "'");
                            }
                        }
                        catch (IOException e) {
                            logger.log(Level.ALL, "Exception in photoCallback", e);
                            e.printStackTrace();
                        }
                        break;
                }
            }

            endTime = System.currentTimeMillis();
            totalTime = endTime - startTime;
            totalTime = totalTime / 1000;
            logger.log(Level.ALL, "\nTiempo utilizado para tratar los datos: " + totalTime + " segundos.");

        } catch (IOException e) {
            //endTime = System.currentTimeMillis();
            //totalTime = (endTime - startTime);
            //totalTime = totalTime / 1000;
            endTime = System.currentTimeMillis();
            errorTime = endTime - startTime;
            totalTime = time1 + errorTime;
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
        //Intent intent = new Intent("pace_finished");
        //intent.putExtra("message", result + "\nTime used: " + (endTime - startTime) + " ms");
        //LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        delegate.processFinish(result);
    }

}
