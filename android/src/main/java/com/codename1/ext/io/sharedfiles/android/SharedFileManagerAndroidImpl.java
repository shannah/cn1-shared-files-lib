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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.IntentResultListener;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codenameone.ext.io.sharedfiles.SharedFile;
import com.codenameone.ext.io.sharedfiles.SharedFileManager;

import java.net.URISyntaxException;
import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class SharedFileManagerAndroidImpl extends SharedFileManager {


    public AsyncResource<SharedFile> openFile(String mimeType) { // TODO: FIX Should accept one or mimetypes
        final AsyncResource<SharedFile> out = new AsyncResource<SharedFile>();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        AndroidNativeUtil.startActivityForResult(intent, new IntentResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        final Uri uri = data.getData();
                        AndroidNativeUtil.getActivity().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        CN.callSerially(new Runnable() {
                            public void run() {
                                out.complete(new SharedFileAndroidImpl(new SharedDocumentFile(AndroidNativeUtil.getContext(), uri, "/tmp")));
                            }
                        });

                        return;
                    }
                }
                out.complete(null);
            }
        });

        return out;
    }
    public AsyncResource<SharedFile> openDirectory() {
        final AsyncResource<SharedFile> out = new AsyncResource<SharedFile>();

        AndroidNativeUtil.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), new IntentResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        final Uri uri = data.getData();
                        AndroidNativeUtil.getActivity().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        CN.callSerially(new Runnable() {
                            public void run() {
                                out.complete(new SharedFileAndroidImpl(new SharedDocumentFile(AndroidNativeUtil.getContext(), uri, "/tmp")));
                            }
                        });

                        return;
                    }
                }
                out.complete(null);
            }
        });

        return out;
    }

    public SharedFile openBookmark(String bookmark) {
        SharedDocumentFileManager mgr = new SharedDocumentFileManager(AndroidNativeUtil.getContext());
        mgr.loadRoots();
        SharedDocumentFile file = mgr.find(bookmark);
        if (file == null) {
            return null;
        }

        return new SharedFileAndroidImpl(file);
    }

    public SharedFile[] getBookmarks() {
        SharedDocumentFileManager mgr = new SharedDocumentFileManager(AndroidNativeUtil.getContext());
        mgr.loadRoots();
        List<SharedDocumentFile> roots = mgr.getRoots();
        int len = roots.size();
        SharedFile[] out = new SharedFile[len];
        for (int i = 0; i < len; i++) {
            out[i] = new SharedFileAndroidImpl(roots.get(i));
        }

        return out;
    }
}