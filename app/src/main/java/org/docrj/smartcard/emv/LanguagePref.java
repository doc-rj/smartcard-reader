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

package org.docrj.smartcard.emv;

import org.docrj.smartcard.iso7816.SmartcardException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LanguagePref {

    private List<Locale> prefs;

    public LanguagePref(byte[] data) {
        if (data.length < 2 || data.length > 8 || data.length % 2 != 0) {
            throw new SmartcardException("Array length must be an even number between 2 (inclusive) and 8 (inclusive). Length=" + data.length);
        }
        prefs = new ArrayList<Locale>();

        int numLang = data.length / 2;

        for (int i = 0; i < numLang; i++) {
            String s = String.valueOf((char) data[i * 2]) + String.valueOf((char) data[i * 2 + 1]);
            prefs.add(new Locale(s));
        }

    }
    
    public List<Locale> getLocales(){
        return prefs;
    }
    
    public Locale getPreferredLocale(){
        return prefs.get(0);
    }
}
