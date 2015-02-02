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

/**
 * (see for example: VISA_VIS_ICC_Card_1.4.pdf for VISA specific tags)
 */
public class VISATags {//implements TagProvider {

    public static final Tag APPLICATION_DEFAULT_ACTION                = new TagImpl("9f52", TagValueType.BINARY, "Application Default Action (ADA)", "Visa proprietary data element indicating the action a card should take when exception conditions occur");
    public static final Tag CONSECUTIVE_TRANSACTION_LIMIT_INT         = new TagImpl("9f53", TagValueType.BINARY, "Consecutive Transaction Limit (International)", "");
    public static final Tag CUMULATIVE_TOTAL_TRANSACTION_AMOUNT_LIMIT = new TagImpl("9f54", TagValueType.BINARY, "Cumulative Total Transaction Amount Limit", "");
    //BER-TLV[9f55, 01 (raw 01), 00]
    public static final Tag GEOGRAPHIC_INDICATOR                      = new TagImpl("9f55", TagValueType.BINARY, "Geographic Indicator", "");
    //BER-TLV[9f56, 12 (raw 12), 00007fffffe0000000000000000000000000]
    public static final Tag ISSUER_AUTHENTICATION_INDICATOR           = new TagImpl("9f56", TagValueType.BINARY, "Issuer Authentication Indicator", "");
    public static final Tag LOWER_CONSECUTIVE_OFFLINE_LIMIT           = new TagImpl("9f58", TagValueType.BINARY, "Lower Consecutive Offline Limit", "");
    public static final Tag UPPER_CONSECUTIVE_OFFLINE_LIMIT           = new TagImpl("9f59", TagValueType.BINARY, "Upper Consecutive Offline Limit", "");
    
    // Cumulative Total Transaction Amount Upper Limit
    public static final Tag CUMULATIVE_TOTAL_TRANSACTION_UPPER_LIMIT  = new TagImpl("9f5c", TagValueType.BINARY, "Cumulative Total Transaction Amount Upper Limit", "");
    
    public static final Tag CONSECUTIVE_TRANSACTION_LIMIT             = new TagImpl("9f72", TagValueType.BINARY, "Consecutive Transaction Limit (International--Country)", "");
    public static final Tag CUMULATIVE_TRANSACTION_AMOUNT_LIMIT_DUAL  = new TagImpl("9f75", TagValueType.BINARY, "Cumulative Transaction Amount Limit--Dual Currency", ""); 
    public static final Tag VLP_FUNDS_LIMIT                           = new TagImpl("9f77", TagValueType.BINARY, "VLP Funds Limit", "");
    public static final Tag SINGLE_TRANSACTION_LIMIT                  = new TagImpl("9f78", TagValueType.BINARY, "VLP Single Transaction Limit", "");
    // 
    public static final Tag VLP_AVAILABLE_FUNDS                       = new TagImpl("9f79", TagValueType.BINARY, "VLP Available Funds", "VLP Available Funds (Decremented during Card Action Analysis for offline approved VLP transactions)");
    // TODO GP?
    public static final Tag CPLC_HISTORY_FILE_IDENTIFIERS             = new TagImpl("9f7f", TagValueType.BINARY, "Card Production Life Cycle (CPLC) History File Identifiers", "");
    
    // Log Format TagAndLength found in GET DATA LOG FORMAT on VISA Electron card: Ex 9f8004 with Log value 03 60 60 00
    public static final Tag VISA_LOG_FORMAT                           = new TagImpl("9f80", TagValueType.BINARY, "Log Format", "");
    
    public static final Tag VISA_LOG_ENTRY                            = new TagImpl("df60", TagValueType.BINARY, "VISA Log Entry ??", "");
    
    //TODO refactor to Kernel 3 Tags
    //These are specified in EMV Contactless (Book C-3) "VISA"
    //9f5a 05 3109780380
    public static final Tag APPLICATION_PROGRAM_IDENTIFIER          = new TagImpl("9f5a", TagValueType.BINARY, "Application Program Identifier (Program ID)", "");
    public static final Tag ISSUER_SCRIPT_RESULTS                   = new TagImpl("9f5b", TagValueType.BINARY, "Issuer Script Results", "");
    public static final Tag AVAILABLE_OFFLINE_SPENDING_AMOUNT       = new TagImpl("9f5d", TagValueType.BINARY, "Available Offline Spending Amount (AOSA)", "");
    public static final Tag CARD_AUTHENTICATION_RELATE_DATA         = new TagImpl("9f69", TagValueType.BINARY, "Card Authentication Related Data", "");
    //9f6c 02 3000
    public static final Tag CARD_TRANSACTION_QUALIFIERS             = new TagImpl("9f6c", TagValueType.BINARY, "Card Transaction Qualifiers (CTQ)", "");
    public static final Tag FORM_FACTOR_INDICATOR                   = new TagImpl("9f6e", TagValueType.BINARY, "Form Factor Indicator (FFI)", "");
    public static final Tag CUSTOMER_EXCLUSIVE_DATA                 = new TagImpl("9f7c", TagValueType.BINARY, "Customer Exclusive Data (CED)", "");

}
