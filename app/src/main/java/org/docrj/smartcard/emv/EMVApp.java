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

package org.docrj.smartcard.emv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.docrj.smartcard.reader.SmartcardApp;
import org.docrj.smartcard.emv.AppPriorityIndicator;
import org.docrj.smartcard.emv.DOL;
import org.docrj.smartcard.emv.LogEntry;
import org.docrj.smartcard.emv.LanguagePref;

import org.docrj.smartcard.iso7816.BERTLV;
import org.docrj.smartcard.util.Util;

public class EMVApp extends SmartcardApp {

    private AppPriorityIndicator mApi;
    private AppInterchangeProfile mAip;
    private DOL mPdol;
    private LanguagePref mLangPref;
    private String mPrefName;
    private int mIssuerCodeTblIdx;
    private AppFileLocator mAfl = new AppFileLocator(new byte[]{});
    private String mIssuerUrl;
    private int mIssuerCC = -1;
    private String mIssuerCCAlpha3;
    private IssuerIdNumber mIssuerIdNumber;
    private LogEntry mLogEntry;
    private int mAppCurrencyCode = -1;
    private List<BERTLV> mUnkRecords = new ArrayList<BERTLV>();
    private List<BERTLV> mUnprocRecords = new ArrayList<BERTLV>();

    public EMVApp() {
        super();
    }

    public EMVApp(String name, String aid, AppPriorityIndicator api) {
        super(name, aid, SmartcardApp.TYPE_PAYMENT);
        mApi = api;
    }

    public EMVApp clone() {
        return new EMVApp(super.getName(), super.getAid(), mApi);
    }

    public void copy(EMVApp app) {
        if (app != null) {
            super.copy(app);
            mApi = app.getApi();
        }
    }

    public void setApi(AppPriorityIndicator api) {
        mApi = api;
    }

    public AppPriorityIndicator getApi() {
        return mApi;
    }
    
    public void setAppFileLocator(AppFileLocator afl) {
        mAfl = afl;
    }

    public AppFileLocator getAppFileLocator() {
        return mAfl;
    }

    public void setAppInterchangeProfile(AppInterchangeProfile aip) {
        mAip = aip;
    }

    public AppInterchangeProfile getAppInterchangeProfile() {
        return mAip;
    }

    public void setIssuerCC(int issuerCC) {
        this.mIssuerCC = issuerCC;
    }

    public int getIssuerCC() {
        return mIssuerCC;
    }

    public String getIssuerCCAlpha3() {
        return mIssuerCCAlpha3;
    }

    public void setIssuerCCAlpha3(String issuerCCAlpha3) {
        mIssuerCCAlpha3 = issuerCCAlpha3;
    }

    public IssuerIdNumber getIssuerIdNumber() {
        return mIssuerIdNumber;
    }

    public void setIssuerIdNumber(IssuerIdNumber issuerIdNumber) {
        mIssuerIdNumber = issuerIdNumber;
    }
    
    public void setLogEntry(LogEntry logEntry) {
        mLogEntry = logEntry;
    }

    public LogEntry getLogEntry() {
        return mLogEntry;
    }

    public DOL getPdol() {
        return mPdol;
    }

    public void setPdol(DOL pdol) {
        mPdol = pdol;
    }
    
    public void setLanguagePref(LanguagePref languagePreference) {
        if (mLangPref != null) {
            throw new RuntimeException("multiple language pref not supported!");
        }
        mLangPref = languagePreference;
    }

    public LanguagePref getLanguagePref() {
        return mLangPref;
    }
    
    public void setPreferredName(String preferredName) {
        mPrefName = preferredName;
    }

    public String getPreferredName() {
        return mPrefName;
    }

    public void setIssuerUrl(String issuerUrl) {
        mIssuerUrl = issuerUrl;
    }

    public String getIssuerUrl() {
        return mIssuerUrl;
    }    

    public void setIssuerCodeTableIndex(int index) {
        mIssuerCodeTblIdx = index;
    }

    public int getIssuerCodeTableIndex() {
        return mIssuerCodeTblIdx;
    }

    public void setAppCurrencyCode(int appCurrencyCode) {
        mAppCurrencyCode = appCurrencyCode;
    }

    public int getAppCurrencyCode() {
        return mAppCurrencyCode;
    }

    public void addUnknownRecord(BERTLV bertlv) {
        if (getAid() != null 
                && Arrays.equals(getAidBytes(), Util.fromHexString("a0 00 00 00 03 00 00 00"))
                && Arrays.equals(bertlv.getTag().getTagBytes(), Util.fromHexString("9f 65"))) {
            //TODO: this is a hack for GP App with tag 9f65 which is very common, but not handled yet
        }
        mUnkRecords.add(bertlv);
    }

    public List<BERTLV> getUnknownRecords() {
        return Collections.unmodifiableList(mUnkRecords);
    }

    public void addUnprocessedRecord(BERTLV bertlv) {
        if (getAid() != null 
                && Arrays.equals(getAidBytes(), Util.fromHexString("a0 00 00 00 03 00 00 00"))
                && Arrays.equals(bertlv.getTag().getTagBytes(), Util.fromHexString("9f 65"))) {
            //TODO: this is a hack for GP App with tag 9f65 which is very common, but not handled yet
        }
        mUnprocRecords.add(bertlv);
    }

    public List<BERTLV> getUnprocessedRecords() {
        return Collections.unmodifiableList(mUnprocRecords);
    }
}
