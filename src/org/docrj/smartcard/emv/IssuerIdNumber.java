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

import java.util.Arrays;
import org.docrj.smartcard.util.Util;

/**
 *
 * A 6-digit number that identifies the major industry and the card issuer 
 * and that forms the first part of the Primary Account Number (PAN)
 */
public class IssuerIdNumber {
    
    byte[] iinBytes;
    
    public IssuerIdNumber(byte[] iinBytes) {
        if(iinBytes == null) {
            throw new NullPointerException("Param iinBytes cannot be null");
        }
        if(iinBytes.length != 3){
            throw new IllegalArgumentException("Param iinBytes must have length 3, but was "+iinBytes.length);
        }
        this.iinBytes = Arrays.copyOf(iinBytes, iinBytes.length);
        
    }
    
    public IssuerIdNumber(int iin) {
        if(iin < 0 || iin > 1000000) {
            throw new IllegalArgumentException("IIN must be between 0 and 999999, but was "+iin);
        }
        byte[] tmp = Util.intToBinaryEncodedDecimalByteArray(iin);
        if(tmp.length != 6){
            iinBytes = Util.resizeArray(tmp, 6);
        } else {
            iinBytes = tmp;
        }
    }
    
    public int getValue(){
        return Util.binaryHexCodedDecimalToInt(iinBytes);
    }
    
    public byte[] getBytes(){
        return Arrays.copyOf(iinBytes, iinBytes.length);
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof IssuerIdNumber)){
            return false;
        }
        IssuerIdNumber that = (IssuerIdNumber)obj;
        if(this == that){
            return true;
        }
        if(Arrays.equals(this.getBytes(), that.getBytes())){
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Arrays.hashCode(this.iinBytes);
        return hash;
    }
}
