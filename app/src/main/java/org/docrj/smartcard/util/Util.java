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

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

public class Util {

    public static String getSpaces(int length) {
        StringBuilder buf = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            buf.append(" ");
        }

        return buf.toString();
    }

    public static String prettyPrintHex(String in, int indent, boolean wrapLines) {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            buf.append(c);

            int nextPos = i+1;
            if (wrapLines && nextPos % 32 == 0 && nextPos != in.length()) {
                buf.append("\n").append(getSpaces(indent));
            } else if (nextPos % 2 == 0 && nextPos != in.length()) {
                //buf.append(" ");
            }
        }
        return buf.toString();
    }

    public static String prettyPrintHex(String in, int indent){
        return prettyPrintHex(in, indent, true);
    }

    public static String prettyPrintHex(byte[] data, int indent) {
        return Util.prettyPrintHex(Util.byteArrayToHexString(data), indent, true);
    }
    
    public static String prettyPrintHex(byte[] data) {
        return Util.prettyPrintHex(Util.byteArrayToHexString(data), 0, true);
    }
    
    public static String prettyPrintHex(byte[] data, int startPos, int length) {
        return Util.prettyPrintHex(Util.byteArrayToHexString(data, startPos, length), 0, true);
    }

    public static String prettyPrintHexNoWrap(byte[] data) {
        return Util.prettyPrintHex(Util.byteArrayToHexString(data), 0, false);
    }
    
    public static String prettyPrintHexNoWrap(byte[] data, int startPos, int length) {
        return Util.prettyPrintHex(Util.byteArrayToHexString(data, startPos, length), 0, false);
    }
    
    public static String prettyPrintHexNoWrap(String in) {
        return Util.prettyPrintHex(in, 0, false);
    }

    public static String prettyPrintHex(String in) {
        return prettyPrintHex(in, 0, true);
    }

    public static String prettyPrintHex(BigInteger bi) {
        byte[] data = bi.toByteArray();
        if (data[0] == (byte) 0x00) {
            byte[] tmp = new byte[data.length - 1];
            System.arraycopy(data, 1, tmp, 0, data.length - 1);
            data = tmp;
        }
        return prettyPrintHex(data);
    }

    public static byte[] performRSA(byte[] dataBytes, byte[] expBytes, byte[] modBytes) {

        int inBytesLength = dataBytes.length;

        if (expBytes[0] >= (byte) 0x80) {
            //Prepend 0x00 to modulus
            byte[] tmp = new byte[expBytes.length + 1];
            tmp[0] = (byte) 0x00;
            System.arraycopy(expBytes, 0, tmp, 1, expBytes.length);
            expBytes = tmp;
        }

        if (modBytes[0] >= (byte) 0x80) {
            //Prepend 0x00 to modulus
            byte[] tmp = new byte[modBytes.length + 1];
            tmp[0] = (byte) 0x00;
            System.arraycopy(modBytes, 0, tmp, 1, modBytes.length);
            modBytes = tmp;
        }

        if (dataBytes[0] >= (byte) 0x80) {
            //Prepend 0x00 to signed data to avoid that the most significant bit is interpreted as the "signed" bit
            byte[] tmp = new byte[dataBytes.length + 1];
            tmp[0] = (byte) 0x00;
            System.arraycopy(dataBytes, 0, tmp, 1, dataBytes.length);
            dataBytes = tmp;
        }

        BigInteger exp = new BigInteger(expBytes);
        BigInteger mod = new BigInteger(modBytes);
        BigInteger data = new BigInteger(dataBytes);

        byte[] result = data.modPow(exp, mod).toByteArray();

        if (result.length == (inBytesLength+1) && result[0] == (byte)0x00) {
            //Remove 0x00 from beginning of array
            byte[] tmp = new byte[inBytesLength];
            System.arraycopy(result, 1, tmp, 0, inBytesLength);
            result = tmp;
        }

        return result;
    }
    
    public static byte[] calculateSHA1(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        return sha1.digest(data);
    }

    public static String byte2Hex(byte b) {
        String[] HEX_DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        int nb = b & 0xFF;
        int i_1 = (nb >>> 4) & 0xF;
        int i_2 = nb & 0xF;
        return HEX_DIGITS[i_1] + HEX_DIGITS[i_2];
    }

    public static String short2Hex(short s) {
        byte b1 = (byte) (s >>> 8);
        byte b2 = (byte) (s & 0xFF);
        return byte2Hex(b1) + byte2Hex(b2);
    }

    public static int byteToInt(byte b) {
        return (int) b & 0xFF;
    }

    public static int byteToInt(byte first, byte second) {
        int value = (first & 0xFF) << 8;
        value += second & 0xFF;
        return value;
    }

    public static short byte2Short(byte b1, byte b2) {
        return (short) ((b1 << 8) | (b2 & 0xFF));
    }

    public static String getFormattedNanoTime(long nano) {
        StringBuilder buf = new StringBuilder();
        buf.append((int) (nano / 1000000));
        buf.append("ms ");
        buf.append(nano % 1000000);
        buf.append("ns");
        return buf.toString();
    }
    
    public static byte[] getCurrentDateAsNumericEncodedByteArray(){
        SimpleDateFormat format = new SimpleDateFormat("yyMMdd", Locale.US);        
        return fromHexString(format.format(new Date()));
    }

    //This prints all non-control characters common to all parts of ISO/IEC 8859
    //See EMV book 4 Annex B: Table 36: Common Character Set
    public static String getSafePrintChars(byte[] byteArray) {
        if (byteArray == null) {
            return "";
//            throw new IllegalArgumentException("Argument 'byteArray' cannot be null");
        }
        return getSafePrintChars(byteArray, 0, byteArray.length);
    }
    
    public static String getSafePrintChars(byte[] byteArray, int startPos, int length) {
        if (byteArray == null) {
            return "";
//            throw new IllegalArgumentException("Argument 'byteArray' cannot be null");
        }
        if(byteArray.length < startPos+length){
            throw new IllegalArgumentException("startPos("+startPos+")+length("+length+") > byteArray.length("+byteArray.length+")");
        }
        StringBuilder buf = new StringBuilder();
        for (int i = startPos; i < startPos+length; i++) {
            if (byteArray[i] >= (byte) 0x20 && byteArray[i] < (byte) 0x7F) {
                buf.append((char) byteArray[i]);
            } else {
                buf.append(".");
            }
        }
        return buf.toString();
    }

    public static byte[] hexToBytes(String str) {
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2),
                    16);
        }
        return bytes;
    }    

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }    
    
    /**
     * Converts a byte array into a hex string.
     * @param byteArray the byte array source
     * @return a hex string representing the byte array
     */
    public static String byteArrayToHexString(final byte[] byteArray) {
        if (byteArray == null) {
            return "";
        }
        return byteArrayToHexString(byteArray, 0, byteArray.length);
    }
    
    public static String byteArrayToHexString(final byte[] byteArray, int startPos, int length) {
        if (byteArray == null) {
            return "";
        }
        if(byteArray.length < startPos+length){
            throw new IllegalArgumentException("startPos("+startPos+")+length("+length+") > byteArray.length("+byteArray.length+")");
        }
//        int readBytes = byteArray.length;
        StringBuilder hexData = new StringBuilder();
        int onebyte;
        for (int i = 0; i < length; i++) {
            onebyte = ((0x000000ff & byteArray[startPos+i]) | 0xffffff00);
            hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }

    public static String int2Hex(int i) {
        String hex = Integer.toHexString(i);
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        return hex;
    }

    public static String int2HexZeroPad(int i) {
        String hex = int2Hex(i);
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        return hex;
    }
    /**
     * The length of the returned array depends on the size of the int
     * @param value
     * @return
     */
    public static byte[] intToByteArray(int value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte one = (byte) (value >>> 24);
        byte two = (byte) (value >>> 16);
        byte three = (byte) (value >>> 8);
        byte four = (byte) (value);

        boolean found = false;

        if (one > 0x00) {
            baos.write(one);
            found = true;
        }
        if (found || two > 0x00) {
            baos.write(two);
            found = true;
        }

        if (found || three > 0x00) {
            baos.write(three);
        }

        baos.write(four);

        return baos.toByteArray();
    }

    /**
     * Returns a byte array with length = 4
     * @param value
     * @return
     */
    public static byte[] intToByteArray4(int value) {
        return new byte[]{
                    (byte) (value >>> 24),
                    (byte) (value >>> 16),
                    (byte) (value >>> 8),
                    (byte) value};
    }

    public static int byteArrayToInt(byte[] byteArray) {
        return byteArrayToInt(byteArray, 0, byteArray.length);
    }
    
    public static int byteArrayToInt(byte[] byteArray, int startPos, int length) {
        if (byteArray == null) {
            throw new IllegalArgumentException("Parameter 'byteArray' cannot be null");
        }
        if (length <= 0 || length > 4) {
            throw new IllegalArgumentException("Length must be between 1 and 4. Length = " + length);
        }
        if (length == 4 && Util.isBitSet(byteArray[startPos], 8)){
            throw new IllegalArgumentException("Signed bit is set (leftmost bit): " + Util.byte2Hex(byteArray[startPos]));
        }
        int value = 0;
        for (int i = 0; i < length; i++) {
            value += ((byteArray[startPos+i] & 0xFF) << 8 * (length - i - 1));
        }
        return value;
    }
    
    public static long byteArrayToLong(byte[] byteArray, int startPos, int length) {
        if (byteArray == null) {
            throw new IllegalArgumentException("Parameter 'byteArray' cannot be null");
        }
        if (length <= 0 || length > 8) {
            throw new IllegalArgumentException("Length must be between 1 and 4. Length = " + length);
        }
        if (length == 8 && Util.isBitSet(byteArray[startPos], 8)){
            throw new IllegalArgumentException("Signed bit is set (leftmost bit): " + Util.byte2Hex(byteArray[startPos]));
        }
        long value = 0;
        for (int i = 0; i < length; i++) {
            value += ((byteArray[startPos+i] & (long)0xFF) << 8 * (length - i - 1));
        }
        return value;
    }

    public static byte[] fromHexString(String encoded) {
        encoded = removeSpaces(encoded);
        if (encoded.length() == 0){
            return new byte[0];
        }
        if ((encoded.length() % 2) != 0) {
            throw new IllegalArgumentException("Input string must contain an even number of characters: "+encoded);
        }
        final byte result[] = new byte[encoded.length() / 2];
        final char enc[] = encoded.toCharArray();
        for (int i = 0; i < enc.length; i += 2) {
            StringBuilder curr = new StringBuilder(2);
            curr.append(enc[i]).append(enc[i + 1]);
            result[i / 2] = (byte) Integer.parseInt(curr.toString(), 16);
        }
        return result;
    }

    public static String removeCRLFTab(String s) {
        StringTokenizer st = new StringTokenizer(s, "\r\n\t", false);
        StringBuilder buf = new StringBuilder();
        while (st.hasMoreElements()) {
            buf.append(st.nextElement());
        }
        return buf.toString();
    }

    public static String removeSpaces(String s) {
        return s.replaceAll(" ", "");
    }

    public static String readInputStreamToString(InputStream is, String encoding) throws IOException {
        InputStreamReader input = new InputStreamReader(is, encoding);
        final int CHARS_PER_PAGE = 5000; //counting spaces
        final char[] buffer = new char[CHARS_PER_PAGE];
        StringBuilder output = new StringBuilder(CHARS_PER_PAGE);
        for (int read = input.read(buffer, 0, buffer.length);
                read != -1;
                read = input.read(buffer, 0, buffer.length)) {
            output.append(buffer, 0, read);
        }

        String text = output.toString();
        return text;
    }

    public static void writeStringToFile(String string, String fileName, boolean append) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName, append));
        out.write(string);
        out.close();
    }
    
    /**
     * Binary Coded Decimal (BCD)
     * @param val
     * @return 
     */
    public static byte[] intToBinaryEncodedDecimalByteArray(int val){
        String str = String.valueOf(val);
        if(str.length() % 2 != 0){
            str = "0"+str;
        }
        return Util.fromHexString(str);
    }

    /**
     * This method converts the literal hex representation of a byte to an int.
     * eg 0x70 = 70 (int)
     * @param b
     */
    public static int binaryCodedDecimalToInt(byte b) {
        String hex = Util.byte2Hex(b);
        try {
            return Integer.parseInt(hex);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("The hex representation of argument b must be digits", ex);
        }
    }

    /**
     * This method converts the literal hex representation of a decimal 
     * encoded in 1-5 bytes to an int.
     * The value should not be larger than Integer.MAX_VALUE
     *  
     * eg 0x70 = 70 (decimal)
     * eg 0x21 47 48 36 47 = 2147483647 (decimal)
     * @param hex
     */
    public static int binaryHexCodedDecimalToInt(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Param hex cannot be null");
        }
        hex = Util.removeSpaces(hex);
        if (hex.length() > 10) {
            throw new IllegalArgumentException("There must be a maximum of 5 hex octets. hex=" + hex);
        }
        try {
            return Integer.parseInt(hex);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Argument hex must be all digits. hex="+hex, ex);
        }
    }
    
    /**
     * This method converts a 1-5 byte BCD to an int.
     * eg 0x7099 = 7099 (int)
     * @param bcdArray
     */
    public static int binaryHexCodedDecimalToInt(byte[] bcdArray) {
        if (bcdArray == null) {
            throw new IllegalArgumentException("Param bcdArray cannot be null");
        }
        return binaryHexCodedDecimalToInt(Util.byteArrayToHexString(bcdArray));
    }

    /**
     * This returns a String with length = 8
     * @param val
     * @return
     */
    public static String byte2BinaryLiteral(byte val) {
        String s = Integer.toBinaryString(Util.byteToInt(val));
        if (s.length() < 8) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8 - s.length(); i++) {
                sb.append('0');
            }
            sb.append(s);
            s = sb.toString();
        }
        return s;
    }

    /**
     * Returns a bitset containing the values in bytes.
     * The byte-ordering of bytes must be big-endian which means the most significant bit is in element 0.
     *
     * @param bytes
     * @return
     */
    public static BitSet byteArray2BitSet(byte[] bytes) {
        BitSet bits = new BitSet();
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    /* Returns a byte array of at least length 1.
     * The most significant bit in the result is guaranteed not to be a 1
     * (since BitSet does not support sign extension).
     * The byte-ordering of the result is big-endian which means the most significant bit is in element 0.
     * The bit at index 0 of the bit set is assumed to be the least significant bit.
     */
    public static byte[] bitSet2ByteArray(BitSet bits) {
        byte[] bytes = new byte[bits.length() / 8 + 1];
        for (int i = 0; i < bits.length(); i++) {
            if (bits.get(i)) {
                bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
            }
        }
        return bytes;
    }

    /**
     *
     * @param val
     * @param bitPos The leftmost bit is 8 (the most significant bit)
     * @return
     */
    public static boolean isBitSet(byte val, int bitPos) {
        if (bitPos < 1 || bitPos > 8) {
            throw new IllegalArgumentException("parameter 'bitPos' must be between 1 and 8. bitPos=" + bitPos);
        }
        if ((val >>> (bitPos - 1) & 0x1) == 1) {
            return true;
        }
        return false;
    }
    
//    /**
//     *
//     * @param val
//     * @return
//     */
//    public static int getBitsSetCount(byte val) {
//        int numBitsSet = 0;
//        for(int i=1; i<=8; i++){
//            if(Util.isBitSet(val, i)){
//                numBitsSet++;
//            }
//        }
//        return numBitsSet;
//    }

    /**
     *
     * @param data
     * @param bitPos The leftmost bit is 8
     * @param on
     * @return
     */
    public static byte setBit(byte data, int bitPos, boolean on) {
        if (bitPos < 1 || bitPos > 8) {
            throw new IllegalArgumentException("parameter 'bitPos' must be between 1 and 8. bitPos=" + bitPos);
        }
        if (on) {
            // set bit
            return data |= 1 << (bitPos - 1);
        } else {
            // clear bit
            return data &= ~(1 << (bitPos - 1));
        }
    }

    public static byte[] generateRandomBytes(int numBytes) {
        // TODO: get bytes from a hardware RNG, or set seed
        byte[] rndBytes = new byte[numBytes];
        SecureRandom random = new SecureRandom();
        random.nextBytes(rndBytes);
        return rndBytes;
    }

    public static InputStream loadResource(Class<?> cls, String path){
        return cls.getResourceAsStream(path);
    }

    /**
     * Copies the specified array, prepending 0x00, or cutting off MSBytes if necessary
     * @param original
     * @param newLength
     * @return 
     */
    public static byte[] resizeArray(byte[] original, int newLength) {
        if(original == null){
            throw new IllegalArgumentException("byte array cannot be null");
        }
        if(newLength < 0){
            throw new IllegalArgumentException("Illegal new length: "+newLength+". Must be >= 0");
        }
        if(newLength == 0){
            return new byte[0];
        }
        byte[] tmp = new byte[newLength];
        
        int srcPos  = tmp.length > original.length ? 0 : original.length - tmp.length;
        int destPos = tmp.length > original.length ? tmp.length - original.length : 0;
        int length  = tmp.length > original.length ? original.length : tmp.length;
        
        System.arraycopy(original, srcPos, tmp, destPos, length);
        
        return tmp;
    }
    
    public static byte[] copyByteArray(byte[] array2Copy){
//        byte[] copy = new byte[array2Copy.length];
//        System.arraycopy(array2Copy, 0, copy, 0, array2Copy.length);
//        return copy;
        if (array2Copy == null) {
            //return new byte[0] instead?
            throw new IllegalArgumentException("Argument 'array2Copy' cannot be null");
        }
        return copyByteArray(array2Copy, 0, array2Copy.length);
    }
    
    public static byte[] copyByteArray(byte[] array2Copy, int startPos, int length){
        if (array2Copy == null) {
            //return new byte[0] instead?
            throw new IllegalArgumentException("Argument 'array2Copy' cannot be null");
        }
        if(array2Copy.length < startPos+length){
            throw new IllegalArgumentException("startPos("+startPos+")+length("+length+") > byteArray.length("+array2Copy.length+")");
        }
        byte[] copy = new byte[array2Copy.length];
        System.arraycopy(array2Copy, startPos, copy, 0, length);
        return copy;
    }
    
    public static String getStackTrace(Throwable t){
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static Class<?> getCallerClass(int i) {
        Class<?>[] classContext = new SecurityManager() {
            @Override public Class<?>[] getClassContext() {
                return super.getClassContext();
            }
        }.getClassContext();
        if (classContext != null) {
            for (int j = 0; j < classContext.length; j++) {
                if (classContext[j] == Util.class) {
                    return classContext[i+j];
                }
            }
        } else {
            // SecurityManager.getClassContext() returns null on Android 4.0
            try {
                StackTraceElement[] classNames = Thread.currentThread().getStackTrace();
                for (int j = 0; j < classNames.length; j++) {
                    if (Class.forName(classNames[j].getClassName()) == Util.class) {
                        return Class.forName(classNames[i+j].getClassName());
                    }
                }
            } catch (ClassNotFoundException e) { }
        }
        return null;
    }
    
    public static String decodeOID(byte[] enc){
        StringBuilder sb = new StringBuilder();
       
        //First OID Component (standard)
        //0: ITU-T
        //1: ISO
        //2: joint-iso-itu-t
        
        //Second OID Component (part in a multi part standard)
        //0: standard
        //1: registration-authority
        //2: member-body
        //3: identified-organization


        long firstSubidentifier = 0;

        int i=0;
        while(Util.isBitSet(enc[i], 8)){
            firstSubidentifier = (firstSubidentifier << 7) | (enc[i] & 0x7f);
            i++;
        }
        firstSubidentifier = (firstSubidentifier << 7) | (enc[i] & 0x7f);
        i++;

        if(firstSubidentifier >= 80){
            long firstOIDComp = 2;
            long secondOIDComp = firstSubidentifier - 80;
            sb.append(firstOIDComp).append(".").append(secondOIDComp);
        }else{
            long secondOIDComp = firstSubidentifier % 40;
            long firstOIDComp = (firstSubidentifier - secondOIDComp)/40;
            sb.append(firstOIDComp).append(".").append(secondOIDComp);
        }
              
        for(; i<enc.length; i++){
            sb.append(".");
            long subIdentifier = 0;
            
            while(Util.isBitSet(enc[i], 8)){
                subIdentifier = (subIdentifier << 7) | (enc[i] & 0x7f);
                i++;
            }
            subIdentifier = (subIdentifier << 7) | (enc[i] & 0x7f);
            sb.append(subIdentifier);
            
        }
        
        String oid = sb.toString();
        String desc = getOIDDescription(oid);
        return oid + ((desc!=null && !desc.isEmpty())?" ("+desc +")":"");
    }
    
    //Simple OID registry
    //See: http://www.oid-info.com/
    public static String getOIDDescription(String oid){

//        1.2.840 - one of 2 US country OIDs 
//        1.2.840.114283 - Global Platform
        
//        1.3.6.1 - the Internet OID 
//        1.3.6.1.4.1 - IANA-assigned company OIDs, used for private MIBs and such things 
//        1.3.6.1.4.1.42 - Sun Microsystems
//        1.3.6.1.4.1.42.2 - Sun Products
//        1.3.6.1.4.1.42.2.110 - java[XML]software
//        1.3.6.1.4.1.42.2.110.1.2 - (Unknown - Java Card?)

        if(oid.startsWith("1.2.840.114283.1")){
            return "Global Platform - Card Recognition Data";
        }
        if(oid.startsWith("1.2.840.114283.2")){
            return "Global Platform v"+oid.substring(17);
        }
        if(oid.startsWith("1.2.840.114283.3")){
            return "Global Platform - Card Identification Scheme";
        }
        if(oid.startsWith("1.2.840.114283.4")){
            return "Global Platform SCP "+oid.substring(17, 18) + " implementation option 0x"+Util.int2Hex(Integer.parseInt(oid.substring(19)));
        }
        if(oid.startsWith("1.2.840.114283")){
            return "Global Platform";
        }
        if(oid.startsWith("1.2.840")){
            return "USA";
        }
        if(oid.startsWith("1.3.6.1.4.1.42.2.110.1.2")){
            return "Sun Microsystems - Java Card ?";
        }
        if(oid.startsWith("1.3.6.1.4.1.42.2")){
            return "Sun Microsystems - Products";
        }
//        if(oid.startsWith("1.3.656.840."))
        //JCOP includes GP refinements according to Visa GP 2.1.1 specification. 
        //This tag is populated accordingly (Visa specific). 
        //The last number tells you what configuration it is (3: SSD + PKI, 2: PKI, 1: just symmetric crypto). 
        //Unfortunately this standard is not open.
        

        return "";
    }

    public static void main(String[] args) {        
//        System.out.println(Util.isBitSet((byte) 0x5f, 2)); // 0101 1111
//        System.out.println(Util.isBitSet((byte) 0x9f, 2)); // 1001 1111
//
//        System.out.println(Util.byte2Short((byte) 0x6F, (byte) 0xEF));
//        System.out.println(Util.short2Hex(Util.byte2Short((byte) 0x6F, (byte) 0xEF)));
//
//        System.out.println(Util.byteArrayToInt(new byte[]{(byte) 0x6F, (byte) 0xEF}));
//        System.out.println(Util.byteArrayToHexString(Util.intToByteArray(28655)));
//
//        System.out.println(Util.byte2BinaryLiteral((byte) 0x00));
//        System.out.println(Util.byte2BinaryLiteral((byte) 0x3F));
//        System.out.println(Util.byte2BinaryLiteral((byte) 0x80));
//        System.out.println(Util.byte2BinaryLiteral((byte) 0xAA));
//        System.out.println(Util.byte2BinaryLiteral((byte) 0xFF));
//
//        System.out.println(Util.byte2BinaryLiteral((byte) 0x8A));
//        System.out.println(Util.byte2BinaryLiteral(Util.setBit((byte) 0x8A, 5, true)));
//        System.out.println(Util.byte2BinaryLiteral(Util.setBit((byte) 0x8A, 8, false)));
//        
//        System.out.println(Util.byteArrayToLong(Util.fromHexString("7f ff ff ff ff ff ff ff"), 0, 8));
//        System.out.println(Util.byteArrayToLong(Util.fromHexString("22 18 09 04 0b 00 e0 30 23 07 00 00 00 42 d2 85 4e 23 07 00 00 00 00 21 69 42"), 13, 4));
        System.out.println("1.2.840.114283.1 : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 01")));
        System.out.println("1.2.840.114283.2.2.1.1 : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 02 02 01 01")));
        System.out.println("1.2.840.114283.4.XXXX : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 04 02 15"))); //JCOP 31
        System.out.println("1.2.840.114283.4.XXXX : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 04 01 05"))); //JCOP 31
        
        System.out.println("Sun Microsystems : " + decodeOID(Util.fromHexString("2b 06 01 04 01 2a 02 6e 01 02")));
        System.out.println("Unknown : " + decodeOID(Util.fromHexString("2b 85 10 86 48 64 02 01 03")));
        System.out.println("{2 100 3} : " + decodeOID(Util.fromHexString("813403")));
        
        System.out.println(Util.prettyPrintHexNoWrap(Util.resizeArray(new byte[]{0x01}, 0)));
        System.out.println(Util.prettyPrintHexNoWrap(Util.resizeArray(new byte[]{0x01}, 1)));
        System.out.println(Util.prettyPrintHexNoWrap(Util.resizeArray(new byte[]{0x01}, 2)));
        
        System.out.println(Util.prettyPrintHexNoWrap(Util.resizeArray(new byte[]{0x01, 0x02}, 1)));
        System.out.println(Util.prettyPrintHexNoWrap(Util.resizeArray(new byte[]{0x01, 0x02}, 4)));
    }
}
