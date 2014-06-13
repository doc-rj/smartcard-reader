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

package org.docrj.smartcard.iso7816;

import java.util.Arrays;

import org.docrj.smartcard.util.Util;

public class TagImpl implements Tag {

    byte[] idBytes;
    String name;
    String description;
    TagValueType tagValueType;
    Class tagClass;

    TagType type;

    public TagImpl(String id, TagValueType tagValueType, String name, String description) {
        build(Util.fromHexString(id), tagValueType, name, description);
    }

    public TagImpl(byte[] idBytes, TagValueType tagValueType, String name, String description) {
        build(idBytes, tagValueType, name, description);
    }

    private void build(byte[] idBytes, TagValueType tagValueType, String name, String description) {
        if (idBytes == null) {
            throw new IllegalArgumentException("Param id cannot be null");
        }
        if (idBytes.length == 0) {
            throw new IllegalArgumentException("Param id cannot be empty");
        }
        if (tagValueType == null){
            throw new IllegalArgumentException("Param tagValueType cannot be null");
        }
        this.idBytes = idBytes;
        this.name = name != null ? name : "";
        this.description = description!=null?description:"";
        this.tagValueType = tagValueType;

        if(Util.isBitSet(this.idBytes[0], 6)){
            this.type = TagType.CONSTRUCTED;
        } else {
            this.type = TagType.PRIMITIVE;
        }
        //Bits 8 and 7 of the first byte of the tag field indicate a class.
        //The value 00 indicates a data object of the universal class.
        //The value 01 indicates a data object of the application class.
        //The value 10 indicates a data object of the context-specific class.
        //The value 11 indicates a data object of the private class.
        byte classValue = (byte)(this.idBytes[0] >>> 6 & 0x03);
        switch(classValue) {
            case (byte)0x00:
                tagClass = Class.UNIVERSAL;
                break;
            case (byte)0x01:
                tagClass = Class.APPLICATION;
                break;
            case (byte)0x02:
                tagClass = Class.CONTEXT_SPECIFIC;
                break;
            case (byte)0x03:
                tagClass = Class.PRIVATE;
                break;
            default:
                throw new RuntimeException("UNEXPECTED TAG CLASS: "+Util.byte2BinaryLiteral(classValue) + " " + Util.byteArrayToHexString(this.idBytes) + " " + name);
        }

    }

    @Override
    public boolean isConstructed() {
        return type == TagType.CONSTRUCTED;
    }

    @Override
    public byte[] getTagBytes() {
        return idBytes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override 
    public String getDescription(){
        return description;
    }

    @Override
    public TagValueType getTagValueType() {
        return tagValueType;
    }

    @Override
    public TagType getType() {
        return type;
    }

    @Override
    public Class getTagClass(){
        return tagClass;
    }

    @Override
    public boolean equals(Object other){
        if(!(other instanceof Tag)) return false;
        Tag that = (Tag)other;
        if(this.getTagBytes().length != that.getTagBytes().length) return false;

        return Arrays.equals(this.getTagBytes(), that.getTagBytes());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Arrays.hashCode(this.idBytes);
        return hash;
    }

    @Override
    public int getNumTagBytes() {
        return idBytes.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tag[");
        sb.append(Util.byteArrayToHexString(getTagBytes()));
        sb.append("] Name=");
        sb.append(getName());
        sb.append(", TagType=");
        sb.append(getType());
        sb.append(", ValueType=");
        sb.append(getTagValueType());
        sb.append(", Class=");
        sb.append(tagClass);
        return sb.toString();
    }
}
