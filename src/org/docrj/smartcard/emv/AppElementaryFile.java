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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppElementaryFile {

    private int mSfi;
    private int mStartRecord;
    private int mEndRecord;
    private int mRecordsInvolvedInOfflineDataAuth;
    // LinkedHashMap to maintain insertion order
    private Map<Integer, Record> records = new LinkedHashMap<Integer, Record>();

    public AppElementaryFile(byte[] data) {
        if (data.length != 4) {
            throw new SmartcardException("App Elementary File: length must be 4. Data length = " + data.length);
        }

        mSfi = data[0] >>> 3; 

        mStartRecord = data[1] & 0xFF;
        if (mStartRecord == 0) {
            throw new SmartcardException("Applicaton Elementary File: start record number cannot be 0");
        }
        mEndRecord = data[2] & 0xFF;
        if (mEndRecord < mStartRecord) {
            throw new SmartcardException("Applicaton Elementary File: end record number (" + mEndRecord +
                ") < start record number (" + mStartRecord + ")");
        }
        mRecordsInvolvedInOfflineDataAuth = data[3] & 0xFF;
    }

    public int getSfi() {
        return mSfi;
    }

    public int getStartRecordNum() {
        return mStartRecord;
    }

    public int getEndRecordNum() {
        return mEndRecord;
    }

    public int getNumRecordsInvolvedInOfflineDataAuth() {
        return mRecordsInvolvedInOfflineDataAuth;
    }

    public void setRecord(int recordNum, Record record) {
        Integer recordNumber = Integer.valueOf(recordNum);
        if (records.containsKey(recordNumber)) {
            throw new IllegalArgumentException("Record number " + recordNum + " already added: " + record);
        }
        records.put(Integer.valueOf(recordNum), record);
    }

    public Record getRecord(int recordNum) {
        return records.get(Integer.valueOf(recordNum));
    }

    public Collection<Record> getRecords() {
        return Collections.unmodifiableCollection(records.values());
    }
}
