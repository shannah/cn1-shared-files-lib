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
package com.codenameone.ext.io.sharedfiles;

import com.codename1.system.NativeLookup;
import com.codename1.util.AsyncResource;

public abstract class SharedFileManager {

    private static SharedFileManager instance;

    public static void setInstance(SharedFileManager mgr) {
        instance = mgr;
    }

    public static SharedFileManager getInstance() {
        if (instance == null) {
            SharedFileManagerNative sfmNative = NativeLookup.create(SharedFileManagerNative.class);
            if (sfmNative.isSupported()) {
                sfmNative.initSharedFiles();
            } else {
                throw new NotSupportedException();
            }
        }
        return instance;
    }

    public AsyncResource<SharedFile> openFile() {
        return openFile("*/*");
    }

    public abstract AsyncResource<SharedFile> openFile(String mimeType);
    public abstract AsyncResource<SharedFile> openDirectory();

    public abstract SharedFile openBookmark(String bookmark);

    public abstract SharedFile[] getBookmarks();
}
