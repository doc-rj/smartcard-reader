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

/**
 * Application Record
 */
public class Record {

    private byte[] rawDataIncTag;
    private boolean isInvolvedInOfflineDataAuthentication = false;
    private int recordNumber;

    public Record(byte[] rawDataIncTag, int recordNumber, boolean isInvolvedInOfflineDataAuthentication){
        this.rawDataIncTag = rawDataIncTag;
        this.recordNumber = recordNumber;
        this.isInvolvedInOfflineDataAuthentication = isInvolvedInOfflineDataAuthentication;
    }

    public byte[] getRawData(){
        return Arrays.copyOf(rawDataIncTag, rawDataIncTag.length);
    }

    public boolean isInvolvedInOfflineDataAuthentication(){
        return isInvolvedInOfflineDataAuthentication;
    }

    public int getRecordNumber(){
        return recordNumber;
    }
}
