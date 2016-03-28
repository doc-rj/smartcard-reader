/*
 * Copyright 2014 Ryan Jones
 * Copyright 2010 sasc
 * 
 * This file was modified from the original source:
 * https://code.google.com/p/javaemvreader/
 * 
 * This file is part of smartcard-reader, package org.docrj.smartcard.reader.
 *
 * smartcard-reader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * smartcard-reader is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with smartcard-reader. If not, see <http://www.gnu.org/licenses/>.
 */

package org.docrj.smartcard.util;

import android.content.res.Resources;

import org.docrj.smartcard.reader.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * ISO 3166-1
 * ISO Country Code (3 digit Numeric)
 *
 * java.util.Locale doesn't support the 3-digit code variant of 3166 (part 1),
 * so we must use our own list
 */
public class ISO3166_1 {

    private final static HashMap<String, String> map = new HashMap<>();

    public static void init(Resources resources) {
        InputStream inputStream = resources.openRawResource(R.raw.iso3166_1_numeric);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line;
            while((line = br.readLine()) != null) {
                if (line.trim().length() < 4 || line.startsWith("#")){
                    continue;
                }
                map.put(line.substring(0, 3), line.substring(4));
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null){
                try {
                    br.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    public static String getCountryForCode(int code) {
        return getCountryForCode(String.valueOf(code));
    }

    public static String getCountryForCode(String code) {
        return map.get(code);
    }
}
