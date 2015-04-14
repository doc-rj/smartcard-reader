/*
 * Copyright 2015 Ryan Jones
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

public class Util {
    public static ArrayList<SmartcardApp> findGroupMembers(String groupName, List<SmartcardApp> apps) {
        ArrayList<SmartcardApp> memberApps = new ArrayList<>();
        for (SmartcardApp app : apps) {
            HashSet<String> groups = app.getGroups();
            for (String group : groups) {
                if (group.equals(groupName)) {
                    memberApps.add(app);
                    break;
                }
            }
        }
        return memberApps;
    }

    public static int findNumGroupMembers(String groupName, List<SmartcardApp> apps) {
        int numMembers = 0;
        for (SmartcardApp app : apps) {
            HashSet<String> groups = app.getGroups();
            for (String group : groups) {
                if (group.equals(groupName)) {
                    numMembers++;
                    break;
                }
            }
        }
        return numMembers;
    }

    public static boolean isGroupEmpty(String groupName, List<SmartcardApp> apps) {
        for (SmartcardApp app : apps) {
            HashSet<String> groups = app.getGroups();
            for (String group : groups) {
                if (group.equals(groupName)) {
                    return false;
                }
            }
        }
        return true;
    }

    // returns a mapping of group member apps to their position in the mApps list
    public static HashMap<SmartcardApp, Integer> mapGroupMembers(String groupName, List<SmartcardApp> apps) {
        HashMap<SmartcardApp, Integer> memberApps = new LinkedHashMap<>();
        int i = 0;
        for (SmartcardApp app : apps) {
            HashSet<String> groups = app.getGroups();
            for (String group : groups) {
                if (group.equals(groupName)) {
                    memberApps.put(app, i);
                    break;
                }
            }
            i++;
        }
        return memberApps;
    }
}
