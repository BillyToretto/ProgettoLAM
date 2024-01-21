package com.lam.progetto_lam.DataBaseR.Entità;

import androidx.room.TypeConverter;
import java.sql.Timestamp;

/**
 *
 * convertire i valori di timestamp in stringa e viceversa.
 * Utilizzato per CreationDate in MisuraEntita.
 *
 */
public class ConvertitoreTimestamp {

    @TypeConverter
    public static Timestamp toTimestamp(String valore) {
        if (valore != null) {
            try {
                return Timestamp.valueOf(valore);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            return null;
        }
    }

    @TypeConverter
    public static String timestampToString(Timestamp timestamp) {
        return (timestamp != null) ? timestamp.toString() : null;
    }
}
