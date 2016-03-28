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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
//import javax.security.auth.callback.Callback;
//import javax.security.auth.callback.CallbackHandler;
//import javax.security.auth.callback.PasswordCallback;
//import javax.security.auth.callback.UnsupportedCallbackException;

import org.docrj.smartcard.iso7816.Tag;
import org.docrj.smartcard.iso7816.TagAndLength;
import org.docrj.smartcard.iso7816.TagImpl;
import org.docrj.smartcard.iso7816.TagValueType;
import org.docrj.smartcard.reader.R;
import org.docrj.smartcard.util.ISO3166_1;
import org.docrj.smartcard.util.ISO4217_Numeric;
import org.docrj.smartcard.util.Util;

import android.content.res.Resources;
import android.util.Log;

/**
 * Point of sale (POS) terminal
 */
public class EMVTerminal {
    private final static String TAG = "smartcard-reader";

    private final static Properties defaultTerminalProperties = new Properties();
    private final static Properties runtimeTerminalProperties = new Properties();
    private final static TerminalVerifResults terminalVerifResults = new TerminalVerifResults();

    // private static CallbackHandler pinCallbackHandler;
    // private static boolean doVerifyPinIfRequired = false;
    // private static boolean isOnline = true;

    public static void loadProperties(Resources resources) {
        InputStream defaultStream = resources.openRawResource(R.raw.terminal_properties);
        try {
            defaultTerminalProperties.load(defaultStream);
            for (String key : defaultTerminalProperties.stringPropertyNames()) {
                // sanitize
                String sanitizedKey = Util.byteArrayToHexString(Util.fromHexString(key)).toLowerCase();
                byte[] valueBytes = Util.fromHexString(defaultTerminalProperties.getProperty(key));
                String sanitizedValue = Util.byteArrayToHexString(valueBytes).toLowerCase();
                defaultTerminalProperties.setProperty(sanitizedKey, sanitizedValue);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        ISO3166_1.init(resources);
        ISO4217_Numeric.init(resources);
    }

    //PDOL (Processing options Data Object List)
    //DDOL (*Default* Dynamic Data Authentication Data Object List)
    //     (Default to be used for constructing the INTERNAL AUTHENTICATE command if the DDOL in the card is not present)
    //TDOL (*Default* Transaction Certificate Data Object List)
    //     (Default to be used for generating the TC Hash Value if the TDOL in the card is not present)
    
    //PDOL example (Visa Electron, contactless)
//    9f 38 18 -- Processing Options Data Object List (PDOL)
//         9f 66 04 -- Terminal Transaction Qualifiers
//         9f 02 06 -- Amount, Authorised (Numeric)
//         9f 03 06 -- Amount, Other (Numeric)
//         9f 1a 02 -- Terminal Country Code
//         95 05 -- Terminal Verification Results (TVR)
//         5f 2a 02 -- Transaction Currency Code
//         9a 03 -- Transaction Date
//         9c 01 -- Transaction Type
//         9f 37 04 -- Unpredictable Number
    private static byte[] getTerminalResidentData(TagAndLength tal, EMVApp app) {
        //Check if the value is specified in the runtime properties file
        String propertyValueStr = runtimeTerminalProperties.getProperty(Util.byteArrayToHexString(tal.getTag().getTagBytes()).toLowerCase());

        if(propertyValueStr != null) {
            byte[] propertyValue = Util.fromHexString(propertyValueStr);

            if (propertyValue.length == tal.getLength()) {
                return propertyValue;
            }
        }
        
        if (tal.getTag().equals(EMVTags.TERMINAL_COUNTRY_CODE) && tal.getLength() == 2) {
            return findCountryCode(app);
        } else if (tal.getTag().equals(EMVTags.TRANSACTION_CURRENCY_CODE) && tal.getLength() == 2) {
            return findCurrencyCode(app);
        }
        
        //Now check for default values
        propertyValueStr = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(tal.getTag().getTagBytes()).toLowerCase());

        if(propertyValueStr != null) {
            byte[] propertyValue = Util.fromHexString(propertyValueStr);

            if (propertyValue.length == tal.getLength()) {
                return propertyValue;
            }
        }

        if (tal.getTag().equals(EMVTags.UNPREDICTABLE_NUMBER)) {
            return Util.generateRandomBytes(tal.getLength());
        } else if (tal.getTag().equals(EMVTags.TERMINAL_TRANSACTION_QUALIFIERS) && tal.getLength() == 4) {
            //This seems only to be used in contactless mode. Construct accordingly
            TerminalTranQualifiers ttq = new TerminalTranQualifiers();
            ttq.setContactlessEMVmodeSupported(true);
            ttq.setReaderIsOfflineOnly(true);
            return ttq.getBytes();
        } else if (tal.getTag().equals(EMVTags.TERMINAL_VERIFICATION_RESULTS) && tal.getLength() == 5) {
            //All bits set to '0'
            return terminalVerifResults.toByteArray();
        } else if (tal.getTag().equals(EMVTags.TRANSACTION_DATE) && tal.getLength() == 3) {
            return Util.getCurrentDateAsNumericEncodedByteArray();
        } else if (tal.getTag().equals(EMVTags.TRANSACTION_TYPE) && tal.getLength() == 1) {
            //transactionTypes = {     0:  "Payment",     1:  "Withdrawal", } 
            //http://www.codeproject.com/Articles/100084/Introduction-to-ISO-8583
            return new byte[]{0x00};
        } else {
            Log.d(TAG, "Terminal Resident Data not found for " + tal);
        }
        byte[] defaultResponse = new byte[tal.getLength()];
        Arrays.fill(defaultResponse, (byte) 0x00);
        return defaultResponse;
    }

    public static TerminalVerifResults getTerminalVerifResults() {
        return terminalVerifResults;
    }
    
    public static void resetTVR(){
        terminalVerifResults.reset();
    }
    
    public static void setProperty(String tagHex, String valueHex) {
        setProperty(new TagImpl(tagHex, TagValueType.BINARY, "", ""), Util.fromHexString(valueHex));
    }
    
    public static void setProperty(Tag tag, byte[] value){
        runtimeTerminalProperties.setProperty(Util.byteArrayToHexString(tag.getTagBytes()).toLowerCase(Locale.US), Util.byteArrayToHexString(value));
    }
    
    public static boolean isCDASupported(EMVApp app) {
        return false;
    }
    
    public static boolean isDDASupported(EMVApp app) {
        return true;
    }
    
    public static boolean isSDASupported(EMVApp app) {
        return true;
    }
    
    public static boolean isATM() {
        return false;
    }
    
    public static Date getCurrentDate() {
        return new Date();
    }
    
    /*
    public static int getSupportedApplicationVersionNumber(EMVApp app) {
        //TODO
        //For now, just return the version number maintained in the card
        //return app.getApplicationVersionNumber();
        return 1;
    }

    public static boolean isCVMRecognized(EMVApp app, CVRule rule) {
        switch(rule.getRule()) {
            case RESERVED_FOR_USE_BY_THE_INDIVIDUAL_PAYMENT_SYSTEMS:
                //app.getAID().getRIDBytes();
                //TODO check if RID specific rule is supported
                //if(supported) {
                //    return true;
                //}
            case RESERVED_FOR_USE_BY_THE_ISSUER:
                
                if(app.getIssuerIdentificationNumber() != null){
                    //TODO check if issuer specific rule is supported
                    //if(supported){
                    //  return true;
                    //}
                }
            case NOT_AVAILABLE_FOR_USE:
            case RFU:
                return false;
        }
        return true;
    }
    
    public static boolean isCVMSupported(CVRule rule) {
        switch(rule.getRule()) {
            //TODO support enciphered PIN
            case ENCIPHERED_PIN_VERIFIED_BY_ICC:
            case ENCIPHERED_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER:
                return false;
            case PLAINTEXT_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER:
            case PLAINTEXT_PIN_VERIFIED_BY_ICC:
                return hasPinInputCapability();
            case SIGNATURE_ON_PAPER:
                return false;
            case ENCIPHERED_PIN_VERIFIED_ONLINE:
                return isOnline();
            case FAIL_PROCESSING:
            case NO_CVM_REQUIRED:
                return true;
        }
        return false;
    }
    
    public static boolean isOnline() {
        return isOnline;
    }
    
    public static void setIsOnline(boolean value){
        isOnline = value;
    }
    
    public static boolean isCVMConditionSatisfied(CVRule rule) {
        if(rule.getConditionAlways()) {
            return true;
        }
        if(rule.getConditionCode() <= 0x05){
            //TODO
            return true;
        }else if(rule.getConditionCode() < 0x0A) {
            //TODO
            //Check for presence Application Currency Code or Amount, Authorised in app records?
            return true;
        } else { //RFU and proprietary
            return false;
        }
    }
    
    public static boolean verifyEncipheredPinOnline() {
        if(!isOnline()) {
            return false;
        }
        //TODO
        return true;
    }
    
    public static boolean hasSignatureOnPaper() {
        return true;
    }
    
    public static void setDoVerifyPinIfRequired(boolean value) {
        doVerifyPinIfRequired = value;
    }
    
    public static boolean getDoVerifyPinIfRequired() {
        return doVerifyPinIfRequired;
    }
    */

    /**
     * 
     * @return true if a Pin CallbackHandler has be set
     */
    /*
    public static boolean hasPinInputCapability() {
        return doVerifyPinIfRequired && pinCallbackHandler != null;
    }
    
    public static void setPinCallbackHandler(CallbackHandler callbackHandler) {
        pinCallbackHandler = callbackHandler;
    }
    
    public static PasswordCallback getPinInput() {
        CallbackHandler callBackHandler = pinCallbackHandler;
        if(callBackHandler == null){
            return null;
        }
        PasswordCallback passwordCallback = new PasswordCallback("Type PIN", false);
        try{
            callBackHandler.handle(new Callback[]{passwordCallback});
        }catch(IOException ex){
            Log.info(Util.getStackTrace(ex));
        }catch(UnsupportedCallbackException ex){
            Log.info(Util.getStackTrace(ex));
        }
        return passwordCallback;
    }
    
    public static boolean getPerformTerminalRiskManagement() {
        return false;
    }
    */

    public static byte[] constructDOLResponse(DOL dol, EMVApp app) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (TagAndLength tagAndLength : dol.getTagAndLengthList()) {
            byte[] data = getTerminalResidentData(tagAndLength, app);
            stream.write(data, 0, data.length);
        }
        return stream.toByteArray();
    }

    //The ICC may contain the DDOL, but there shall be a default DDOL in the terminal, 
    //specified by the payment system, for use in case the DDOL is not present in the ICC.
    public static byte[] getDefaultDDOLResponse(EMVApp app) {
        //It is mandatory that the DDOL contains the Unpredictable Number generated by the terminal (tag '9F37', 4 bytes binary).
        byte[] unpredictableNumber = Util.generateRandomBytes(4);
        
        //TODO add other DDOL data specified by the payment system
        //if(app.getAID().equals(SOMEAID))
        
        return unpredictableNumber;
    }

    //Ex Banco BRADESCO (f0 00 00 00 03 00 01) failes GPO with wrong COUNTRY_CODE !
    private static byte[] findCountryCode(EMVApp app) {
        if(app != null){
            if(app.getIssuerCC() != -1){
                byte[] countryCode = Util.intToBinaryEncodedDecimalByteArray(app.getIssuerCC());
                return Util.resizeArray(countryCode, 2);
            }
        }

        Log.d(TAG, "No Issuer Country Code found in app. Using default Terminal Country Code");

        String countryCode = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_COUNTRY_CODE.getTagBytes()));
        if(countryCode != null){
            return Util.fromHexString(countryCode);
        }
        
        return new byte[]{0x08, 0x26};
    }
    
    private static byte[] findCurrencyCode(EMVApp app){
        if(app != null){
            int appCurrencyCode = app.getAppCurrencyCode();
            if (appCurrencyCode != -1) {
                byte[] currencyCode = Util.intToBinaryEncodedDecimalByteArray(appCurrencyCode);
                return Util.resizeArray(currencyCode, 2);
            }
            Locale preferredLocale = null;
            if (app.getLanguagePref() != null) {
                preferredLocale = app.getLanguagePref().getPreferredLocale();
            }
            /* TODO:
            if (preferredLocale == null 
                    && app.getCard() != null 
                    && app.getCard().getPSE() != null
                    && app.getCard().getPSE().getLanguagePreference() != null){
                preferredLocale = app.getCard().getPSE().getLanguagePreference().getPreferredLocale();
            }
            */
            if (preferredLocale != null) {
                if(preferredLocale.getLanguage().equals(Locale.getDefault().getLanguage())) {
                    //Guesstimate; we presume default locale is the preferred
                    preferredLocale = Locale.getDefault();
                }
                List<Integer> numericCodes = ISO4217_Numeric.getNumericCodeForLocale(preferredLocale);
                if (numericCodes != null && numericCodes.size() > 0) {
                    //Just use the first found. It might not be correct, eg Brazil (BRZ) vs Portugal (EUR)
                    return Util.resizeArray(Util.intToBinaryEncodedDecimalByteArray(numericCodes.get(0)), 2); 
                }
            }
            
        }
        String currencyCode = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TRANSACTION_CURRENCY_CODE.getTagBytes()));
        if(currencyCode != null){
            return Util.fromHexString(currencyCode);
        }
        return new byte[]{0x08, 0x26};
    }
}
