package ru.zhelonkin.tgcontest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IOUtils {

    public static String readToString(InputStream is) throws IOException {
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            return total.toString();
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }

    }
}
