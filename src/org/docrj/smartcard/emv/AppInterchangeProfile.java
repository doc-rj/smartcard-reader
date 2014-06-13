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

/**
 * Application Interchange Profile
 * Indicates the capabilities of the card to support specific functions in the application
 *
 * EMV Book 3 Annex C1 (page 182)
 */
public class AppInterchangeProfile {
    private byte mFirstByte;
    private byte mSecondByte;

    public AppInterchangeProfile(byte firstByte, byte secondByte) {
        this.mFirstByte = firstByte;
        this.mSecondByte = secondByte;
    }

    // left most bit of mFirstByte is RFU

    public boolean isSDASupported() {
        return (mFirstByte & (byte) 0x40) > 0;
    }

    public boolean isDDASupported() {
        return (mFirstByte & (byte) 0x20) > 0;
    }

    public boolean isCardholderVerificationSupported() {
        return (mFirstByte & (byte) 0x10) > 0;
    }

    public boolean isTerminalRiskManagementToBePerformed() {
        return (mFirstByte & (byte) 0x08) > 0;
    }

    /**
     * When this bit is set to 1, Issuer Authentication using the EXTERNAL AUTHENTICATE command is supported
     */
    public boolean isIssuerAuthenticationIsSupported() {
        return (mFirstByte & (byte) 0x04) > 0;
    }

    public boolean isCDASupported() {
        return (mFirstByte & (byte) 0x01) > 0;
    }

    //The rest of the bits are RFU (Reserved for Future Use)

    public String getSDASupportedString(){
        if(isSDASupported()){
            return "Static Data Authentication (SDA) supported";
        }else{
            return "Static Data Authentication (SDA) not supported";
        }
    }

    public String getDDASupportedString(){
        if(isDDASupported()){
            return "Dynamic Data Authentication (DDA) supported";
        }else{
            return "Dynamic Data Authentication (DDA) not supported";
        }
    }

    public String getCardholderVerificationSupportedString(){
        if(isCardholderVerificationSupported()){
            return "Cardholder verification is supported";
        }else{
            return "Cardholder verification is not supported";
        }
    }

    public String getTerminalRiskManagementToBePerformedString() {
        if (isTerminalRiskManagementToBePerformed()) {
            return "Terminal risk management is to be performed";
        } else {
            return "Terminal risk management does not need to be performed";
        }
    }

    public String getIssuerAuthenticationIsSupportedString(){
        if (isIssuerAuthenticationIsSupported()) {
            return "Issuer authentication is supported";
        } else {
            return "Issuer authentication is not supported";
        }
    }

    public String getCDASupportedString() {
        if (isCDASupported()) {
            return "CDA supported";
        } else {
            return "CDA not supported";
        }
    }

    public byte[] getBytes() {
        return new byte[]{mFirstByte, mSecondByte};
    }
}
