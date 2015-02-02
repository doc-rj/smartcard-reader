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

import java.util.ArrayList;

/**
 * Directory Definition File
 */
public class DDF {

    private byte[] name;
    private int sfi;
    private LanguagePref languagePreference = null;
    private int issuerCodeTableIndex = -1;
    private ArrayList<EMVApp> mEMVApps = new ArrayList<EMVApp>();

    public DDF() {
    }

    public void setSFI(int sfi) {
        this.sfi = sfi;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    public void setLanguagePreference(LanguagePref languagePreference) {
        this.languagePreference = languagePreference;
    }    

    public void setIssuerCodeTableIndex(int index){
        issuerCodeTableIndex = index;
    }    

    public void addEMVApp(EMVApp app) {
        mEMVApps.add(app);
    }

    public byte[] getName() {
        return name;
    }

    public int getSFI() {
        return sfi;
    }

    public LanguagePref getLanguagePreference() {
        return languagePreference;
    }

    public int getIssuerCodeTableIndex() {
        return issuerCodeTableIndex;
    }

    public String getIssuerCodeTable() {
        return "ISO-8859-"+issuerCodeTableIndex;
    }

    public ArrayList<EMVApp> getEMVApps() {
        return mEMVApps;
    }
    
    public int getNumEMVApps() {
        return mEMVApps.size();
    }
}
