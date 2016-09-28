package app.unizar.dnie30_brute_force.tools;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Clase que reemplaza al 'java.util.logging.Formatter' original
 * para evitar que aparezca la fecha al inicio de cada mensaje de log
 */
public class CustomFormatterLog extends Formatter {

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(formatMessage(record));
        builder.append("\n");
        return builder.toString();
    }
}
