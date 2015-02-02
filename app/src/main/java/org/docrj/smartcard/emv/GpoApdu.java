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

import org.docrj.smartcard.iso7816.CommandApdu;
import org.docrj.smartcard.util.Util;

public class GpoApdu extends CommandApdu {

    public static final byte EMV_CLA = (byte)0x80;
    public static final byte EMV_GPO = (byte)0xA8;

    public GpoApdu() {
        super(EMV_CLA, EMV_GPO, 0x00, 0x00, Util.hexToBytes("8300"), 0x00); // GW MC
    }

    public GpoApdu(byte[] data) {
        super(EMV_CLA, EMV_GPO, 0x00, 0x00, data, 0x00);
    }

    public static GpoApdu getGpoApdu(DOL pdol, EMVApp app) {
        if (pdol != null && pdol.getTagAndLengthList().size() > 0) {
            byte[] pdolResponseData = EMVTerminal.constructDOLResponse(pdol, app);
            String data = Util.int2Hex(pdolResponseData.length + 2) + "83" +
                Util.int2Hex(pdolResponseData.length) + Util.bytesToHex(pdolResponseData);
            return new GpoApdu(Util.hexToBytes(data));
        } else {
            return new GpoApdu();
        }
    }

    @Override
    public String getCommandName() {
        return "get processing options";
    }
}
