/*
 * Copyright 2014 Ryan Jones
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

package org.docrj.smartcard.reader;

import org.docrj.smartcard.util.Util;
import org.docrj.smartcard.emv.GpoApdu;
import org.docrj.smartcard.iso7816.CommandApdu;
import org.docrj.smartcard.iso7816.ReadRecordApdu;
import org.docrj.smartcard.iso7816.ResponseApdu;
import org.docrj.smartcard.iso7816.SelectApdu;
import org.docrj.smartcard.iso7816.TLVUtil;
import org.docrj.smartcard.iso7816.TLVException;

import org.docrj.smartcard.emv.AppPriorityIndicator;
import org.docrj.smartcard.emv.DDF;
import org.docrj.smartcard.emv.EMVApp;
import org.docrj.smartcard.emv.EMVTags;
import org.docrj.smartcard.emv.LanguagePref;
import org.docrj.smartcard.iso7816.BERTLV;

import org.docrj.smartcard.emv.AppElementaryFile;
import org.docrj.smartcard.emv.AppFileLocator;
import org.docrj.smartcard.emv.AppInterchangeProfile;
//import org.docrj.smartcard.emv.AppPriorityIndicator;
import org.docrj.smartcard.emv.DOL;
//import org.docrj.smartcard.emv.EMVApp;
import org.docrj.smartcard.emv.IssuerIdNumber;
import org.docrj.smartcard.emv.LogEntry;
import org.docrj.smartcard.emv.VISATags;
import org.docrj.smartcard.iso7816.SmartcardException;
import org.docrj.smartcard.iso7816.Tag;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class PaymentReaderXcvr extends ReaderXcvr {

    public static final int SW_SELECTED_FILE_INVALIDATED = 0x6283;
    public static final int SW_COMMAND_NOT_ALLOWED_CONDITIONS_OF_USE_NOT_SATISFIED = 0x6985;

    private static final String EMV_PPSE_AID = "325041592E5359532E4444463031";
    private final byte[] mPpseAidBytes;

    private int mTestMode;    
    private DDF mPpseDdf;

    public PaymentReaderXcvr(IsoDep isoDep, String aid, UiCallbacks onMessage, int testMode) {
        super(isoDep, aid, onMessage);
        mPpseAidBytes = Util.hexToBytes(EMV_PPSE_AID);
        mTestMode = testMode;
    }

    @Override
    public void run() {
        try {
            mIsoDep.connect();

            // select ppse
            if (selectPpse()) {
                if (mTestMode == Launcher.TEST_MODE_APP_SELECT) {
                    selectApp(mAid);
                } else if (mPpseDdf != null) {
                    // process each app found in ppse select response
                    for (EMVApp app : mPpseDdf.getEMVApps()) {
                        if (selectApp(app)) {
                            if (initiateAppProcessing(app)) {
                                readAppData(app);
                            }
                        }
                    }
                }
            }
            mIsoDep.close();
        } catch (TagLostException e) {
            mUiCallbacks
                    .onError(mContext.getString(R.string.tag_lost_err));
        } catch (IOException e) {
            mUiCallbacks.onError(e.getMessage());
        } catch (TLVException e) {
            mUiCallbacks.onError(e.getMessage());
        }
    }

    private boolean selectPpse() throws TagLostException, IOException {
        Log.d(TAG, "select PPSE");
        CommandApdu selectPpseApdu = new SelectApdu(mPpseAidBytes);
        selectPpseApdu.setCommandName("select ppse");
        ResponseApdu rspApdu = sendAndRcv(selectPpseApdu, true);

        if (rspApdu.isStatus(SW_NO_ERROR)) {
            mUiCallbacks.onOkay(mContext.getString(R.string.select_ppse_ok,
                    rspApdu.getSW1SW2()));
        } else {
            mUiCallbacks.onError(
                    mContext.getString(R.string.select_ppse_err,
                            rspApdu.getSW1SW2(),
                            ApduParser.parse(false, rspApdu.toBytes())));
            return false;
        }
        try {
            mPpseDdf = parseFCIDDF(rspApdu.getData());
        } catch (TLVException e) {
            mPpseDdf = null;
            mUiCallbacks.onError(e.getMessage());
            return true;
        }
        return true;
    }

    private boolean selectApp(EMVApp app) throws IOException {
        return selectApp(app.getAid(), app);
    }

    private boolean selectApp(String aid) throws IOException {
        return selectApp(aid, null);
    }

    private boolean selectApp(String aid, EMVApp app) throws TagLostException, IOException {
        Log.d(TAG, "select app: " + aid);
        byte[] aidBytes = Util.hexToBytes(aid);
        ResponseApdu rspApdu = sendAndRcv(new SelectApdu(aidBytes), true);
        
        if (rspApdu.isStatus(SW_NO_ERROR)) {
            mUiCallbacks.onOkay(mContext.getString(R.string.select_app_ok,
                    rspApdu.getSW1SW2()));
            if (app != null) {
                try {
                    parseFCIADF(rspApdu.getData(), app);
                } catch (Exception e) {
                    mUiCallbacks.onError(e.getMessage());
                }
            }
        } else {
            if (rspApdu.getSW1SW2() == SW_SELECTED_FILE_INVALIDATED) {
                Log.d(TAG, "Application blocked!");
            }
            mUiCallbacks.onError(
                    mContext.getString(R.string.select_app_err,
                            rspApdu.getSW1SW2(),
                            ApduParser.parse(false, rspApdu.toBytes())));
            return false;
        }        
        return true;
    }

    private boolean initiateAppProcessing(EMVApp app) throws IOException {
        Log.d(TAG, "get processing options");
        ResponseApdu rspApdu = sendAndRcv(GpoApdu.getGpoApdu(app.getPdol(), app), false);

        if (rspApdu.isStatus(SW_NO_ERROR)) {
            mUiCallbacks.onOkay(mContext.getString(R.string.gpo_ok, rspApdu.getSW1SW2()));
            try {
                // format of the response message is given in EMV 4.2 book 3, section 6.5.8. 
                parseProcessingOpts(rspApdu.getData(), app);
            } catch (Exception e) {
                mUiCallbacks.onError(e.getMessage());
            }

            if (app.getAppInterchangeProfile() == null || app.getAppFileLocator() == null) {
                //throw new SmartcardException("GPO response did not contain AIP and AFL");
                Log.d(TAG, "GPO response did not contain AIP and AFL");
                mUiCallbacks.onError(mContext.getString(R.string.gpo_aip_afl_err));
                return false;
            }
        } else {
            mUiCallbacks.onError(mContext.getString(R.string.gpo_err, rspApdu.getSW1SW2()));
                            //ApduParser.parse(false, rspApdu.toBytes())));
            return false;
        }
        return true;
    }

    private boolean readAppData(EMVApp app) throws IOException {
        for (AppElementaryFile aef : app.getAppFileLocator().getApplicationElementaryFiles()) {
            int sfi = aef.getSfi();
            int start = aef.getStartRecordNum();
            int end = aef.getEndRecordNum();

            for (int record = start; record <= end; record++) {
                Log.d(TAG, "Read record, sfi: " + sfi + " , record: " + record);
                ResponseApdu rspApdu = sendAndRcv(new ReadRecordApdu(record, sfi), true);

                if (rspApdu.isStatus(SW_NO_ERROR)) {
                    mUiCallbacks.onOkay(mContext.getString(R.string.read_rec_ok,
                        sfi, record, rspApdu.getSW1SW2()));
                    //parseAppRecord(rspApdu.getData(), app);
                    //boolean isInvolvedInOfflineDataAuth =
                    //    (recordNum - startRecord + 1) <= aef.getNumRecordsInvolvedInOfflineDataAuth();
                    //Record record = new Record(rspApdu.getData(), recordNum, isInvolvedInOfflineDataAuth);
                    //aef.setRecord(recordNum, record);
                } else {
                    // any SW1 SW2 other than '9000' passed to the application layer as a result
                    // of reading any record shall cause the transaction to be terminated [spec]
                    mUiCallbacks.onError(
                            mContext.getString(R.string.read_rec_err,
                                    sfi, record,
                                    rspApdu.getSW1SW2(),
                                    ApduParser.parse(false, rspApdu.toBytes())));
                    return false;
                }
            }
        }
        return true;
    }

    private static DDF parseFCIDDF(byte[] data) {
        DDF ddf = new DDF();
        BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(data));

        if (tlv.getTag().equals(EMVTags.FCI_TEMPLATE)) {
            ByteArrayInputStream templateStream = tlv.getValueStream();

            while (templateStream.available() >= 2) {
                tlv = TLVUtil.getNextTLV(templateStream);
                if (tlv.getTag().equals(EMVTags.DEDICATED_FILE_NAME)) {
                    ddf.setName(tlv.getValueBytes());
                } else if (tlv.getTag().equals(EMVTags.FCI_PROPRIETARY_TEMPLATE)) {
                    ByteArrayInputStream bis2 = new ByteArrayInputStream(tlv.getValueBytes());
                    int totalLen = bis2.available();
                    int templateLen = tlv.getLength();
                    while (bis2.available() > (totalLen - templateLen)) {
                        tlv = TLVUtil.getNextTLV(bis2);

                        if (tlv.getTag().equals(EMVTags.SFI)) {
                            int sfi = Util.byteArrayToInt(tlv.getValueBytes());
                            ddf.setSFI(sfi);
                        } else if (tlv.getTag().equals(EMVTags.LANGUAGE_PREFERENCE)) {
                            LanguagePref languagePreference = new LanguagePref(tlv.getValueBytes());
                            ddf.setLanguagePreference(languagePreference);
                        } else if (tlv.getTag().equals(EMVTags.ISSUER_CODE_TABLE_INDEX)) {
                            int index = Util.byteArrayToInt(tlv.getValueBytes());
                            ddf.setIssuerCodeTableIndex(index);
                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_LABEL)) {
                            // TODO: is this tag expected at this point?
                            // should be in app_template! any info in book 1?
                            //String label = Util.getSafePrintChars(tlv.getValueBytes());
                            //ddf.setApplicationLabel(label);
                        } else if (tlv.getTag().equals(EMVTags.FCI_ISSUER_DISCRETIONARY_DATA)) { //PPSE
                            ByteArrayInputStream discrStream = new ByteArrayInputStream(tlv.getValueBytes());
                            int total3Len = discrStream.available();
                            int template3Len = tlv.getLength();
                            while (discrStream.available() > (total3Len - template3Len)) {
                                tlv = TLVUtil.getNextTLV(discrStream);

                                if (tlv.getTag().equals(EMVTags.APPLICATION_TEMPLATE)) {
                                    ByteArrayInputStream appTemplateStream = new ByteArrayInputStream(tlv.getValueBytes());
                                    int appTemplateTotalLen = appTemplateStream.available();
                                    int template4Len = tlv.getLength();

                                    EMVApp app = new EMVApp();
                                    while (appTemplateStream.available() > (appTemplateTotalLen - template4Len)) {
                                        tlv = TLVUtil.getNextTLV(appTemplateStream);

                                        if (tlv.getTag().equals(EMVTags.AID_CARD)) {
                                            app.setAid(Util.bytesToHex(tlv.getValueBytes()));
                                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_LABEL)) {
                                            app.setName(Util.getSafePrintChars(tlv.getValueBytes()));
                                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_PRIORITY_INDICATOR)) {
                                            app.setApi(new AppPriorityIndicator(tlv.getValueBytes()[0]));
                                        } else {
                                            //TODO call ddf instead of card?
                                            //card.addUnhandledRecord(tlv);
                                        }
                                    }
                                    if (app.getAid() == null) {
                                        Log.d(TAG, "invalid app template: " + app.toString());
                                    } else {
                                        // TODO
                                        ddf.addEMVApp(app);
                                    }
                                    
                                } else {
                                    //TODO call ddf instead of card?
                                    //card.addUnhandledRecord(tlv);
                                }
                            }
                        } else {
                            //TODO call ddf instead of card?
                            //card.addUnhandledRecord(tlv);
                        }
                    }
                } else {
                    //TODO call ddf instead of card?
                    //card.addUnhandledRecord(tlv);
                }
            }
        } else {
            //TODO call ddf instead of card?
            //card.addUnhandledRecord(tlv);
        }

        return ddf;
    }

    private static void parseFCIADF(byte[] data, EMVApp app) {
        if (data == null || data.length < 2) {
            return;
        }

        BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(data));

        if (tlv.getTag().equals(EMVTags.FCI_TEMPLATE)) {
            ByteArrayInputStream templateStream = tlv.getValueStream();
            while (templateStream.available() >= 2) {

                tlv = TLVUtil.getNextTLV(templateStream);
                if (tlv.getTag().equals(EMVTags.DEDICATED_FILE_NAME)) {
                    app.setAid(Util.bytesToHex(tlv.getValueBytes()));
                } else if (tlv.getTag().equals(EMVTags.FCI_PROPRIETARY_TEMPLATE)) { //Proprietary Information Template
                    ByteArrayInputStream bis2 = tlv.getValueStream();
                    int totalLen = bis2.available();
                    int templateLen = tlv.getLength();
                    while (bis2.available() > (totalLen - templateLen)) {
                        tlv = TLVUtil.getNextTLV(bis2);

                        if (tlv.getTag().equals(EMVTags.APPLICATION_LABEL)) {
                            app.setName(Util.getSafePrintChars(tlv.getValueBytes()));
                        } else if (tlv.getTag().equals(EMVTags.PDOL)) {
                            app.setPdol(new DOL(DOL.Type.PDOL, tlv.getValueBytes()));
                        } else if (tlv.getTag().equals(EMVTags.LANGUAGE_PREFERENCE)) {
                            LanguagePref languagePreference = new LanguagePref(tlv.getValueBytes());
                            app.setLanguagePref(languagePreference);
                        } else if (tlv.getTag().equals(EMVTags.APP_PREFERRED_NAME)) {
                            //TODO: "Use Issuer Code Table Index"
                            String preferredName = Util.getSafePrintChars(tlv.getValueBytes()); //Use only safe print chars, just in case
                            app.setPreferredName(preferredName);
                        } else if (tlv.getTag().equals(EMVTags.ISSUER_CODE_TABLE_INDEX)) {
                            int index = Util.byteArrayToInt(tlv.getValueBytes());
                            app.setIssuerCodeTableIndex(index);
                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_PRIORITY_INDICATOR)) {
                            AppPriorityIndicator api = new AppPriorityIndicator(tlv.getValueBytes()[0]);
                            app.setApi(api);
                        } else if (tlv.getTag().equals(EMVTags.FCI_ISSUER_DISCRETIONARY_DATA)) { // File Control Information (FCI) Issuer Discretionary Data
                            ByteArrayInputStream bis3 = tlv.getValueStream();
                            int totalLenFCIDiscretionary = bis3.available();
                            int tlvLen = tlv.getLength();
                            while (bis3.available() > (totalLenFCIDiscretionary - tlvLen)) {
                                tlv = TLVUtil.getNextTLV(bis3);
                                if (tlv.getTag().equals(EMVTags.LOG_ENTRY)) {
                                    app.setLogEntry(new LogEntry(tlv.getValueBytes()[0], tlv.getValueBytes()[1]));
                                } else if (tlv.getTag().equals(VISATags.VISA_LOG_ENTRY)) {
                                    // TODO: add this to VISAApp
                                    //app.setVisaLogEntry(new LogEntry(tlv.getValueBytes()[0], tlv.getValueBytes()[1]));
                                } else if (tlv.getTag().equals(EMVTags.ISSUER_URL)) {
                                    app.setIssuerUrl(Util.getSafePrintChars(tlv.getValueBytes()));
                                } else if (tlv.getTag().equals(EMVTags.ISSUER_IDENTIFICATION_NUMBER)) {
                                    IssuerIdNumber iin = new IssuerIdNumber(tlv.getValueBytes());
                                    app.setIssuerIdNumber(iin);
                                } else if (tlv.getTag().equals(EMVTags.ISSUER_COUNTRY_CODE_ALPHA3)) {
                                    app.setIssuerCCAlpha3(Util.getSafePrintChars(tlv.getValueBytes()));
                                } else {
                                    checkForProprietaryTagOrAddToUnhandled(app, tlv);
                                }
                            }
                        } else {
                            checkForProprietaryTagOrAddToUnhandled(app, tlv);
                        }

                    }
                }
            }

        } else {
            checkForProprietaryTagOrAddToUnhandled(app, tlv);
            throw new SmartcardException("Error parsing ADF, expected FCI template. data: " +
                Util.byteArrayToHexString(data));
        }
    }

    private static void checkForProprietaryTagOrAddToUnhandled(EMVApp app, BERTLV tlv) {
        Tag tagFound = EMVTags.get(app, tlv.getTag());
        if (tagFound != null) {
            app.addUnprocessedRecord(tlv);
        } else {
            app.addUnknownRecord(tlv);
        }
    }

    public static void parseProcessingOpts(byte[] data, EMVApp app) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        if (bis.available() < 2) {
            throw new SmartcardException("Error parsing Processing Options. Invalid TLV Length. Data: " +
                Util.byteArrayToHexString(data));
        }
        BERTLV tlv = TLVUtil.getNextTLV(bis);

        ByteArrayInputStream valueBytesBis = tlv.getValueStream();

        if (valueBytesBis.available() < 2) {
            throw new SmartcardException("Error parsing Processing Options: Invalid ValueBytes length: " +
                valueBytesBis.available());
        }

        if (tlv.getTag().equals(EMVTags.RESPONSE_MESSAGE_TEMPLATE_1)) {
            // AIP and AFL concatenated without delimiters (that is, excluding tag and length)
            AppInterchangeProfile aip = new AppInterchangeProfile((byte) valueBytesBis.read(), (byte) valueBytesBis.read());
            app.setAppInterchangeProfile(aip);

            if (valueBytesBis.available() % 4 != 0) {
                throw new SmartcardException("Error parsing Processing Options: Invalid AFL length: " +
                    valueBytesBis.available());
            }

            byte[] aflBytes = new byte[valueBytesBis.available()];
            valueBytesBis.read(aflBytes, 0, aflBytes.length);

            AppFileLocator afl = new AppFileLocator(aflBytes);
            app.setAppFileLocator(afl);
        } else if (tlv.getTag().equals(EMVTags.RESPONSE_MESSAGE_TEMPLATE_2)) {
            //AIP (and AFL) WITH delimiters (that is, including, including tag and length) and
            // possibly other BER TLV tags (that might be proprietary)
            while (valueBytesBis.available() >= 2) {
                tlv = TLVUtil.getNextTLV(valueBytesBis);
                
//   Example:
//                77 4e -- Response Message Template Format 2
//                    82 02 -- Application Interchange Profile
//                          00 00 (BINARY)
//                    9f 36 02 -- Application Transaction Counter (ATC)
//                             00 01 (BINARY)
//                    57 13 -- Track 2 Equivalent Data
//                          40 23 60 09 00 12 50 08 d1 80 52 21 15 15 29 93
//                          00 00 0f (BINARY)
//                    9f 10 07 -- Issuer Application Data
//                             06 0a 0a 03 a0 00 00 (BINARY)
//                    9f 26 08 -- Application Cryptogram
//                             4e 29 20 46 bf 43 38 51 (BINARY)
//                    5f 34 01 -- Application Primary Account Number (PAN) Sequence Number
//                             01 (NUMERIC)
//                    9f 6c 02 -- [UNHANDLED TAG]
//                             30 00 (BINARY)
//                    5f 20 0f -- Cardholder Name
//                             56 49 53 41 20 43 41 52 44 48 4f 4c 44 45 52 (=VISA CARDHOLDER)
                
                if (tlv.getTag().equals(EMVTags.APPLICATION_INTERCHANGE_PROFILE)) {
                    byte[] aipBytes = tlv.getValueBytes();
                    AppInterchangeProfile aip = new AppInterchangeProfile(aipBytes[0], aipBytes[1]);
                    app.setAppInterchangeProfile(aip);
                } else if (tlv.getTag().equals(EMVTags.APPLICATION_FILE_LOCATOR)) {
                    byte[] aflBytes = tlv.getValueBytes();
                    AppFileLocator afl = new AppFileLocator(aflBytes);
                    app.setAppFileLocator(afl);
                } else {
                    checkForProprietaryTagOrAddToUnhandled(app, tlv);
                }
            }
        } else {
            checkForProprietaryTagOrAddToUnhandled(app, tlv);
        }
    }
    
    /*
    public static void parseAppRecord(byte[] data, EMVApp app) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        if (bis.available() < 2) {
            throw new SmartCardException("Error parsing Application Record. Data: " + Util.byteArrayToHexString(data));
        }
        BERTLV tlv = TLVUtil.getNextTLV(bis);

        if (!tlv.getTag().equals(EMVTags.RECORD_TEMPLATE)) {
            throw new SmartCardException("Error parsing Application Record: No Response Template found. Data=" + Util.byteArrayToHexString(tlv.getValueBytes()));
        }

        bis = new ByteArrayInputStream(tlv.getValueBytes());

        while (bis.available() >= 2) {
            tlv = TLVUtil.getNextTLV(bis);
            if (tlv.getTag().equals(EMVTags.CARDHOLDER_NAME)) {
                app.setCardholderName(Util.getSafePrintChars(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.TRACK1_DISCRETIONARY_DATA)) {
                app.setTrack1DiscretionaryData(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.TRACK2_DISCRETIONARY_DATA)) {
                app.setTrack2DiscretionaryData(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.TRACK_2_EQV_DATA)) {
                Track2EquivalentData t2Data = new Track2EquivalentData(tlv.getValueBytes());
                app.setTrack2EquivalentData(t2Data);
            } else if (tlv.getTag().equals(EMVTags.APP_EXPIRATION_DATE)) {
                app.setExpirationDate(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.APP_EFFECTIVE_DATE)) {
                app.setEffectiveDate(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.PAN)) {
                PAN pan = new PAN(tlv.getValueBytes());
                app.setPAN(pan);
            } else if (tlv.getTag().equals(EMVTags.PAN_SEQUENCE_NUMBER)) {
                app.setPANSequenceNumber(tlv.getValueBytes()[0]);
            } else if (tlv.getTag().equals(EMVTags.APP_USAGE_CONTROL)) {
                ApplicationUsageControl auc = new ApplicationUsageControl(tlv.getValueBytes()[0], tlv.getValueBytes()[1]);
                app.setApplicationUsageControl(auc);
            } else if (tlv.getTag().equals(EMVTags.CVM_LIST)) {
                CVMList cvmList = new CVMList(tlv.getValueBytes());
                app.setCVMList(cvmList);
            } else if (tlv.getTag().equals(EMVTags.LANGUAGE_PREFERENCE)) {
                LanguagePreference languagePreference = new LanguagePreference(tlv.getValueBytes());
                app.setLanguagePreference(languagePreference);
            } else if (tlv.getTag().equals(EMVTags.ISSUER_ACTION_CODE_DEFAULT)) {
                app.setIssuerActionCodeDefault(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_ACTION_CODE_DENIAL)) {
                app.setIssuerActionCodeDenial(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_ACTION_CODE_ONLINE)) {
                app.setIssuerActionCodeOnline(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_COUNTRY_CODE)) {
                int issuerCountryCode = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setIssuerCountryCode(issuerCountryCode);
            } else if (tlv.getTag().equals(EMVTags.APPLICATION_CURRENCY_CODE)) {
                int currencyCode = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setApplicationCurrencyCode(currencyCode);
            } else if (tlv.getTag().equals(EMVTags.APP_CURRENCY_EXPONENT)) {
                int applicationCurrencyExponent = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setApplicationCurrencyExponent(applicationCurrencyExponent);
            } else if (tlv.getTag().equals(EMVTags.APP_VERSION_NUMBER_CARD)) {
                app.setApplicationVersionNumber(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.CDOL1)) {
                DOL cdol1 = new DOL(DOL.Type.CDOL1, tlv.getValueBytes());
                app.setCDOL1(cdol1);
            } else if (tlv.getTag().equals(EMVTags.CDOL2)) {
                DOL cdol2 = new DOL(DOL.Type.CDOL2, tlv.getValueBytes());
                app.setCDOL2(cdol2);
            } else if (tlv.getTag().equals(EMVTags.LOWER_CONSEC_OFFLINE_LIMIT)) {
                app.setLowerConsecutiveOfflineLimit(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.UPPER_CONSEC_OFFLINE_LIMIT)) {
                app.setUpperConsecutiveOfflineLimit(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.SERVICE_CODE)) {
                int serviceCode = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setServiceCode(serviceCode);
            } else if (tlv.getTag().equals(EMVTags.SDA_TAG_LIST)) {
                StaticDataAuthenticationTagList staticDataAuthTagList = new StaticDataAuthenticationTagList(tlv.getValueBytes());
                app.setStaticDataAuthenticationTagList(staticDataAuthTagList);
            } else if (tlv.getTag().equals(EMVTags.CA_PUBLIC_KEY_INDEX_CARD)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    CA ca = CA.getCA(app.getAID());

                    if (ca == null) {
                        //ca == null is permitted (we might not have the CA public keys for every exotic CA)
                        Log.info("No CA configured for AID: " + app.getAID().toString());
//                        throw new SmartCardException("No CA configured for AID: "+app.getAID().toString());
                    }
                    issuerCert = new IssuerPublicKeyCertificate(ca);
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.setCAPublicKeyIndex(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.ISSUER_PUBLIC_KEY_CERT)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    issuerCert = new IssuerPublicKeyCertificate(CA.getCA(app.getAID()));
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_PUBLIC_KEY_EXP)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    issuerCert = new IssuerPublicKeyCertificate(CA.getCA(app.getAID()));
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.getIssuerPublicKey().setExponent(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_PUBLIC_KEY_REMAINDER)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    issuerCert = new IssuerPublicKeyCertificate(CA.getCA(app.getAID()));
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.getIssuerPublicKey().setRemainder(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.SIGNED_STATIC_APP_DATA)) {
                SignedStaticApplicationData ssad = app.getSignedStaticApplicationData();
                if (ssad == null) {
                    ssad = new SignedStaticApplicationData(app);
                    app.setSignedStaticApplicationData(ssad);
                }
                ssad.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PUBLIC_KEY_CERT)) {
                ICCPublicKeyCertificate iccCert = app.getICCPublicKeyCertificate();
                if (iccCert == null) {
                    iccCert = new ICCPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPublicKeyCertificate(iccCert);
                }
                iccCert.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PUBLIC_KEY_EXP)) {
                ICCPublicKeyCertificate iccCert = app.getICCPublicKeyCertificate();
                if (iccCert == null) {
                    iccCert = new ICCPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPublicKeyCertificate(iccCert);
                }
                iccCert.getICCPublicKey().setExponent(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PUBLIC_KEY_REMAINDER)) {
                ICCPublicKeyCertificate iccCert = app.getICCPublicKeyCertificate();
                if (iccCert == null) {
                    iccCert = new ICCPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPublicKeyCertificate(iccCert);
                }
                iccCert.getICCPublicKey().setRemainder(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_CERT)) {
                ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert = app.getICCPinEnciphermentPublicKeyCertificate();
                if (iccPinEnciphermentCert == null) {
                    iccPinEnciphermentCert = new ICCPinEnciphermentPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPinEnciphermentPublicKeyCertificate(iccPinEnciphermentCert);
                }
                iccPinEnciphermentCert.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_EXP)) {
                ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert = app.getICCPinEnciphermentPublicKeyCertificate();
                if (iccPinEnciphermentCert == null) {
                    iccPinEnciphermentCert = new ICCPinEnciphermentPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPinEnciphermentPublicKeyCertificate(iccPinEnciphermentCert);
                }
                iccPinEnciphermentCert.getICCPublicKey().setExponent(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_REM)) {
                ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert = app.getICCPinEnciphermentPublicKeyCertificate();
                if (iccPinEnciphermentCert == null) {
                    iccPinEnciphermentCert = new ICCPinEnciphermentPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPinEnciphermentPublicKeyCertificate(iccPinEnciphermentCert);
                }
                iccPinEnciphermentCert.getICCPublicKey().setRemainder(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.DDOL)) {
                DOL ddol = new DOL(DOL.Type.DDOL, tlv.getValueBytes());
                app.setDDOL(ddol);
            } else if (tlv.getTag().equals(EMVTags.IBAN)) {
                app.setIBAN(new IBAN(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.BANK_IDENTIFIER_CODE)) {
                app.setBIC(new BankIdentifierCode(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.APP_DISCRETIONARY_DATA)) {
                app.setDiscretionaryData(tlv.getValueBytes());
            } else {
                checkForProprietaryTagOrAddToUnhandled(app, tlv);
            }
        }
    }
    */    
}
