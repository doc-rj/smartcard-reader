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
 * This implementation is a mix of EMV & VISA TTQ
 * 
 * VISA:
 * Terminal Transaction Qualifiers (Tag '9F66') is a reader data element
 * indicating capabilities (e.g., MSD or qVSDC) and transaction-specific
 * requirements (e.g., online) of the reader. It is requested by the card in the
 * PDOL and used by the card to determine how to process the transaction
 * (for example, process using MSD or qVSDC, process offline or online).
 */
public class TerminalTranQualifiers {

    private byte[] data = new byte[4];

    public TerminalTranQualifiers() {
    }

    public boolean contactlessMagneticStripeSupported() {
        return Util.isBitSet(data[0], 8);
    }

    public boolean contactlessVSDCsupported() {
        return Util.isBitSet(data[0], 7);
    }

    public boolean contactlessEMVmodeSupported() {
        return Util.isBitSet(data[0], 6);
    }

    public boolean contactEMVsupported() {
        return Util.isBitSet(data[0], 5);
    }

    public boolean readerIsOfflineOnly() {
        return Util.isBitSet(data[0], 4);
    }

    public boolean onlinePINsupported() {
        return Util.isBitSet(data[0], 3);
    }

    public boolean signatureSupported() {
        return Util.isBitSet(data[0], 2);
    }

    public boolean onlineCryptogramRequired() {
        return Util.isBitSet(data[1], 8);
    }

    public boolean cvmRequired() {
        return Util.isBitSet(data[1], 7);
    }

    public boolean contactChipOfflinePINsupported() {
        return Util.isBitSet(data[1], 6);
    }

    public boolean issuerUpdateProcessingSupported() {
        return Util.isBitSet(data[2], 8);
    }

    public boolean consumerDeviceCVMsupported() {
        return Util.isBitSet(data[2], 7);
    }

    public void setContactlessMagneticStripeSupported(boolean value) {
        data[0] = Util.setBit(data[0], 8, value);
    }

    public void setContactlessVSDCsupported(boolean value) {
        data[0] = Util.setBit(data[0], 7, value);
        if (value) {
            /*
             * A reader that supports contactless VSDC in addition to
             * qVSDC shall not indicate support for qVSDC in the Terminal
             * Transaction Qualifiers (set byte 1 bit 6 to b'0'). The reader
             * shall restore this bit to b'1' prior to deactivation
             */
            setContactlessEMVmodeSupported(false);
        }
    }

    public void setContactlessEMVmodeSupported(boolean value) {
        data[0] = Util.setBit(data[0], 6, value);
    }

    public void setContactEMVsupported(boolean value) {
        data[0] = Util.setBit(data[0], 5, value);
    }

    public void setReaderIsOfflineOnly(boolean value) {
        data[0] = Util.setBit(data[0], 4, value);
    }

    public void setOnlinePINsupported(boolean value) {
        data[0] = Util.setBit(data[0], 3, value);
    }

    public void setSignatureSupported(boolean value) {
        data[0] = Util.setBit(data[0], 2, value);
    }

    public void setOnlineCryptogramRequired(boolean value) {
        data[1] = Util.setBit(data[1], 8, value);
    }

    public void setCvmRequired(boolean value) {
        data[1] = Util.setBit(data[1], 7, value);
    }

    public void setContactChipOfflinePINsupported(boolean value) {
        data[1] = Util.setBit(data[1], 6, value);
    }

    public void setIssuerUpdateProcessingSupported(boolean value) {
        data[2] = Util.setBit(data[2], 8, value);
    }

    public void setConsumerDeviceCVMsupported(boolean value) {
        data[2] = Util.setBit(data[2], 7, value);
    }

    //The rest of the bits in the second byte are RFU (Reserved for Future Use)
    
    
    public String getContactlessMagneticStripeSupportedString() {
        if (contactlessMagneticStripeSupported()) {
            return "Contactless magnetic stripe (MSD) supported";
        } else {
            return "Contactless magnetic stripe (MSD) not supported";
        }
    }

    public String getContactlessVSDCsupportedString() {
        if (contactlessVSDCsupported()) {
            return "Contactless VSDC supported";
        } else {
            return "Contactless VSDC not supported";
        }
    }

    public String getContactlessEMVmodeSupportedString() {
        if (contactlessEMVmodeSupported()) {
            return "Contactless EMV (qVSDC) mode supported";
        } else {
            return "Contactless EMV (qVSDC) mode not supported";
        }
    }

    public String getContactEMVsupportedString() {
        if (contactEMVsupported()) {
            return "Contact EMV (VSDC) supported";
        } else {
            return "Contact EMV (VSDC) not supported";
        }
    }

    public String getReaderIsOfflineOnlyString() {
        if (readerIsOfflineOnly()) {
            return "Reader is Offline Only";
        } else {
            return "Reader is Online Capable";
        }
    }

    public String getOnlinePINsupportedString() {
        if (onlinePINsupported()) {
            return "Online PIN supported";
        } else {
            return "Online PIN not supported";
        }
    }

    public String getSignatureSupportedString() {
        if (signatureSupported()) {
            return "Signature supported";
        } else {
            return "Signature not supported";
        }
    }

    public String getOnlineCryptogramRequiredString() {
        if (onlineCryptogramRequired()) {
            return "Online cryptogram required";
        } else {
            return "Online cryptogram not required";
        }
    }

    public String getCVMrequiredString() {
        if (cvmRequired()) {
            return "CVM required";
        } else {
            return "CVM not required";
        }
    }

    public String getContactChipOfflinePINsupportedString() {
        if (contactChipOfflinePINsupported()) {
            return "Contact Chip Offline PIN supported";
        } else {
            return "Contact Chip Offline PIN not supported";
        }
    }

    public String getIssuerUpdateProcessingSupportedString() {
        if (issuerUpdateProcessingSupported()) {
            return "Issuer Update Processing supported";
        } else {
            return "Issuer Update Processing not supported";
        }
    }

    public String getConsumerDeviceCVMsupportedString() {
        if (consumerDeviceCVMsupported()) {
            return "Consumer Device CVM supported";
        } else {
            return "Consumer Device CVM not supported";
        }
    }

    public byte[] getBytes() {
        return Arrays.copyOf(data, data.length);
    }
}
