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

import org.docrj.smartcard.iso7816.SmartcardException;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Application File Locator (AFL)
 * Indicates the location (SFI, range of records) of the AEFs related to a given application
 */
public class AppFileLocator {
    private LinkedList<AppElementaryFile> aefList = new LinkedList<AppElementaryFile>();

    public List<AppElementaryFile> getApplicationElementaryFiles() {
        return Collections.unmodifiableList(aefList);
    }

    public AppFileLocator(byte[] data) {
        if (data.length % 4 != 0) {
            throw new SmartcardException("Length is not a multiple of 4. Length=" + data.length);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        while (bis.available() > 0) {
            byte[] tmp = new byte[4];
            bis.read(tmp, 0, tmp.length);
            aefList.add(new AppElementaryFile(tmp));
        }
    }
}
