/*
 * Copyright 2014 Ryan Jones
 * Copyright 2010 Giesecke & Devrient GmbH.
 * 
 * This file was modified from the original source:
 * https://code.google.com/p/seek-for-android/
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

import java.security.AccessControlException;

public class ResponseApdu {

    protected int mSw1 = 0x00;
    protected int mSw2 = 0x00;

    protected byte[] mData = new byte[0];
    protected byte[] mBytes = new byte[0];

    public ResponseApdu(byte[] respApdu) {
        if (respApdu.length < 2) {
            return;
        }
        if (respApdu.length > 2) {
            mData = new byte[respApdu.length - 2];
            System.arraycopy(respApdu, 0, mData, 0, respApdu.length - 2);
        }
        mSw1 = 0x00FF & respApdu[respApdu.length - 2];
        mSw2 = 0x00FF & respApdu[respApdu.length - 1];
        mBytes = respApdu;
    }

    public int getSW1() {
        return mSw1;
    }

    public int getSW2() {
        return mSw2;
    }

    public int getSW1SW2() {
        return (mSw1 << 8) | mSw2;
    }

    public byte[] getData() {
        return mData;
    }

    public byte[] toBytes() {
        return mBytes;        
    }

    public void checkLengthAndStatus(int length, int sw1sw2, String message)
            throws AccessControlException {
        if (getSW1SW2() != sw1sw2 || mData.length != length) {
            throw new AccessControlException("ResponseApdu is wrong at "
                    + message);
        }
    }

    public void checkLengthAndStatus(int length, int[] sw1sw2List,
            String message) throws AccessControlException {
        if (mData.length != length) {
            throw new AccessControlException("ResponseApdu is wrong at "
                    + message);
        }
        for (int sw1sw2 : sw1sw2List) {
            if (getSW1SW2() == sw1sw2) {
                return; // sw1sw2 matches => return
            }
        }
        throw new AccessControlException("ResponseApdu is wrong at " + message);
    }

    public void checkStatus(int[] sw1sw2List, String message)
            throws AccessControlException {
        for (int sw1sw2 : sw1sw2List) {
            if (getSW1SW2() == sw1sw2) {
                return; // sw1sw2 matches => return
            }
        }
        throw new AccessControlException("ResponseApdu is wrong at " + message);
    }

    public void checkStatus(int sw1sw2, String message)
            throws AccessControlException {
        if (getSW1SW2() != sw1sw2) {
            throw new AccessControlException("ResponseApdu is wrong at "
                    + message);
        }
    }

    public boolean isStatus(int sw1sw2) {
        if (getSW1SW2() == sw1sw2) {
            return true;
        } else {
            return false;
        }
    }
}
