/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ext.io.sharedfiles.android;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.codenameone.ext.io.sharedfiles.SharedFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class SharedFileAndroidImpl implements SharedFile {

    private SharedDocumentFile internal;

    public SharedFileAndroidImpl(SharedDocumentFile internal) {
        this.internal = internal;
    }

    public InputStream openInputStream() throws IOException {
        return internal.openInputStream();
    }
    public OutputStream openOutputStream(String mimetype) throws IOException {
        return internal.openOutputStream(mimetype);
    }

    public SharedFile[] listFiles() {
        SharedDocumentFile[] children = internal.listFiles();
        int len = children.length;
        SharedFile[] out = new SharedFile[len];

        for (int i=0; i<len; i++) {
            out[i] = new SharedFileAndroidImpl(children[i]);
        }

        return out;
    }

    public boolean copyTo(SharedFile destination) throws IOException {
        SharedFileAndroidImpl destinationImpl = (SharedFileAndroidImpl) destination;
        return internal.copyTo(destinationImpl.internal);
    }
    public SharedFile createBookmark() {
        return new SharedFileAndroidImpl(internal.createBookmark());
    }

    public boolean delete() {
        return internal.delete();
    }

    public SharedFile deleteBookmark() {
        return new SharedFileAndroidImpl(internal.deleteBookmark());
    }

    public boolean exists() {
        return internal.exists();
    }

    public String getMimeType() {
        return internal.getMimeType();
    }

    public String getName() {
        return internal.getName();
    }

    public SharedFile getOrCreateBookmark() {
        return new SharedFileAndroidImpl(internal.getOrCreateBookmark());
    }

    public SharedFile getParent() {
        return new SharedFileAndroidImpl(internal.getParent());
    }

    public String getPath() {
        return internal.getPath();
    }

    public String getPathPrefix() {
        return internal.getPathPrefix();
    }

    public long getSize() {
        return internal.getSize();
    }

    public boolean isBookmarked() {
        return internal.isBookmarked();
    }

    public boolean isDir() {
        return internal.isDir();
    }

    public boolean isFile() {
        return internal.isFile();
    }

    public void renameTo(SharedFile destination) throws IOException {
        SharedFileAndroidImpl destinationImpl = (SharedFileAndroidImpl)destination;
        internal.renameTo(destinationImpl.internal);
    }

    public SharedFile getChild(String fileName) {
        if (fileName.contains("/")) {
            String name1 = fileName.substring(0, fileName.indexOf("/"));
            String remainder = fileName.substring(name1.length()+1);
            return getChild(name1).getChild(remainder);
        }

        return new SharedFileAndroidImpl(new SharedDocumentFile(internal, fileName));
    }

}