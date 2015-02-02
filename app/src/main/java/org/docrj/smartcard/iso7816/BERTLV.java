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

package org.docrj.smartcard.iso7816;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.docrj.smartcard.util.Util;

public class BERTLV {

    private Tag tag;
    private byte[] rawEncodedLengthBytes;
    private byte[] valueBytes;
    private int length;

    /**
     *
     * @param tag
     * @param length contains the number of value bytes (parsed from the rawEncodedLengthBytes)
     * @param rawLengthBytes the raw encoded length bytes
     * @param valueBytes
     */
    public BERTLV(Tag tag, int length, byte[] rawEncodedLengthBytes, byte[] valueBytes) {
        if (length != valueBytes.length) {
            //Assert
            throw new IllegalArgumentException("length != bytes.length");
        }
        this.tag = tag;
        this.rawEncodedLengthBytes = rawEncodedLengthBytes;
        this.valueBytes = valueBytes;
        this.length = length;
    }

    public BERTLV(Tag tag, byte[] valueBytes) {
        this.tag = tag;
        this.rawEncodedLengthBytes = encodeLength(valueBytes.length);
        this.valueBytes = valueBytes;
        this.length = valueBytes.length;
    }
    
    public static byte[] encodeLength(int length){
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public byte[] getTagBytes() {
        return tag.getTagBytes();
    }

    public byte[] getRawEncodedLengthBytes() {
        return rawEncodedLengthBytes;
    }

    public byte[] getValueBytes() {
        return valueBytes;
    }

    public ByteArrayInputStream getValueStream() {
        return new ByteArrayInputStream(valueBytes);
    }

    public byte[] toBERTLVByteArray() {
        byte[] tagBytes = tag.getTagBytes();
        ByteArrayOutputStream stream =
            new ByteArrayOutputStream(tagBytes.length + rawEncodedLengthBytes.length + valueBytes.length);
        stream.write(tagBytes, 0, tagBytes.length);
        stream.write(rawEncodedLengthBytes, 0, rawEncodedLengthBytes.length);
        stream.write(valueBytes, 0, valueBytes.length);
        return stream.toByteArray();
    }

    @Override
    public String toString() {
        return "BER-TLV[" + Util.byteArrayToHexString(getTagBytes()) + ", " + Util.int2Hex(length) +
            " (raw " + Util.byteArrayToHexString(rawEncodedLengthBytes) + ")" + ", " +
            Util.byteArrayToHexString(valueBytes) + "]";
    }

    public Tag getTag() {
        return tag;
    }

    public int getLength() {
        return length;
    }
    
    public static void main(String[] args) throws Exception {
//> 6f 23 -- File Control Information (FCI) Template
//>       84 0e -- Dedicated File (DF) Name
//>             32 50 41 59 2e 53 59 53 2e 44 44 46 30 31 (BINARY)
//>       a5 11 -- File Control Information (FCI) Proprietary Template
//>             bf 0c 0e -- File Control Information (FCI) Issuer Discretionary Data
//>                      61 0c -- Application Template
//>                            4f 07 -- Application Identifier (AID) - card
//>                                  a0 00 00 00 04 10 10 (BINARY)
//>                            87 01 -- Application Priority Indicator
//>                                  01 (BINARY)
        
//        BERTLV fciTemplate = new BERTLV(EMVTags.FCI_TEMPLATE, )
                
        //TODO create PPSE.toBytes(byte[] outBuf, short len)

    }
}
