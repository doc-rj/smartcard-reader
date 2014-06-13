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

import org.docrj.smartcard.util.Util;

public class CommandApdu {

    public static final byte ISO_CLA           = (byte)0x00;  
    public static final byte ISO_SELECT        = (byte)0xA4;
    public static final byte ISO_READ_RECORD   = (byte)0xB2;
    public static final byte ISO_INTERNAL_AUTH = (byte)0x88;
    public static final byte ISO_EXTERNAL_AUTH = (byte)0x82;
    public static final byte ISO_GET_DATA      = (byte)0xCA;

    protected String mCmdName = "";
    protected int mCla = 0x00;
    protected int mIns = 0x00;
    protected int mP1 = 0x00;
    protected int mP2 = 0x00;
    protected int mLc = 0x00;

    protected byte[] mData = new byte[0];

    protected int mLe = 0x00;
    protected boolean mLeUsed = false;

    public CommandApdu() {
    }    

    public CommandApdu(int cla, int ins, int p1, int p2) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mP1 = p1;
        mP2 = p2;
    }

    public CommandApdu(int cla, int ins, int p1, int p2, byte[] data) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mLc = data.length;
        mP1 = p1;
        mP2 = p2;
        mData = data;
    }

    public CommandApdu(int cla, int ins, int p1, int p2, byte[] data, int le) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mLc = data.length;
        mP1 = p1;
        mP2 = p2;
        mData = data;
        mLe = le;
        mLeUsed = true;
    }

    public CommandApdu(int cla, int ins, int p1, int p2, int le) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mP1 = p1;
        mP2 = p2;
        mLe = le;
        mLeUsed = true;
    }

    public void setCommandName(String cmdName) {
        mCmdName = cmdName;
    }

    private void setCommandName(int ins) {
        switch(ins) {
        case ISO_SELECT:
            mCmdName = "select app";
            break;
        case ISO_READ_RECORD:
            mCmdName = "read record";
            break;
        default:
            mCmdName = "";
            break;
        }
    }

    public String getCommandName() {
        return mCmdName;
    }

    public void setP1(int p1) {
        mP1 = p1;
    }

    public void setP2(int p2) {
        mP2 = p2;
    }

    public void setData(byte[] data) {
        mLc = data.length;
        mData = data;
    }

    public void setLe(int le) {
        mLe = le;
        mLeUsed = true;
    }

    public int getP1() {
        return mP1;
    }

    public int getP2() {
        return mP2;
    }

    public int getLc() {
        return mLc;
    }

    public byte[] getData() {
        return mData;
    }

    public int getLe() {
        return mLe;
    }

    public static String toString(byte[] cmdApdu, int Lc) {
        String cmd = Util.bytesToHex(cmdApdu);
        return cmd.substring(0, 8) + " " + cmd.substring(8, 10) + " " +
            cmd.substring(10, 10 + Lc*2) + " " + cmd.substring(10 + Lc*2, cmd.length());
    }

    public byte[] toBytes() {
        int length = 4; // CLA, INS, P1, P2
        if (mData.length != 0) {
            length += 1; // LC
            length += mData.length; // DATA
        }
        if (mLeUsed) {
            length += 1; // LE
        }

        byte[] apdu = new byte[length];

        int index = 0;
        apdu[index] = (byte) mCla;
        index++;
        apdu[index] = (byte) mIns;
        index++;
        apdu[index] = (byte) mP1;
        index++;
        apdu[index] = (byte) mP2;
        index++;
        if (mData.length != 0) {
            apdu[index] = (byte) mLc;
            index++;
            System.arraycopy(mData, 0, apdu, index, mData.length);
            index += mData.length;
        }
        if (mLeUsed) {
            apdu[index] += (byte) mLe; // LE
        }

        return apdu;
    }

    public static boolean compareHeaders(byte[] header1, byte[] mask,
            byte[] header2) {
        if (header1.length < 4 || header2.length < 4) {
            return false;
        }
        byte[] compHeader = new byte[4];
        compHeader[0] = (byte) (header1[0] & mask[0]);
        compHeader[1] = (byte) (header1[1] & mask[1]);
        compHeader[2] = (byte) (header1[2] & mask[2]);
        compHeader[3] = (byte) (header1[3] & mask[3]);

        if (((byte) compHeader[0] == (byte) header2[0])
                && ((byte) compHeader[1] == (byte) header2[1])
                && ((byte) compHeader[2] == (byte) header2[2])
                && ((byte) compHeader[3] == (byte) header2[3])) {
            return true;
        }
        return false;
    }

    public CommandApdu clone() {
        CommandApdu apdu = new CommandApdu();
        apdu.setCommandName(mCmdName);
        apdu.mCla = mCla;
        apdu.mIns = mIns;
        apdu.mP1 = mP1;
        apdu.mP2 = mP2;
        apdu.mLc = mLc;
        apdu.mData = new byte[mData.length];
        System.arraycopy(mData, 0, apdu.mData, 0, mData.length);
        apdu.mLe = mLe;
        apdu.mLeUsed = mLeUsed;
        return apdu;
    }
}
