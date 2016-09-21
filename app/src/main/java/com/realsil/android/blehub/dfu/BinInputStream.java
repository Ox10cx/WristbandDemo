/*
 * Copyright (C) 2015 Realsil Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.realsil.android.blehub.dfu;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinInputStream extends BufferedInputStream {
    private static final String TAG = "BinInputStream";

    /** BIN file header components */
    private short offset;
    private short signature;
    private short version;
    private short checksum;
    private short length;
    private byte ota_flag;
    private byte reserved_8;
    private static final int headerSize = 12;

    private final int packetBytes;
    private final byte[] localBuf;
    private int bytesRead;

    /**
     * Creates the BIN Input Stream.
     * The constructor parses the header of BIN file. Get the available size of the BIN content
     * through {@link #remainSizeInBytes()}. If BIN file is invalid then the bin size is 0.
     *
     * @param in the input stream to read from
     * @throws IOException if the stream is closed or another IOException occurs.
     */
    public BinInputStream(final InputStream in, int pktBytes) throws IOException {
        super(new BufferedInputStream(in));
        packetBytes = pktBytes;
        localBuf = new byte[packetBytes];
        bytesRead = 0;

        parseBinFileHeader();
    }

    /**
     * Creates the BIN Input Stream.
     * The constructor parses the header of BIN file. Get the available size of the BIN content
     * through {@link #remainSizeInBytes()}. If BIN file is invalid then the bin size is 0.
     *
     * @param in the input stream to read from
     * @throws IOException if the stream is closed or another IOException occurs.
     */
    public BinInputStream(final InputStream in) throws IOException {
        this(in, 20);
    }

    private void parseBinFileHeader() throws IOException {
        read(localBuf, 0, headerSize);
        ByteBuffer headerBuf = ByteBuffer.wrap(localBuf, 0, headerSize);
        headerBuf.order(ByteOrder.LITTLE_ENDIAN);

        offset = headerBuf.getShort();
        signature = headerBuf.getShort();
        version = headerBuf.getShort();
        checksum = headerBuf.getShort();
        length = headerBuf.getShort();
        ota_flag = localBuf[10];
        reserved_8 = localBuf[11];

        Log.d(TAG, "ParseBinFileHeader: offset " + String.format("0x%04x,", offset) +
                "signature " + String.format("0x%04x,", signature) +
                "version " + String.format("0x%04x,", version) +
                "checksum " + String.format("0x%04x,", checksum) +
                "length " + String.format("0x%04x,", length) +
                "ota_flag " + String.format("0x%02x,", ota_flag) +
                "reserved_8 " + String.format("0x%02x", reserved_8));
    }

    //@Override
    //public int available() throws IOException {
    //    return available - bytesRead;
    //}

    /**
     * Fills the buffer with next bytes from the stream.
     *
     * @return the size of the buffer
     * @throws IOException
     */
    public int readPacket(byte[] buffer) throws IOException {
        int i = 0;

        i = read(buffer, i, packetBytes);
        if (i > 0)
            bytesRead += i;

        return i;
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException("Use readPacket() method instead");
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return readPacket(buffer);
    }

    /**
     * Returns the offset of BIN file.
     *
     * @return
     */
    public short binFileOffset() {
        return offset;
    }

    /**
     * Returns the signature of BIN file.
     *
     * @return
     */
    public short binFileSignature() {
        return signature;
    }

    /**
     * Returns the version of BIN file.
     *
     * @return
     */
    public short binFileVersion() {
        return version;
    }

    /**
     * Returns the checksum of BIN file.
     *
     * @return
     */
    public short binFileChecksum() {
        return checksum;
    }

    /**
     * Returns the length of BIN file.Exclude the header size.
     *
     * @return
     */
    public short binFileLength() {
        return length;
    }

    /**
     * Returns the ota_flag of BIN file.
     *
     * @return
     */
    public byte binFileOtaFlag() {
        return ota_flag;
    }

    /**
     * Returns the reserved_8 of BIN file.
     *
     * @return byte reserved_8
     */
    public byte binFileReserved8() {
        return reserved_8;
    }

    /**
     * Returns the total number of bytes. Call this method before reading packets.
     * This size does not contain header size.
     *
     * @return total number of bytes available
     * @throws IOException
     */
    public int remainSizeInBytes() throws IOException {
    	// be careful, short must to be int
        return (toUnsigned(length)*4 - bytesRead);
    }
    // short change to unsigned
    public static int toUnsigned(short s) {  
        return s & 0x0FFFF;  
    } 
    /**
     * Returns the total number of packets with given size that are needed to get all available data
     *
     * @param packetSize the maximum packet size
     * @return the number of packets needed to get all the content
     * @throws IOException
     */
    public int remainNumInPackets(final int packetSize) throws IOException {
        final int sizeInBytes = remainSizeInBytes();

        return sizeInBytes / packetSize + ((sizeInBytes % packetSize) > 0 ? 1 : 0);
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        bytesRead = 0;
    }
}