/* 
 * Copyright 2014 Ryan Jones
 * 
 * This file was modified from the original source:
 * https://code.google.com/p/nfcspy/
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

package org.docrj.smartcard.reader;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.content.res.XmlResourceParser;

final class ApduParser {

    static String parse(boolean isCmd, byte[] apdu) {
        if (isCmd) {
            return CMDS.search(0, apdu).name();
        } else {
            final int len = apdu.length;
            if (len > 1)
                return SWS.search(0, apdu[len - 2], apdu[len - 1]).name();
        }
        return "";
    }

    static void init(Context context) {
        if (CMDS != null && SWS != null)
            return;

        final XmlResourceParser xml = context.getResources().getXml(
                R.xml.apdu7816);

        try {
            // START__DOCUMENT
            xml.next();

            if (xml.next() == START_TAG
                    && "apdu".equalsIgnoreCase(xml.getName())) {

                while (xml.next() != END_DOCUMENT) {

                    Apdu7816 cmds = readTag(Apdu7816.CMDS.class, xml);
                    if (cmds != null) {
                        CMDS = cmds;
                        continue;
                    }

                    Apdu7816 sws = readTag(Apdu7816.SWS.class, xml);
                    if (sws != null) {
                        SWS = sws;
                        continue;
                    }

                    break;

                }
            }
        } catch (Exception e) {
        } finally {
            if (xml != null)
                xml.close();
        }

        if (CMDS == null)
            CMDS = new Apdu7816.CMDS();

        if (SWS == null)
            SWS = new Apdu7816.SWS();
    }

    @SuppressWarnings("unchecked")
    static Apdu7816 readTag(Class<? extends Apdu7816> clazz,
            XmlResourceParser xml) throws Exception {
        if (xml.getEventType() != START_TAG)
            return null;

        final String thisTag = xml.getName();
        final String testTag = (String) clazz.getDeclaredField("TAG").get(null);
        if (!thisTag.equalsIgnoreCase(testTag))
            return null;

        final String apduName = xml.getAttributeValue(null, "name");
        final String apduVal = xml.getAttributeValue(null, "val");
        final ArrayList<Apdu7816> list = new ArrayList<Apdu7816>();

        final Object child = clazz.getDeclaredField("SUB").get(null);
        while (true) {
            int event = xml.next();
            if (event == END_DOCUMENT)
                break;

            if (event == END_TAG && thisTag.equalsIgnoreCase(xml.getName()))
                break;

            if (child != null) {
                Apdu7816 apdu = readTag((Class<? extends Apdu7816>) child, xml);
                if (apdu != null)
                    list.add(apdu);
            }
        }

        final Apdu7816[] sub;
        if (list.isEmpty())
            sub = null;
        else
            sub = list.toArray(new Apdu7816[list.size()]);

        final Apdu7816 ret = clazz.newInstance();

        ret.init(parseValue(apduVal), apduName, sub);

        return ret;
    }

    private static byte parseValue(String strVal) {
        if (strVal != null && strVal.length() > 0) {

            try {
                return (byte) (Integer.parseInt(strVal, 16) & 0xFF);
            } catch (Exception e) {
            }
        }
        return (byte) 0;
    }

    private static Apdu7816 CMDS;
    private static Apdu7816 SWS;

    @SuppressWarnings("unused")
    private static class Apdu7816 implements Comparator<Apdu7816> {

        final static class CMDS extends Apdu7816 {
            final static String TAG = "cmds";
            final static Object SUB = CLS.class;
        }

        final static class SWS extends Apdu7816 {
            final static String TAG = "sws";
            final static Object SUB = SW1.class;
        }

        final static class CLS extends Apdu7816 {
            final static String TAG = "class";
            final static Object SUB = INS.class;

            int compare(Apdu7816 apdu) {
                if (apdu.getClass() != CLS.class) {
                    final byte val = this.val;
                    final byte oth = apdu.val;

                    if (val == 0) {
                        // class 0XXX XXXXb
                        if ((oth | 0x7F) == 0x7F)
                            return 0;
                    } else if (val == 1) {
                        // class 1XXX XXXXb
                        if ((oth & 0x80) == 0x80)
                            return 0;
                    } else {
                        // class as val
                        if (val == oth)
                            return 0;
                    }
                }
                return super.compare(apdu);
            }
        }

        final static class INS extends Apdu7816 {
            final static String TAG = "ins";
            final static Object SUB = P1.class;
        }

        final static class P1 extends Apdu7816 {
            final static String TAG = "p1";
            final static Object SUB = P2.class;
        }

        final static class P2 extends Apdu7816 {
            final static String TAG = "p2";
            final static Object SUB = null;
        }

        final static class SW1 extends Apdu7816 {
            final static String TAG = "sw1";
            final static Object SUB = SW2.class;
        }

        final static class SW2 extends Apdu7816 {
            final static String TAG = "sw2";
            final static Object SUB = null;
        }

        int compare(Apdu7816 apdu) {
            return (this.val & 0xFF) - (apdu.val & 0xFF);
        }

        String name() {
            return name == null ? "" : name;
        }

        Apdu7816 search(int start, byte... val) {

            final Apdu7816[] sub = this.sub;
            if (sub != null && start < val.length) {
                Apdu7816 cp = comparator;
                cp.val = val[start];
                int i = Arrays.binarySearch(sub, cp, cp);
                if (i >= 0)
                    return sub[i].search(++start, val);
            }

            return this;
        }

        @Override
        public int compare(Apdu7816 lhs, Apdu7816 rhs) {
            return lhs.compare(rhs);
        }

        void init(byte val, String name, Apdu7816[] sub) {
            this.val = val;
            this.name = name;
            this.sub = sub;

            if (sub != null)
                Arrays.sort(sub, comparator);
        }

        protected Apdu7816[] sub;
        protected byte val;
        protected String name;
        protected static Apdu7816 comparator = new Apdu7816();
    }
}
