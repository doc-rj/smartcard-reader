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
 * Application Priority Indicator
 * Indicates the priority of a given application or group of applications in a directory
 */
public class AppPriorityIndicator {
    private byte mApiByte;

    public AppPriorityIndicator(byte apiByte) {
        mApiByte = apiByte;
    }

    public boolean mayBeselectedWithoutCardholderConfirmation() {
        return (mApiByte & 0xFF & 0x80) == 0;
    }

    public int getSelectionPriority() {
        return (mApiByte & 0x0F);
    }

    public boolean isPriorityAssigned() {
        return (mApiByte & 0x0F) > 0;
    }
}
