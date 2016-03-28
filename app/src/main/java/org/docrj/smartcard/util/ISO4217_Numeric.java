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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * ISO 4217
 * ISO 3-digit Currency Code
 *
 * http://www.iso.org/iso/support/faqs/faqs_widely_used_standards/widely_used_standards_other/currency_codes/currency_codes_list-1.htm
 *
 * java.util.Currency is pretty useless in java 1.6. Must wait for java 1.7 to get the methods:
 * getDisplayName()
 * getNumericCode()
 * getAvailableCurrencies()
 */
public class ISO4217_Numeric {

    private static final HashMap<String, Currency> code2CurrencyMap = new HashMap<>();
    private static final HashMap<String, Integer> currencyCode2NumericMap = new HashMap<>();

    public static void init(Resources resources) {
        InputStream inputStream = resources.openRawResource(R.raw.iso4217_numeric);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() <= 0 || line.startsWith("#")) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(line, ",");
                String numericCodeStr = st.nextToken();
                String currencyCodeStr = st.nextToken();
                String displayName = st.nextToken();
                int numericCode = Integer.parseInt(numericCodeStr);
                code2CurrencyMap.put(numericCodeStr, new Currency(numericCode, currencyCodeStr, displayName));
                currencyCode2NumericMap.put(currencyCodeStr, numericCode);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }
    }

    public static String getCurrencyNameForCode(int code) {

        return getCurrencyNameForCode(String.valueOf(code));
    }

    public static String getCurrencyNameForCode(String code) {
        Currency c = code2CurrencyMap.get(code);
        if (c == null) {
            return null;
        }
        return c.getDisplayName();
    }

    public static Currency getCurrencyForCode(int code) {
        return code2CurrencyMap.get(String.valueOf(code));
    }

    public static Integer getNumericCodeForCurrencyCode(String currencyCode) {
        return currencyCode2NumericMap.get(currencyCode);
    }

    public static List<Integer> getNumericCodeForLocale(final Locale locale) {
        List<Integer> codeList = new ArrayList<Integer>();
        if (locale.getCountry() == null || locale.getCountry().length() != 2) {
            //We have no country! Might find more than 1 match
            for (Locale l : Locale.getAvailableLocales()) {
                if (l.getLanguage().equals(locale.getLanguage()) && l.getCountry() != null && l.getCountry().length() == 2) {
                    String currencyCode = java.util.Currency.getInstance(l).getCurrencyCode();
                    codeList.add(ISO4217_Numeric.getNumericCodeForCurrencyCode(currencyCode));
                }
            }
        }else if (locale.getCountry() != null && locale.getCountry().length() == 2) {
            String currencyCode = java.util.Currency.getInstance(locale).getCurrencyCode();
            codeList.add(ISO4217_Numeric.getNumericCodeForCurrencyCode(currencyCode));
        }
        return codeList;
    }

    public static class Currency {

        int numericCode;
        String code;
        String displayName;

        Currency(int numericCode, String code, String displayName) {
            this.numericCode = numericCode;
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static void main(String[] args) {
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(578));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(955));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(999));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(998));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(1000));
        System.out.println(ISO4217_Numeric.getNumericCodeForCurrencyCode("USD"));
    }
}
