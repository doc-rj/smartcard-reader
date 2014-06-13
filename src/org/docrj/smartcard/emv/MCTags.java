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

import org.docrj.smartcard.iso7816.Tag;
import org.docrj.smartcard.iso7816.TagImpl;
import org.docrj.smartcard.iso7816.TagValueType;

public class MCTags {

    public static final Tag MASTERCARD_UPPER_OFFLINE_AMOUNT      = new TagImpl("9f52", TagValueType.BINARY, "Upper Cumulative Domestic Offline Transaction Amount", "Issuer specified data element indicating the required maximum cumulative offline amount allowed for the application before the transaction goes online.");

//    //EMV-CAP tags (see also VISA Tags)
//    //9f55 01 c0
//    //9f55 01 00
//    public static final Tag TAG_9f55                             = new TagImpl("9f55", TagValueType.BINARY, "?", "");
   
    //EMV Cap
    //9f56 0c 0f00007fffffe00000000000
    //9f56 1d 00007fffffe00000000000000000000000000000000000000000000000
    //9f56 0b 00 00 7f ff ff 00 00 00 00 00 00
    public static final Tag TAG_9f56                             = new TagImpl("9f56", TagValueType.BINARY, "?", "");

    //Example: BER-TLV[9f6c, 02 (raw 02), 0001]
    public static final Tag MAG_STRIPE_APP_VERSION_NUMBER_CARD   = new TagImpl("9f6c", TagValueType.BINARY, "Mag Stripe Application Version Number (Card)", "Must be personalized with the value 0x0001");

    //Transaction log data 
    //df3e 01 01
    public static final Tag TAG_df3e                             = new TagImpl("df3e", TagValueType.BINARY, "?", "");
    
    //TODO are these issuer specific?
        //Card from portugal (TODO find issuer code)
//    a000000004 Unhandled tags:
//
//    df48 02 0620
//    df40 01 00
//    df27 08 0103000000000000
//    df28 10 ffffffffffffffffffffffffffffffff
//    df47 01 01
//    df49 0d 48000001011900000000000000
//    df44 28 00000000000000000000202020202020202020202020202020202020202020202020202020202020
//    df45 22 00000000202020202020202020202020202020202020202020202020202020202020
//    df46 03 000000
//
//
//    501649ff20 Unhandled tags:
//
//    df48 02 0620
//    df40 01 00
//    df27 08 0100000000000000
//    df28 10 ffffffffffffffffffffffffffffffff
//    df47 01 01
//    df49 0d 00000001010000000000000000
    
    
    
    //TODO refactor to Kernel 2 Tags
    //These are specified in EMV Contactless (Book C-2) "MasterCard"
    public static final Tag OFFLINE_ACCUMULATOR_BALANCE             = new TagImpl("9f50", TagValueType.BINARY, "Offline Accumulator Balance", "Represents the amount of offline spending available in the Card.");
    //9f51 03 9f 37 04 
    public static final Tag DRDOL                                   = new TagImpl("9f51", TagValueType.BINARY, "DRDOL", "A data object in the Card that provides the Kernel with a list of data objects that must be passed to the Card in the data field of the RECOVER AC command");
    public static final Tag TRANSACTION_CATEGORY_CODE               = new TagImpl("9f53", TagValueType.BINARY, "Transaction Category Code", "");
    public static final Tag DS_ODS_CARD                             = new TagImpl("9f54", TagValueType.BINARY, "DS ODS Card", "");
    public static final Tag MOBILE_SUPPORT_INDICATOR                = new TagImpl("9f55", TagValueType.BINARY, "Mobile Support Indicator", "");
    public static final Tag DSDOL                                   = new TagImpl("9f5b", TagValueType.BINARY, "DSDOL", "");
    public static final Tag DS_REQUESTED_OPERATOR_ID                = new TagImpl("9f5c", TagValueType.BINARY, "DS Requested Operator ID", "");
    //9f5d 01 01
    public static final Tag APPLICATION_CAPABILITIES_INFORMATION    = new TagImpl("9f5d", TagValueType.BINARY, "Application Capabilities Information", "Lists a number of card features beyond regular payment");
    public static final Tag DS_ID                                   = new TagImpl("9f5e", TagValueType.BINARY, "Data Storage Identifier", "Constructed as follows: Application PAN (without any 'F' padding) || Application PAN Sequence Number (+ zero padding)");
    public static final Tag DS_SLOT_AVAILABILITY                    = new TagImpl("9f5f", TagValueType.BINARY, "DS Slot Availability", "");
    public static final Tag CVC3_TRACK1                             = new TagImpl("9f60", TagValueType.BINARY, "CVC3 (Track1)", "The CVC3 (Track1) is a 2-byte cryptogram returned by the Card in the response to the COMPUTE CRYPTOGRAPHIC CHECKSUM command.");
    public static final Tag CVC3_TRACK2                             = new TagImpl("9f61", TagValueType.BINARY, "CVC3 (Track2)", "The CVC3 (Track2) is a 2-byte cryptogram returned by the Card in the response to the COMPUTE CRYPTOGRAPHIC CHECKSUM command.");
    //9f62 06 00 00 00 00 00 0e
    //9f62 06 00 00 00 03 80 00
    public static final Tag PCVC3_TRACK1                            = new TagImpl("9f62", TagValueType.BINARY, "Track 1 bit map for CVC3", "PCVC3(Track1) indicates to the Kernel the positions in the discretionary data field of the Track 1 Data where the CVC3 (Track1) digits must be copied");
    //9f63 06 00 00 00 00 07 f0
    //9f63 06 00 00 00 00 07 8e
    public static final Tag PUNTAC_TRACK1                           = new TagImpl("9f63", TagValueType.BINARY, "Track 1 bit map for UN and ATC", "PUNATC(Track1) indicates to the Kernel the positions in the discretionary data field of Track 1 Data where the Unpredictable Number (Numeric) digits and Application Transaction Counter digits have to be copied.");
    //9f64 01 03
    //9f64 01 04
    public static final Tag NATC_TRACK1                             = new TagImpl("9f64", TagValueType.BINARY, "Track 1 number of ATC digits", "The value of NATC(Track1) represents the number of digits of the Application Transaction Counter to be included in the discretionary data field of Track 1 Data");
    //9f65 02 000e
    //9f65 02 0070
    public static final Tag PCVC_TRACK2                             = new TagImpl("9f65", TagValueType.BINARY, "Track 2 bit map for CVC3", "PCVC3(Track2) indicates to the Kernel the positions in the discretionary data field of the Track 2 Data where the CVC3 (Track2) digits must be copied");
    //9f66 02 07f0
    //9f66 02 1e0e
    public static final Tag PUNTAC_TRACK2                           = new TagImpl("9f66", TagValueType.BINARY, "Track 2 bit map for UN and ATC", "PUNATC(Track2) indicates to the Kernel the positions in the discretionary data field of Track 2 Data where the Unpredictable Number (Numeric) digits and Application Transaction Counter digits have to be copied.");
    //9f67 01 03
    //9f67 01 04
    public static final Tag NATC_TRACK2                             = new TagImpl("9f67", TagValueType.BINARY, "Track 2 number of ATC digits", "The value of NATC(Track2) represents the number of digits of the Application Transaction Counter to be included in the discretionary data field of Track 2 Data");
    public static final Tag UDOL                                    = new TagImpl("9f69", TagValueType.BINARY, "UDOL", "");
    public static final Tag UNPREDICTABLE_NUMBER_NUMERIC            = new TagImpl("9f6a", TagValueType.BINARY, "Unpredictable Number (Numeric)", "");
    public static final Tag MAG_STRIPE_APP_VERSION_NUMBER_READER    = new TagImpl("9f6d", TagValueType.BINARY, "Mag-stripe Application Version Number (Reader)", "");
    public static final Tag THIRD_PARTY_DATA                        = new TagImpl("9f6e", TagValueType.BINARY, "Third Party Data", "");
    public static final Tag DS_SLOT_MANAGEMENT_CONTROL              = new TagImpl("9f6f", TagValueType.BINARY, "DS Slot Management Control", "");
    public static final Tag PROTECTED_DATA_ENVELOPE_1               = new TagImpl("9f70", TagValueType.BINARY, "Protected Data Envelope 1", "");
    public static final Tag PROTECTED_DATA_ENVELOPE_2               = new TagImpl("9f71", TagValueType.BINARY, "Protected Data Envelope 2", "");
    public static final Tag PROTECTED_DATA_ENVELOPE_3               = new TagImpl("9f72", TagValueType.BINARY, "Protected Data Envelope 3", "");
    public static final Tag PROTECTED_DATA_ENVELOPE_4               = new TagImpl("9f73", TagValueType.BINARY, "Protected Data Envelope 4", "");
    public static final Tag PROTECTED_DATA_ENVELOPE_5               = new TagImpl("9f74", TagValueType.BINARY, "Protected Data Envelope 5", "");
    public static final Tag UNPROTECTED_DATA_ENVELOPE_1             = new TagImpl("9f75", TagValueType.BINARY, "Unprotected Data Envelope 1", "");
    public static final Tag UNPROTECTED_DATA_ENVELOPE_2             = new TagImpl("9f76", TagValueType.BINARY, "Unprotected Data Envelope 2", "");
    public static final Tag UNPROTECTED_DATA_ENVELOPE_3             = new TagImpl("9f77", TagValueType.BINARY, "Unprotected Data Envelope 3", "");
    public static final Tag UNPROTECTED_DATA_ENVELOPE_4             = new TagImpl("9f78", TagValueType.BINARY, "Unprotected Data Envelope 4", "");
    public static final Tag UNPROTECTED_DATA_ENVELOPE_5             = new TagImpl("9f79", TagValueType.BINARY, "Unprotected Data Envelope 5", "");
    public static final Tag MERCHANT_CUSTOM_DATA                    = new TagImpl("9f7c", TagValueType.BINARY, "Merchant Custom Data", "");
    public static final Tag DS_SUMMARY_1                            = new TagImpl("9f7d", TagValueType.BINARY, "DS Summary 1", "");
    public static final Tag DS_UNPREDICTABLE_NUMBER                 = new TagImpl("9f7f", TagValueType.BINARY, "DS Unpredictable Number", "");

    public static final Tag POS_CARDHOLDER_INTERACTION_INFORMATION  = new TagImpl("df4b", TagValueType.BINARY, "POS Cardholder Interaction Information", "");
    public static final Tag DS_INPUT_CARD                           = new TagImpl("df60", TagValueType.BINARY, "DS Input (Card)", "");
    public static final Tag DS_DIGEST_H                             = new TagImpl("df61", TagValueType.BINARY, "DS Digest H", "");
    public static final Tag DS_ODS_INFO                             = new TagImpl("df62", TagValueType.BINARY, "DS ODS Info", "");
    public static final Tag DS_ODS_TERM                             = new TagImpl("df63", TagValueType.BINARY, "DS ODS Term", "");
    public static final Tag BALANCE_READ_BEFORE_GEN_AC              = new TagImpl("df8104", TagValueType.BINARY, "Balance Read Before Gen AC", "");
    public static final Tag BALANCE_READ_AFTER_GEN_AC               = new TagImpl("df8105", TagValueType.BINARY, "Balance Read After Gen AC", "");
    public static final Tag DATA_NEEDED                             = new TagImpl("df8106", TagValueType.BINARY, "Data Needed", "");
    public static final Tag CDOL1_RELATED_DATA                      = new TagImpl("df8107", TagValueType.BINARY, "CDOL1 Related Data", "");
    public static final Tag DS_AC_TYPE                              = new TagImpl("df8108", TagValueType.BINARY, "DS AC Type", "");
    public static final Tag DS_INPUT_TERM                           = new TagImpl("df8109", TagValueType.BINARY, "DS Input (Term)", "");
    public static final Tag DS_ODS_INFO_FOR_READER                  = new TagImpl("df810a", TagValueType.BINARY, "DS ODS Info For Reader", "");
    public static final Tag DS_SUMMARY_STATUS                       = new TagImpl("df810b", TagValueType.BINARY, "DS Summary Status", "");
    public static final Tag KERNEL_ID                               = new TagImpl("df810c", TagValueType.BINARY, "Kernel ID", "");
    public static final Tag DSVN_TERM                               = new TagImpl("df810d", TagValueType.BINARY, "DSVN Term", "");
    public static final Tag POST_GEN_AC_PUT_DATA_STATUS             = new TagImpl("df810e", TagValueType.BINARY, "Post-Gen AC Put Data Status", "");
    public static final Tag PRE_GEN_AC_PUT_DATA_STATUS              = new TagImpl("df810f", TagValueType.BINARY, "Pre-Gen AC Put Data Status", "");
    public static final Tag PROCEED_TO_WRITE_FIRST_FLAG             = new TagImpl("df8110", TagValueType.BINARY, "Proceed To First Write Flag", "");
    public static final Tag PDOL_RELATED_DATA                       = new TagImpl("df8111", TagValueType.BINARY, "PDOL Related Data", "");
    public static final Tag TAGS_TO_READ                            = new TagImpl("df8112", TagValueType.BINARY, "Tags To Read", "");
    public static final Tag DRDOL_RELATED_DATA                      = new TagImpl("df8113", TagValueType.BINARY, "DRDOL Related Data", "");
    public static final Tag REFERENCE_CONTROL_PARAMETER             = new TagImpl("df8114", TagValueType.BINARY, "Reference Control Parameter", "");
    public static final Tag ERROR_INDICATION                        = new TagImpl("df8115", TagValueType.BINARY, "Error Indication", "");
    public static final Tag USER_INTERFACE_REQUEST_DATA             = new TagImpl("df8116", TagValueType.BINARY, "User Interface Request Data", "");
    public static final Tag CARD_DATA_INPUT_CAPABILITY              = new TagImpl("df8117", TagValueType.BINARY, "Card Data Input Capability", "");
    public static final Tag CMV_CAPABILITY_CMV_REQUIRED             = new TagImpl("df8118", TagValueType.BINARY, "CVM Capability - CVM Required", "");
    public static final Tag CMV_CAPABILITY_NO_CMV_REQUIRED          = new TagImpl("df8119", TagValueType.BINARY, "CVM Capability - No CVM Required", "");
    public static final Tag DEFAULT_UDOL                            = new TagImpl("df811a", TagValueType.BINARY, "Default UDOL", "");
    public static final Tag KERNEL_CONFIGURATION                    = new TagImpl("df811b", TagValueType.BINARY, "Kernel Configuration", "");
    public static final Tag MAX_LIFETIME_TORN_TRANSACTION_LOG_REC   = new TagImpl("df811c", TagValueType.BINARY, "Max Lifetime of Torn Transaction Log Record", "");
    public static final Tag MAX_NUMBER_TORN_TRANSACTION_LOG_REC     = new TagImpl("df811d", TagValueType.BINARY, "Max Number of Torn Transaction Log Records", "");
    public static final Tag MAG_STRIPE_CMV_CAPABILITY_CMV_REQUIRED  = new TagImpl("df811e", TagValueType.BINARY, "Mag-stripe CVM Capability – CVM Required", "");
    public static final Tag SECURITY_CAPABILITY                     = new TagImpl("df811f", TagValueType.BINARY, "Security Capability", "");
    public static final Tag TERMINAL_ACTION_CODE_DEFAULT            = new TagImpl("df8120", TagValueType.BINARY, "Terminal Action Code – Default", "");
    public static final Tag TERMINAL_ACTION_CODE_DENIAL             = new TagImpl("df8121", TagValueType.BINARY, "Terminal Action Code – Denial", "");
    public static final Tag TERMINAL_ACTION_CODE_ONLINE             = new TagImpl("df8122", TagValueType.BINARY, "Terminal Action Code – Online", "");
    public static final Tag READER_CONTACTLESS_FLOOR_LIMIT          = new TagImpl("df8123", TagValueType.BINARY, "Reader Contactless Floor Limit", "");
    public static final Tag READER_CL_TRANSACTION_LIMIT_NO_CMV      = new TagImpl("df8124", TagValueType.BINARY, "Reader Contactless Transaction Limit (No On-device CVM)", "");
    public static final Tag READER_CL_TRANSACTION_LIMIT_CVM         = new TagImpl("df8125", TagValueType.BINARY, "Reader Contactless Transaction Limit (On-device CVM)", "");
    public static final Tag READER_CMV_REQUIRED_LIMIT               = new TagImpl("df8126", TagValueType.BINARY, "Reader CVM Required Limit", "");
    public static final Tag TIME_OUT_VALUE                          = new TagImpl("df8127", TagValueType.BINARY, "TIME_OUT_VALUE", "");
    public static final Tag IDS_STATUS                              = new TagImpl("df8128", TagValueType.BINARY, "IDS Status", "");
    public static final Tag OUTCOME_PARAMETER_SET                   = new TagImpl("df8129", TagValueType.BINARY, "Outcome Parameter Set", "");
    public static final Tag DD_CARD_TRACK1                          = new TagImpl("df812a", TagValueType.BINARY, "DD Card (Track1)", "");
    public static final Tag DD_CARD_TRACK2                          = new TagImpl("df812b", TagValueType.BINARY, "DD Card (Track2)", "");
    public static final Tag MAG_STRIPE_CMV_CAPABILITY_NO_CMV_REQ    = new TagImpl("df812c", TagValueType.BINARY, "Mag-stripe CVM Capability – No CVM Required", "");
    public static final Tag MESSAGE_HOLD_TIME                       = new TagImpl("df812d", TagValueType.BINARY, "Message Hold Time", "");
    
    public static final Tag TORN_RECORD                             = new TagImpl("ff8101", TagValueType.BINARY, "Torn Record", "");
    public static final Tag TAGS_TO_WRITE_BEFORE_GEN_AC             = new TagImpl("ff8102", TagValueType.BINARY, "Tags To Write Before Gen AC", "");
    public static final Tag TAGS_TO_WRITE_AFTER_GEN_AC              = new TagImpl("ff8103", TagValueType.BINARY, "Tags To Write After Gen AC", "");
    public static final Tag DATA_TO_SEND                            = new TagImpl("ff8104", TagValueType.BINARY, "Data To Send", "");
    public static final Tag DATA_RECORD                             = new TagImpl("ff8105", TagValueType.BINARY, "Data Record", "");
    public static final Tag DISCRETIONARY_DATA                      = new TagImpl("ff8106", TagValueType.BINARY, "Discretionary Data", "");

    
}