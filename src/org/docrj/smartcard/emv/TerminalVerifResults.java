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

import org.docrj.smartcard.util.Util;
import org.docrj.smartcard.iso7816.SmartcardException;

import java.util.BitSet;


/**
 * Terminal Verification Results (TVR)
 * Status of the different functions as seen from the terminal
 *
 * Data flow: Terminal -> ICC
 * (Terminal constructs TVR and transmits to card)
 */
public class TerminalVerifResults {

    private static String[] description;

    static {
        description = new String[5 * 8];

        //In the BitSet bitIndex order (bit 0 = TVR byte 5 rightmost bit)
        description[0] = "RFU";
        description[1] = "RFU";
        description[2] = "RFU";
        description[3] = "RFU";
        description[4] = "Script processing failed after final GENERATE AC";
        description[5] = "Script processing failed before final GENERATE AC";
        description[6] = "Issuer authentication failed";
        description[7] = "Default TDOL used";
        description[8] = "RFU";
        description[9] = "RFU";
        description[10] = "RFU";
        description[11] = "Merchant forced transaction online";
        description[12] = "Transaction selected randomly for online processing";
        description[13] = "Upper consecutive offline limit exceeded";
        description[14] = "Lower consecutive offline limit exceeded";
        description[15] = "Transaction exceeds floor limit";
        description[16] = "RFU";
        description[17] = "RFU";
        description[18] = "Online PIN entered";
        description[19] = "PIN entry required, PIN pad present, but PIN was not entered";
        description[20] = "PIN entry required and PIN pad not present or not working";
        description[21] = "PIN Try Limit exceeded";
        description[22] = "Unrecognised CVM";
        description[23] = "Cardholder verification was not successful";
        description[24] = "RFU";
        description[25] = "RFU";
        description[26] = "RFU";
        description[27] = "New card";
        description[28] = "Requested service not allowed for card product";
        description[29] = "Application not yet effective";
        description[30] = "Expired application";
        description[31] = "ICC and terminal have different application versions";
        description[32] = "RFU";
        description[33] = "RFU";
        description[34] = "CDA failed";
        description[35] = "DDA failed";
        description[36] = "Card appears on terminal exception file";
        description[37] = "ICC data missing";
        description[38] = "SDA failed";
        description[39] = "Offline data authentication was not performed";
    }
    
    private BitSet bitSet;

    public TerminalVerifResults() {
        this.bitSet = new BitSet(5 * 8);
    }

    TerminalVerifResults(byte[] data) {
        if (data.length != 5) {
            throw new SmartcardException("TVR must be initialized with 5 bytes. Length=" + data.length);
        }
        this.bitSet = Util.byteArray2BitSet(data);
    }

    public void setOfflineDataAuthenticationWasNotPerformed(boolean value) {
        bitSet.set(getBitSetIndex(1, 8), value);
    }

    public boolean offlineDataAuthenticationWasNotPerformed() {
        return bitSet.get(getBitSetIndex(1, 8));
    }

    public void sdaFailed(boolean value) {
        bitSet.set(getBitSetIndex(1, 7), value);
    }

    public boolean sdaFailed() {
        return bitSet.get(getBitSetIndex(1, 7));
    }

    public void setICCDataMissing(boolean value) {
        bitSet.set(getBitSetIndex(1, 6), value);
    }

    public boolean iccDataMissing() {
        return bitSet.get(getBitSetIndex(1, 6));
    }

    public void setCardAppearsOnTerminalExceptionFile(boolean value) {
        bitSet.set(getBitSetIndex(1, 5), value);
    }

    /**
     * There is no requirement in the EMV specification for an exception file,
     * but it is recognised that some terminals may have this capability.
     * @return
     */
    public boolean cardAppearsOnTerminalExceptionFile() {
        return bitSet.get(getBitSetIndex(1, 5));
    }

    public void setDDAFailed(boolean value) {
        bitSet.set(getBitSetIndex(1, 4), value);
    }

    public boolean ddaFailed() {
        return bitSet.get(getBitSetIndex(1, 4));
    }

    public void setCDAFailed(boolean value) {
        bitSet.set(getBitSetIndex(1, 3), value);
    }

    public boolean cdaFailed() {
        return bitSet.get(getBitSetIndex(1, 3));
    }

    //2 rightmost bits of the first byte are RFU
    //Second byte
    public void setICCAndTerminalHaveDifferentApplicationVersions(boolean value) {
        bitSet.set(getBitSetIndex(2, 8), value);
    }

    public boolean iccAndTerminalHaveDifferentApplicationVersions() {
        return bitSet.get(getBitSetIndex(2, 8));
    }

    public void setExpiredApplication(boolean value) {
        bitSet.set(getBitSetIndex(2, 7), value);
    }

    public boolean expiredApplication() {
        return bitSet.get(getBitSetIndex(2, 7));
    }

    public void setApplicationNotYetEffective(boolean value) {
        bitSet.set(getBitSetIndex(2, 6), value);
    }

    public boolean applicationNotYetEffective() {
        return bitSet.get(getBitSetIndex(2, 6));
    }

    public void setRequestedServiceNotAllowedForCardProduct(boolean value) {
        bitSet.set(getBitSetIndex(2, 5), value);
    }

    public boolean requestedServiceNotAllowedForCardProduct() {
        return bitSet.get(getBitSetIndex(2, 5));
    }

    public void setNewCard(boolean value) {
        bitSet.set(getBitSetIndex(2, 4), value);
    }

    public boolean newCard() {
        return bitSet.get(getBitSetIndex(2, 4));
    }

    //3 rightmost bits of the second byte are RFU
    //Third byte
    public void setCardholderVerificationWasNotSuccessful(boolean value) {
        bitSet.set(getBitSetIndex(3, 8), value);
    }

    public boolean cardholderVerificationWasNotSuccessful() {
        return bitSet.get(getBitSetIndex(3, 8));
    }

    public void setUnrecognisedCVM(boolean value) {
        bitSet.set(getBitSetIndex(3, 7), value);
    }

    public boolean unrecognisedCVM() {
        return bitSet.get(getBitSetIndex(3, 7));
    }

    public void setPinTryLimitExceeded(boolean value) {
        bitSet.set(getBitSetIndex(3, 6), value);
    }

    public boolean pinTryLimitExceeded() {
        return bitSet.get(getBitSetIndex(3, 6));
    }

    public void setPinEntryRequiredAndPINPadNotPresentOrNotWorking(boolean value) {
        bitSet.set(getBitSetIndex(3, 5), value);
    }

    public boolean pinEntryRequiredAndPINPadNotPresentOrNotWorking() {
        return bitSet.get(getBitSetIndex(3, 5));
    }

    public void setPinEntryRequired_PINPadPresent_ButPINWasNotEntered(boolean value) {
        bitSet.set(getBitSetIndex(3, 4), value);
    }

    public boolean pinEntryRequired_PINPadPresent_ButPINWasNotEntered() {
        return bitSet.get(getBitSetIndex(3, 4));
    }

    public void setOnlinePINEntered(boolean value) {
        bitSet.set(getBitSetIndex(3, 3), value);
    }

    public boolean onlinePINEntered() {
        return bitSet.get(getBitSetIndex(3, 3));
    }

    //2 rightmost bits of the third byte are RFU
    //Fourth byte
    public void setTransactionExceedsFloorLimit(boolean value) {
        bitSet.set(getBitSetIndex(4, 8), value);
    }

    public boolean transactionExceedsFloorLimit() {
        return bitSet.get(getBitSetIndex(4, 8));
    }

    public void setLowerConsecutiveOfflineLimitExceeded(boolean value) {
        bitSet.set(getBitSetIndex(4, 7), value);
    }

    public boolean lowerConsecutiveOfflineLimitExceeded() {
        return bitSet.get(getBitSetIndex(4, 7));
    }

    public void setUpperConsecutiveOfflineLimitExceeded(boolean value) {
        bitSet.set(getBitSetIndex(4, 6), value);
    }

    public boolean upperConsecutiveOfflineLimitExceeded() {
        return bitSet.get(getBitSetIndex(4, 6));
    }

    public void setTransactionSelectedRandomlyForOnlineProcessing(boolean value) {
        bitSet.set(getBitSetIndex(4, 5), value);
    }

    public boolean transactionSelectedRandomlyForOnlineProcessing() {
        return bitSet.get(getBitSetIndex(4, 5));
    }

    public void setMerchantForcedTransactionOnline(boolean value) {
        bitSet.set(getBitSetIndex(4, 4), value);
    }

    public boolean merchantForcedTransactionOnline() {
        return bitSet.get(getBitSetIndex(4, 4));
    }

    //3 rightmost bits of the fourth byte are RFU
    //Fifth byte
    public void setDefaultTDOLused(boolean value) {
        bitSet.set(getBitSetIndex(5, 8), value);
    }

    public boolean defaultTDOLused() {
        return bitSet.get(getBitSetIndex(5, 8));
    }

    public void setIssuerAuthenticationFailed(boolean value) {
        bitSet.set(getBitSetIndex(5, 7), value);
    }

    public boolean issuerAuthenticationFailed() {
        return bitSet.get(getBitSetIndex(5, 7));
    }

    public void setScriptProcessingFailedBeforeFinal_GENERATE_AC(boolean value) {
        bitSet.set(getBitSetIndex(5, 6), value);
    }

    public boolean scriptProcessingFailedBeforeFinal_GENERATE_AC() {
        return bitSet.get(getBitSetIndex(5, 6));
    }

    public void setScriptProcessingFailedAfterFinal_GENERATE_AC(boolean value) {
        bitSet.set(getBitSetIndex(5, 5), value);
    }

    public boolean scriptProcessingFailedAfterFinal_GENERATE_AC() {
        return bitSet.get(getBitSetIndex(5, 5));
    }
    
    public void reset() {
        bitSet.clear();
    }

    //4 rightmost bits of the fifth byte are RFU
    public byte[] toByteArray() {
        return Util.resizeArray(Util.bitSet2ByteArray(bitSet), 5);
    }

    private int getBitSetIndex(int byteNum, int bitPos) {
        //byteNum 1 is the leftmost byte
        //bitNum 1 is the rightmost bit
        if (byteNum > 8 || byteNum < 1 || bitPos > 8 || bitPos < 1) {
            throw new IllegalArgumentException("byteNum and bitPos must be in the range 1-8. byteNum=" + byteNum + " bitPost=" + bitPos);
        }
        return (5 - byteNum) * 8 + (bitPos - 1);
    }

    // TODO checkActionCode(byte[] actionCode) ?????
}
