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

import org.docrj.smartcard.iso7816.TagValueType;
import org.docrj.smartcard.iso7816.TagImpl;
import org.docrj.smartcard.iso7816.Tag;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.docrj.smartcard.emv.MCTags;
import org.docrj.smartcard.emv.VISATags;
import org.docrj.smartcard.util.ByteArrayWrapper;
import org.docrj.smartcard.util.Util;

/**
 * http://www.emvlab.org/emvtags/all/
 *
 * The coding of primitive context-specific class data objects in the ranges '80' to '9E' and
 * '9F00' to '9F4F' is reserved for EMV specification.
 * 
 * The coding of primitive context-specific class data objects in the range '9F50' to '9F7F'
 * is reserved for the payment systems.
 */
public class EMVTags {

    private static LinkedHashMap<IssuerIdNumber, LinkedHashMap<ByteArrayWrapper, Tag>> issuerToTagsMap =
        new LinkedHashMap<IssuerIdNumber, LinkedHashMap<ByteArrayWrapper, Tag>>();
    private static LinkedHashMap<ByteArrayWrapper, LinkedHashMap<ByteArrayWrapper, Tag>> paymentSystemToTagsMap =
        new LinkedHashMap<ByteArrayWrapper, LinkedHashMap<ByteArrayWrapper, Tag>>();

    private static LinkedHashMap<ByteArrayWrapper, Tag> tags = new LinkedHashMap<ByteArrayWrapper, Tag>();
    // one-byte tags
    // 7816-4 inter-industry data object for tag allocation authority
    public static final Tag UNIVERSAL_TAG_FOR_OID                   = new TagImpl("06", TagValueType.BINARY, "Object Identifier (OID)", "Universal tag for OID");
    public static final Tag COUNTRY_CODE                            = new TagImpl("41", TagValueType.NUMERIC, "Country Code", "Country code (encoding specified in ISO 3166-1) and optional national data");
    public static final Tag ISSUER_IDENTIFICATION_NUMBER            = new TagImpl("42", TagValueType.NUMERIC, "Issuer ID Number (IIN)", "The number that identifies the major industry and the card issuer and that forms the first part of the Primary Account Number (PAN)");

    // 7816-4 inter-industry data objects for application identification and selection
    public static final Tag AID_CARD                                = new TagImpl("4f", TagValueType.BINARY, "App Identifier (AID)", "Identifies the application as described in ISO/IEC 7816-5");
    public static final Tag APPLICATION_LABEL                       = new TagImpl("50", TagValueType.TEXT, "App Label", "Mnemonic associated with the AID according to ISO/IEC 7816-5");
    public static final Tag PATH                                    = new TagImpl("51", TagValueType.BINARY, "File reference data element", "ISO-7816 Path");
    public static final Tag COMMAND_APDU                            = new TagImpl("52", TagValueType.BINARY, "Command APDU", "");
    public static final Tag DISCRETIONARY_DATA_OR_TEMPLATE          = new TagImpl("53", TagValueType.BINARY, "Discretionary data (or template)", "");
    public static final Tag APPLICATION_TEMPLATE                    = new TagImpl("61", TagValueType.BINARY, "App Template", "Contains one or more data objects relevant to an application directory entry according to ISO/IEC 7816-5");
    public static final Tag FCI_TEMPLATE                            = new TagImpl("6f", TagValueType.BINARY, "File Control Info (FCI) Template", "Set of file control parameters and file management data (according to ISO/IEC 7816-4)");
    public static final Tag DD_TEMPLATE                             = new TagImpl("73", TagValueType.BINARY, "Directory Discretionary Template", "Issuer discretionary part of the directory according to ISO/IEC 7816-5");
    public static final Tag DEDICATED_FILE_NAME                     = new TagImpl("84", TagValueType.BINARY, "Dedicated File (DF) Name", "Identifies the name of the DF as described in ISO/IEC 7816-4");
    public static final Tag SFI                                     = new TagImpl("88", TagValueType.BINARY, "Short File Identifier (SFI)", "Identifies the SFI to be used in the commands related to a given AEF or DDF. The SFI data object is a binary field with the three high order bits set to zero");
    
    public static final Tag FCI_PROPRIETARY_TEMPLATE                = new TagImpl("a5", TagValueType.BINARY, "File Control Info (FCI) Proprietary Template", "Identifies the data object proprietary to this specification in the FCI template according to ISO/IEC 7816-4");
    public static final Tag ISSUER_URL                              = new TagImpl("5f50", TagValueType.TEXT, "Issuer URL", "The URL provides the location of the Issuer’s Library Server on the Internet");
    
    // EMV
    public static final Tag TRACK_2_EQV_DATA                        = new TagImpl("57", TagValueType.BINARY, "Track 2 Equivalent Data", "Contains the data elements of track 2 according to ISO/IEC 7813, excluding start sentinel, end sentinel, and Longitudinal Redundancy Check (LRC)");
    public static final Tag PAN                                     = new TagImpl("5a", TagValueType.NUMERIC, "App Primary Account Number (PAN)", "Valid cardholder account number");
    public static final Tag RECORD_TEMPLATE                         = new TagImpl("70", TagValueType.BINARY, "Record Template (EMV Proprietary)", "Template proprietary to the EMV specification");
    public static final Tag ISSUER_SCRIPT_TEMPLATE_1                = new TagImpl("71", TagValueType.BINARY, "Issuer Script Template 1", "Contains proprietary issuer data for transmission to the ICC before the second GENERATE AC command");
    public static final Tag ISSUER_SCRIPT_TEMPLATE_2                = new TagImpl("72", TagValueType.BINARY, "Issuer Script Template 2", "Contains proprietary issuer data for transmission to the ICC after the second GENERATE AC command");
    public static final Tag RESPONSE_MESSAGE_TEMPLATE_2             = new TagImpl("77", TagValueType.BINARY, "Response Message Template Format 2", "Contains the data objects (with tags and lengths) returned by the ICC in response to a command");
    public static final Tag RESPONSE_MESSAGE_TEMPLATE_1             = new TagImpl("80", TagValueType.BINARY, "Response Message Template Format 1", "Contains the data objects (without tags and lengths) returned by the ICC in response to a command");
    public static final Tag AMOUNT_AUTHORISED_BINARY                = new TagImpl("81", TagValueType.BINARY, "Amount, Authorised (Binary)", "Authorised amount of the transaction (excluding adjustments)");
    public static final Tag APPLICATION_INTERCHANGE_PROFILE         = new TagImpl("82", TagValueType.BINARY, "App Interchange Profile", "Indicates the capabilities of the card to support specific functions in the application");
    public static final Tag COMMAND_TEMPLATE                        = new TagImpl("83", TagValueType.BINARY, "Command Template", "Identifies the data field of a command message");
    public static final Tag ISSUER_SCRIPT_COMMAND                   = new TagImpl("86", TagValueType.BINARY, "Issuer Script Command", "Contains a command for transmission to the ICC");
    public static final Tag APPLICATION_PRIORITY_INDICATOR          = new TagImpl("87", TagValueType.BINARY, "App Priority Indicator", "Indicates the priority of a given application or group of applications in a directory");
    public static final Tag AUTHORISATION_CODE                      = new TagImpl("89", TagValueType.BINARY, "Authorisation Code", "Value generated by the authorisation authority for an approved transaction");
    public static final Tag AUTHORISATION_RESPONSE_CODE             = new TagImpl("8a", TagValueType.TEXT, "Authorisation Response Code", "Code that defines the disposition of a message");
    public static final Tag CDOL1                                   = new TagImpl("8c", TagValueType.DOL, "Card Risk Management Data Object List 1 (CDOL1)", "List of data objects (tag and length) to be passed to the ICC in the first GENERATE AC command");
    public static final Tag CDOL2                                   = new TagImpl("8d", TagValueType.DOL, "Card Risk Management Data Object List 2 (CDOL2)", "List of data objects (tag and length) to be passed to the ICC in the second GENERATE AC command");
    public static final Tag CVM_LIST                                = new TagImpl("8e", TagValueType.BINARY, "Cardholder Verification Method (CVM) List", "Identifies a method of verification of the cardholder supported by the application");
    public static final Tag CA_PUBLIC_KEY_INDEX_CARD                = new TagImpl("8f", TagValueType.BINARY, "Certification Authority Public Key Index - card", "Identifies the certification authority’s public key in conjunction with the RID");
    public static final Tag ISSUER_PUBLIC_KEY_CERT                  = new TagImpl("90", TagValueType.BINARY, "Issuer Public Key Certificate", "Issuer public key certified by a certification authority");
    public static final Tag ISSUER_AUTHENTICATION_DATA              = new TagImpl("91", TagValueType.BINARY, "Issuer Authentication Data", "Data sent to the ICC for online issuer authentication");
    public static final Tag ISSUER_PUBLIC_KEY_REMAINDER             = new TagImpl("92", TagValueType.BINARY, "Issuer Public Key Remainder", "Remaining digits of the Issuer Public Key Modulus");
    public static final Tag SIGNED_STATIC_APP_DATA                  = new TagImpl("93", TagValueType.BINARY, "Signed Static App Data", "Digital signature on critical application parameters for SDA");
    public static final Tag APPLICATION_FILE_LOCATOR                = new TagImpl("94", TagValueType.BINARY, "App File Locator (AFL)", "Indicates the location (SFI, range of records) of the AEFs related to a given application");
    public static final Tag TERMINAL_VERIFICATION_RESULTS           = new TagImpl("95", TagValueType.BINARY, "Terminal Verification Results (TVR)", "Status of the different functions as seen from the terminal");
    public static final Tag TDOL                                    = new TagImpl("97", TagValueType.BINARY, "Transaction Certificate Data Object List (TDOL)", "List of data objects (tag and length) to be used by the terminal in generating the TC Hash Value");
    public static final Tag TC_HASH_VALUE                           = new TagImpl("98", TagValueType.BINARY, "Transaction Certificate (TC) Hash Value", "Result of a hash function specified in Book 2, Annex B3.1");
    public static final Tag TRANSACTION_PIN_DATA                    = new TagImpl("99", TagValueType.BINARY, "Transaction Personal ID Number (PIN) Data", "Data entered by the cardholder for the purpose of the PIN verification");
    public static final Tag TRANSACTION_DATE                        = new TagImpl("9a", TagValueType.NUMERIC, "Transaction Date", "Local date that the transaction was authorised");
    public static final Tag TRANSACTION_STATUS_INFORMATION          = new TagImpl("9b", TagValueType.BINARY, "Transaction Status Info", "Indicates the functions performed in a transaction");
    public static final Tag TRANSACTION_TYPE                        = new TagImpl("9c", TagValueType.NUMERIC, "Transaction Type", "Indicates the type of financial transaction, represented by the first two digits of ISO 8583:1987 Processing Code");
    public static final Tag DDF_NAME                                = new TagImpl("9d", TagValueType.BINARY, "Directory Definition File (DDF) Name", "Identifies the name of a DF associated with a directory");
    // two-byte tags
    public static final Tag CARDHOLDER_NAME                         = new TagImpl("5f20", TagValueType.TEXT, "Cardholder Name", "Indicates cardholder name according to ISO 7813");
    public static final Tag APP_EXPIRATION_DATE                     = new TagImpl("5f24", TagValueType.NUMERIC, "App Expiration Date", "Date after which application expires");
    public static final Tag APP_EFFECTIVE_DATE                      = new TagImpl("5f25", TagValueType.NUMERIC, "App Effective Date", "Date from which the application may be used");
    public static final Tag ISSUER_COUNTRY_CODE                     = new TagImpl("5f28", TagValueType.NUMERIC, "Issuer Country Code", "Indicates the country of the issuer according to ISO 3166");
    public static final Tag TRANSACTION_CURRENCY_CODE               = new TagImpl("5f2a", TagValueType.TEXT, "Transaction Currency Code", "Indicates the currency code of the transaction according to ISO 4217");
    public static final Tag LANGUAGE_PREFERENCE                     = new TagImpl("5f2d", TagValueType.TEXT, "Language Preference", "1–4 languages stored in order of preference, each represented by 2 alphabetical characters according to ISO 639");
    public static final Tag SERVICE_CODE                            = new TagImpl("5f30", TagValueType.NUMERIC, "Service Code", "Service code as defined in ISO/IEC 7813 for track 1 and track 2");
    public static final Tag PAN_SEQUENCE_NUMBER                     = new TagImpl("5f34", TagValueType.NUMERIC, "App Primary Account Number (PAN) Sequence Number", "Identifies and differentiates cards with the same PAN");
    public static final Tag TRANSACTION_CURRENCY_EXP                = new TagImpl("5f36", TagValueType.NUMERIC, "Transaction Currency Exponent", "Indicates the implied position of the decimal point from the right of the transaction amount represented according to ISO 4217");
    public static final Tag IBAN                                    = new TagImpl("5f53", TagValueType.BINARY, "International Bank Account Number (IBAN)", "Uniquely identifies the account of a customer at a financial institution as defined in ISO 13616");
    public static final Tag BANK_IDENTIFIER_CODE                    = new TagImpl("5f54", TagValueType.MIXED, "Bank Identifier Code (BIC)", "Uniquely identifies a bank as defined in ISO 9362");
    public static final Tag ISSUER_COUNTRY_CODE_ALPHA2              = new TagImpl("5f55", TagValueType.TEXT, "Issuer Country Code (alpha2 format)", "Indicates the country of the issuer as defined in ISO 3166 (using a 2 character alphabetic code)");
    public static final Tag ISSUER_COUNTRY_CODE_ALPHA3              = new TagImpl("5f56", TagValueType.TEXT, "Issuer Country Code (alpha3 format)", "Indicates the country of the issuer as defined in ISO 3166 (using a 3 character alphabetic code)");
    public static final Tag ACQUIRER_IDENTIFIER                     = new TagImpl("9f01", TagValueType.NUMERIC, "Acquirer Identifier", "Uniquely identifies the acquirer within each payment system");
    public static final Tag AMOUNT_AUTHORISED_NUMERIC               = new TagImpl("9f02", TagValueType.NUMERIC, "Amount, Authorised (Numeric)", "Authorised amount of the transaction (excluding adjustments)");
    public static final Tag AMOUNT_OTHER_NUMERIC                    = new TagImpl("9f03", TagValueType.NUMERIC, "Amount, Other (Numeric)", "Secondary amount associated with the transaction representing a cashback amount");
    public static final Tag AMOUNT_OTHER_BINARY                     = new TagImpl("9f04", TagValueType.NUMERIC, "Amount, Other (Binary)", "Secondary amount associated with the transaction representing a cashback amount");
    public static final Tag APP_DISCRETIONARY_DATA                  = new TagImpl("9f05", TagValueType.BINARY, "App Discretionary Data", "Issuer or payment system specified data relating to the application");
    public static final Tag AID_TERMINAL                            = new TagImpl("9f06", TagValueType.BINARY, "App Identifier (AID) - terminal", "Identifies the application as described in ISO/IEC 7816-5");
    public static final Tag APP_USAGE_CONTROL                       = new TagImpl("9f07", TagValueType.BINARY, "App Usage Control", "Indicates issuer’s specified restrictions on the geographic usage and services allowed for the application");
    public static final Tag APP_VERSION_NUMBER_CARD                 = new TagImpl("9f08", TagValueType.BINARY, "App Version Number - card", "Version number assigned by the payment system for the application");
    public static final Tag APP_VERSION_NUMBER_TERMINAL             = new TagImpl("9f09", TagValueType.BINARY, "App Version Number - terminal", "Version number assigned by the payment system for the application");
    public static final Tag CARDHOLDER_NAME_EXTENDED                = new TagImpl("9f0b", TagValueType.TEXT, "Cardholder Name Extended", "Indicates the whole cardholder name when greater than 26 characters using the same coding convention as in ISO 7813");
    public static final Tag ISSUER_ACTION_CODE_DEFAULT              = new TagImpl("9f0d", TagValueType.BINARY, "Issuer Action Code - Default", "Specifies the issuer’s conditions that cause a transaction to be rejected if it might have been approved online, but the terminal is unable to process the transaction online");
    public static final Tag ISSUER_ACTION_CODE_DENIAL               = new TagImpl("9f0e", TagValueType.BINARY, "Issuer Action Code - Denial", "Specifies the issuer’s conditions that cause the denial of a transaction without attempt to go online");
    public static final Tag ISSUER_ACTION_CODE_ONLINE               = new TagImpl("9f0f", TagValueType.BINARY, "Issuer Action Code - Online", "Specifies the issuer’s conditions that cause a transaction to be transmitted online");
    public static final Tag ISSUER_APPLICATION_DATA                 = new TagImpl("9f10", TagValueType.BINARY, "Issuer App Data", "Contains proprietary application data for transmission to the issuer in an online transaction");
    public static final Tag ISSUER_CODE_TABLE_INDEX                 = new TagImpl("9f11", TagValueType.NUMERIC, "Issuer Code Table Index", "Indicates the code table according to ISO/IEC 8859 for displaying the App Preferred Name");
    public static final Tag APP_PREFERRED_NAME                      = new TagImpl("9f12", TagValueType.TEXT, "App Preferred Name", "Preferred mnemonic associated with the AID");
    public static final Tag LAST_ONLINE_ATC_REGISTER                = new TagImpl("9f13", TagValueType.BINARY, "Last Online App Transaction Counter (ATC) Register", "ATC value of the last transaction that went online");
    public static final Tag LOWER_CONSEC_OFFLINE_LIMIT              = new TagImpl("9f14", TagValueType.BINARY, "Lower Consecutive Offline Limit", "Issuer-specified preference for the maximum number of consecutive offline transactions for this ICC application allowed in a terminal with online capability");
    public static final Tag MERCHANT_CATEGORY_CODE                  = new TagImpl("9f15", TagValueType.NUMERIC, "Merchant Category Code", "Classifies the type of business being done by the merchant, represented according to ISO 8583:1993 for Card Acceptor Business Code");
    public static final Tag MERCHANT_IDENTIFIER                     = new TagImpl("9f16", TagValueType.TEXT, "Merchant Identifier", "When concatenated with the Acquirer Identifier, uniquely identifies a given merchant");
    public static final Tag PIN_TRY_COUNTER                         = new TagImpl("9f17", TagValueType.BINARY, "Personal ID Number (PIN) Try Counter", "Number of PIN tries remaining");
    public static final Tag ISSUER_SCRIPT_IDENTIFIER                = new TagImpl("9f18", TagValueType.BINARY, "Issuer Script Identifier", "ID of the Issuer Script");
    public static final Tag TERMINAL_COUNTRY_CODE                   = new TagImpl("9f1a", TagValueType.TEXT, "Terminal Country Code", "Indicates the country of the terminal, represented according to ISO 3166");
    public static final Tag TERMINAL_FLOOR_LIMIT                    = new TagImpl("9f1b", TagValueType.BINARY, "Terminal Floor Limit", "Indicates the floor limit in the terminal in conjunction with the AID");
    public static final Tag TERMINAL_IDENTIFICATION                 = new TagImpl("9f1c", TagValueType.TEXT, "Terminal ID", "Designates the unique location of a terminal at a merchant");
    public static final Tag TERMINAL_RISK_MANAGEMENT_DATA           = new TagImpl("9f1d", TagValueType.BINARY, "Terminal Risk Management Data", "App-specific value used by the card for risk management purposes");
    public static final Tag INTERFACE_DEVICE_SERIAL_NUMBER          = new TagImpl("9f1e", TagValueType.TEXT, "Interface Device (IFD) Serial Number", "Unique and permanent serial number assigned to the IFD by the manufacturer");
    public static final Tag TRACK1_DISCRETIONARY_DATA               = new TagImpl("9f1f", TagValueType.TEXT, "[Magnetic Stripe] Track 1 Discretionary Data", "Discretionary part of track 1 according to ISO/IEC 7813");
    public static final Tag TRACK2_DISCRETIONARY_DATA               = new TagImpl("9f20", TagValueType.TEXT, "[Magnetic Stripe] Track 2 Discretionary Data", "Discretionary part of track 2 according to ISO/IEC 7813");
    public static final Tag TRANSACTION_TIME                        = new TagImpl("9f21", TagValueType.NUMERIC, "Transaction Time (HHMMSS)", "Local time that the transaction was authorised");
    public static final Tag CA_PUBLIC_KEY_INDEX_TERMINAL            = new TagImpl("9f22", TagValueType.BINARY, "Certification Authority Public Key Index - Terminal", "Identifies the certification authority’s public key in conjunction with the RID");
    public static final Tag UPPER_CONSEC_OFFLINE_LIMIT              = new TagImpl("9f23", TagValueType.BINARY, "Upper Consecutive Offline Limit", "Issuer-specified preference for the maximum number of consecutive offline transactions for this ICC application allowed in a terminal without online capability");
    public static final Tag APP_CRYPTOGRAM                          = new TagImpl("9f26", TagValueType.BINARY, "App Cryptogram", "Cryptogram returned by the ICC in response of the GENERATE AC command");
    public static final Tag CRYPTOGRAM_INFORMATION_DATA             = new TagImpl("9f27", TagValueType.BINARY, "Cryptogram Info Data", "Indicates the type of cryptogram and the actions to be performed by the terminal");
    public static final Tag ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_CERT    = new TagImpl("9f2d", TagValueType.BINARY, "ICC PIN Encipherment Public Key Certificate", "ICC PIN Encipherment Public Key certified by the issuer");
    public static final Tag ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_EXP     = new TagImpl("9f2e", TagValueType.BINARY, "ICC PIN Encipherment Public Key Exponent", "ICC PIN Encipherment Public Key Exponent used for PIN encipherment");
    public static final Tag ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_REM     = new TagImpl("9f2f", TagValueType.BINARY, "ICC PIN Encipherment Public Key Remainder", "Remaining digits of the ICC PIN Encipherment Public Key Modulus");
    public static final Tag ISSUER_PUBLIC_KEY_EXP                   = new TagImpl("9f32", TagValueType.BINARY, "Issuer Public Key Exponent", "Issuer public key exponent used for the verification of the Signed Static App Data and the ICC Public Key Certificate");
    public static final Tag TERMINAL_CAPABILITIES                   = new TagImpl("9f33", TagValueType.BINARY, "Terminal Capabilities", "Indicates the card data input, CVM, and security capabilities of the terminal");
    public static final Tag CVM_RESULTS                             = new TagImpl("9f34", TagValueType.BINARY, "Cardholder Verification (CVM) Results", "Indicates the results of the last CVM performed");
    public static final Tag TERMINAL_TYPE                           = new TagImpl("9f35", TagValueType.NUMERIC, "Terminal Type", "Indicates the environment of the terminal, its communications capability, and its operational control");
    public static final Tag APP_TRANSACTION_COUNTER                 = new TagImpl("9f36", TagValueType.BINARY, "App Transaction Counter (ATC)", "Counter maintained by the application in the ICC (incrementing the ATC is managed by the ICC)");
    public static final Tag UNPREDICTABLE_NUMBER                    = new TagImpl("9f37", TagValueType.BINARY, "Unpredictable Number", "Value to provide variability and uniqueness to the generation of a cryptogram");
    public static final Tag PDOL                                    = new TagImpl("9f38", TagValueType.DOL, "Processing Options Data Object List (PDOL)", "Contains a list of terminal resident data objects (tags and lengths) needed by the ICC in processing the GET PROCESSING OPTIONS command");
    public static final Tag POINT_OF_SERVICE_ENTRY_MODE             = new TagImpl("9f39", TagValueType.NUMERIC, "Point-of-Service (POS) Entry Mode", "Indicates the method by which the PAN was entered, according to the first two digits of the ISO 8583:1987 POS Entry Mode");
    public static final Tag AMOUNT_REFERENCE_CURRENCY               = new TagImpl("9f3a", TagValueType.BINARY, "Amount, Reference Currency", "Authorised amount expressed in the reference currency");
    public static final Tag APP_REFERENCE_CURRENCY                  = new TagImpl("9f3b", TagValueType.NUMERIC, "App Reference Currency", "1–4 currency codes used between the terminal and the ICC when the Transaction Currency Code is different from the App Currency Code; each code is 3 digits according to ISO 4217");
    public static final Tag TRANSACTION_REFERENCE_CURRENCY_CODE     = new TagImpl("9f3c", TagValueType.NUMERIC, "Transaction Reference Currency Code", "Code defining the common currency used by the terminal in case the Transaction Currency Code is different from the App Currency Code");
    public static final Tag TRANSACTION_REFERENCE_CURRENCY_EXP      = new TagImpl("9f3d", TagValueType.NUMERIC, "Transaction Reference Currency Exponent", "Indicates the implied position of the decimal point from the right of the transaction amount, with the Transaction Reference Currency Code represented according to ISO 4217");
    public static final Tag ADDITIONAL_TERMINAL_CAPABILITIES        = new TagImpl("9f40", TagValueType.BINARY, "Additional Terminal Capabilities", "Indicates the data input and output capabilities of the terminal");
    public static final Tag TRANSACTION_SEQUENCE_COUNTER            = new TagImpl("9f41", TagValueType.NUMERIC, "Transaction Sequence Counter", "Counter maintained by the terminal that is incremented by one for each transaction");
    public static final Tag APPLICATION_CURRENCY_CODE               = new TagImpl("9f42", TagValueType.NUMERIC, "App Currency Code", "Indicates the currency in which the account is managed according to ISO 4217");
    public static final Tag APP_REFERENCE_CURRECY_EXPONENT          = new TagImpl("9f43", TagValueType.NUMERIC, "App Reference Currency Exponent", "Indicates the implied position of the decimal point from the right of the amount, for each of the 1–4 reference currencies represented according to ISO 4217");
    public static final Tag APP_CURRENCY_EXPONENT                   = new TagImpl("9f44", TagValueType.NUMERIC, "App Currency Exponent", "Indicates the implied position of the decimal point from the right of the amount represented according to ISO 4217");
    public static final Tag DATA_AUTHENTICATION_CODE                = new TagImpl("9f45", TagValueType.BINARY, "Data Authentication Code", "An issuer assigned value that is retained by the terminal during the verification process of the Signed Static App Data");
    public static final Tag ICC_PUBLIC_KEY_CERT                     = new TagImpl("9f46", TagValueType.BINARY, "ICC Public Key Certificate", "ICC Public Key certified by the issuer");
    public static final Tag ICC_PUBLIC_KEY_EXP                      = new TagImpl("9f47", TagValueType.BINARY, "ICC Public Key Exponent", "ICC Public Key Exponent used for the verification of the Signed Dynamic App Data");
    public static final Tag ICC_PUBLIC_KEY_REMAINDER                = new TagImpl("9f48", TagValueType.BINARY, "ICC Public Key Remainder", "Remaining digits of the ICC Public Key Modulus");
    public static final Tag DDOL                                    = new TagImpl("9f49", TagValueType.DOL, "Dynamic Data Authentication Data Object List (DDOL)", "List of data objects (tag and length) to be passed to the ICC in the INTERNAL AUTHENTICATE command");
    public static final Tag SDA_TAG_LIST                            = new TagImpl("9f4a", TagValueType.BINARY, "Static Data Authentication Tag List", "List of tags of primitive data objects defined in this specification whose value fields are to be included in the Signed Static or Dynamic App Data");
    public static final Tag SIGNED_DYNAMIC_APPLICATION_DATA         = new TagImpl("9f4b", TagValueType.BINARY, "Signed Dynamic App Data", "Digital signature on critical application parameters for DDA or CDA");
    public static final Tag ICC_DYNAMIC_NUMBER                      = new TagImpl("9f4c", TagValueType.BINARY, "ICC Dynamic Number", "Time-variant number generated by the ICC, to be captured by the terminal");
    public static final Tag LOG_ENTRY                               = new TagImpl("9f4d", TagValueType.BINARY, "Log Entry", "Provides the SFI of the Transaction Log file and its number of records");
    public static final Tag MERCHANT_NAME_AND_LOCATION              = new TagImpl("9f4e", TagValueType.TEXT, "Merchant Name and Location", "Indicates the name and location of the merchant");
    public static final Tag LOG_FORMAT                              = new TagImpl("9f4f", TagValueType.DOL, "Log Format", "List (in tag and length format) of data objects representing the logged data elements that are passed to the terminal when a transaction log record is read");
    
    public static final Tag FCI_ISSUER_DISCRETIONARY_DATA           = new TagImpl("bf0c", TagValueType.BINARY, "File Control Info (FCI) Issuer Discretionary Data", "Issuer discretionary part of the FCI (e.g. O/S Manufacturer proprietary data)");

    //'9F50' to '9F7F' are reserved for the payment systems (proprietary)
    
    //The following tags are specified in EMV Contactless (Book A)
    
    //The Track 1 Data may be present in the file read using the READ 
    //RECORD command during a mag-stripe mode transaction. It is made up of 
    //the following sub-fields:
    //+------------------------+--------------+--------------------+
    //| Data Field             | Length       |      Format        |
    //+------------------------+--------------+--------------------+
    //| Format Code            |      1       |              '42'  |
    //| Primary Account Number | var up to 19 |             digits |
    //| Field Separator        |      1       |              '5E'  |
    //| Name                   |   2-26       | (see ISO/IEC 7813) |
    //| Field Separator        |      1       |              '5E'  |
    //| Expiry Date            |      4       |              YYMM  |
    //| Service Code           |      3       |            digits  |
    //| Discretionary Data     | var.         |               ans  |
    //+------------------------+--------------+--------------------+
    //BER-TLV[56, 29 (raw 29), 42 xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx 5e 20 2f 5e xx xx xx xx 32 30 31 30 31 30 30 30 30 30 30 30 30 30 30 30]
    //BER-TLV[56, 34 (raw 34), 42 XX XX XX XX XX XX XX XX XX XX XX XX XX XX XX XX 5e 20 2f 5e YY YY MM MM 32 30 31 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30
    public static final Tag TRACK1_DATA                             = new TagImpl("56", TagValueType.BINARY, "Track 1 Data", "Track 1 Data contains the data objects of the track 1 according to [ISO/IEC 7813] Structure B, excluding start sentinel, end sentinel and LRC.");

    public static final Tag TERMINAL_TRANSACTION_QUALIFIERS         = new TagImpl("9f66", TagValueType.BINARY, "Terminal Transaction Qualifiers", "Provided by the reader in the GPO command and used by the card to determine processing choices based on reader functionality");
    //The Track 2 Data is present in the file read using the READ RECORD command 
    //during a mag-stripe mode transaction. It is made up of the following 
    //sub-fields (same as tag 57):
    //
    //+------------------------+-----------------------+-----------+
    //| Data Field             | Length                | Format    |
    //+------------------------+-----------------------+-----------+
    //| Primary Account Number | var. up to 19 nibbles | n         |
    //| Field Separator        |              1 nibble | b ('D')   |
    //| Expiry Date            |                     2 | n (YYMM)  |
    //| Service Code           |             3 nibbles | n         |
    //| Discretionary Data     | var.                  | n         |
    //| Padding if needed      |              1 nibble | b ('F')   |
    //+------------------------+-----------------------+-----------+

    //9f6b 13  BB BB BB BB BB BB BB BB dY YM M2 01 00 00 00 00 00 00 0f
    public static final Tag TRACK2_DATA                             = new TagImpl("9f6b", TagValueType.BINARY, "Track 2 Data", "Track 2 Data contains the data objects of the track 2 according to [ISO/IEC 7813] Structure B, excluding start sentinel, end sentinel and LRC.");
    public static final Tag VLP_ISSUER_AUTHORISATION_CODE           = new TagImpl("9f6e", TagValueType.BINARY, "Visa Low-Value Payment (VLP) Issuer Authorisation Code", "");
    
    //These are specified in EMV Contactless (Book B)
    public static final Tag EXTENDED_SELECTION                      = new TagImpl("9f29", TagValueType.BINARY, "Indicates the card's preference for the kernel on which the contactless application can be processed", "");
    public static final Tag KERNEL_IDENTIFIER                       = new TagImpl("9f2a", TagValueType.BINARY, "The value to be appended to the ADF Name in the data field of the SELECT command, if the Extended Selection Support flag is present and set to 1", "");    

    /**
     * If the tag is not found, this method returns the "[UNHANDLED TAG]" containing 'tagBytes'
     *
     * @param tagBytes
     * @return
     */
    public static Tag getNotNull(byte[] tagBytes) {
        Tag tag = find(tagBytes);
        if (tag == null) {
            tag = createUnknownTag(tagBytes);
        }
        return tag;
    }

    public static Tag getNotNull(EMVApp app, Tag tag) {
        Tag tagFound = get(app, tag);
        if (tagFound == null) {
            tagFound = createUnknownTag(tag.getTagBytes());
        }
        return tagFound;
    }

    public static Tag createUnknownTag(byte[] tagBytes) {
        return new TagImpl(tagBytes, TagValueType.BINARY, "[UNKNOWN TAG]", "");
    }

    /**
     * Returns null if Tag not found
     */
    public static Tag find(byte[] tagBytes) {
        return tags.get(ByteArrayWrapper.wrapperAround(tagBytes));
    }

    private static void addTag(Tag tag) {
        // use 'wrapper around', since the underlying byte-array will not be changed in this case
        ByteArrayWrapper baw = ByteArrayWrapper.wrapperAround(tag.getTagBytes());
        if (tags.containsKey(baw)) {
            throw new IllegalArgumentException("Tag already added " + tag);
        }
        tags.put(baw, tag);
    }

    static {
        Field[] fields; 
        
        fields = EMVTags.class.getFields();
        for (Field f : fields) {
            if (f.getType() == Tag.class) {
                try {
                    Tag t = (Tag) f.get(null);
                    addTag(t);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        fields = VISATags.class.getFields();
        for (Field f : fields) {
            if (f.getType() == Tag.class) {
                try {
                    Tag t = (Tag) f.get(null);
                    addPaymentSystemTag(new byte[]{(byte)0xA0, 0x00, 0x00, 0x00, 0x03}, t);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        fields = MCTags.class.getFields();
        for (Field f : fields) {
            if (f.getType() == Tag.class) {
                try {
                    Tag t = (Tag) f.get(null);
                    addPaymentSystemTag(new byte[]{(byte)0xA0, 0x00, 0x00, 0x00, 0x04}, t);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        //Chipnip
        addPaymentSystemTag(Util.fromHexString("A000000315"), new TagImpl("c1",   TagValueType.BINARY, "?", "Example: BER-TLV[c1, 02 (raw 02), 1101]"));
    }
        
    @SuppressWarnings("unused")
    private static void addIssuerTag(IssuerIdNumber iin, Tag tag) {
        //Use 'wrapper around', since the underlying byte-array will not be changed in this case
        ByteArrayWrapper tagBytesWrapped = ByteArrayWrapper.wrapperAround(tag.getTagBytes());
        LinkedHashMap<ByteArrayWrapper, Tag> issuerTags = issuerToTagsMap.get(iin);
        if (issuerTags == null) {
            issuerTags = new LinkedHashMap<ByteArrayWrapper, Tag>();
            issuerToTagsMap.put(iin, issuerTags);
        }
        if (issuerTags.containsKey(tagBytesWrapped)) {
            throw new IllegalArgumentException("Tag already added " + tag);
        }
        issuerTags.put(tagBytesWrapped, tag);
    }
    
    private static void addPaymentSystemTag(byte[] ridBytes, Tag tag) {
        //Use 'wrapper around', since the underlying byte-array will not be changed in this case
        ByteArrayWrapper tagBytesWrapped = ByteArrayWrapper.wrapperAround(tag.getTagBytes());
        ByteArrayWrapper ridBytesWrapped = ByteArrayWrapper.wrapperAround(ridBytes);
        LinkedHashMap<ByteArrayWrapper, Tag> paymentSystemTags = paymentSystemToTagsMap.get(ridBytesWrapped);
        if (paymentSystemTags == null) {
            paymentSystemTags = new LinkedHashMap<ByteArrayWrapper, Tag>();
            paymentSystemToTagsMap.put(ridBytesWrapped, paymentSystemTags);
        }
        if (paymentSystemTags.containsKey(tagBytesWrapped)) {
            throw new IllegalArgumentException("Tag already added " + tag);
        }
        paymentSystemTags.put(tagBytesWrapped, tag);
    }

    public static Tag get(EMVApp app, Tag tag){
        ByteArrayWrapper tagBytesWrapped = ByteArrayWrapper.wrapperAround(tag.getTagBytes());
        IssuerIdNumber iin = app.getIssuerIdNumber();
        if(iin != null){
            if(issuerToTagsMap.containsKey(iin) && issuerToTagsMap.get(iin).containsKey(tagBytesWrapped)){
                return issuerToTagsMap.get(iin).get(tagBytesWrapped);
            }
        }
        if (app.getAid() != null) {
            byte[] ridBytes = Arrays.copyOf(app.getAidBytes(), 5);
            ByteArrayWrapper ridBytesWrapped = ByteArrayWrapper.wrapperAround(ridBytes);
            if (paymentSystemToTagsMap.containsKey(ridBytesWrapped) &&
                paymentSystemToTagsMap.get(ridBytesWrapped).containsKey(tagBytesWrapped)) {
                return paymentSystemToTagsMap.get(ridBytesWrapped).get(tagBytesWrapped);
            }
        }
        return find(tag.getTagBytes());
    }

    public static Iterator<Tag> iterator() {
        return tags.values().iterator();
    }

    private EMVTags() {
        throw new UnsupportedOperationException("Not allowed to instantiate");
    }
}
