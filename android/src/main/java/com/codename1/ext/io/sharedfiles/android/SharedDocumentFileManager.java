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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class SharedDocumentFileManager {

    private static final String ROOTS_PREFERENCES_KEY = "com.example.testsharedstorage.ROOTS";
    private SharedPreferences sharedPreferences;

    private List<SharedDocumentFile> roots = new ArrayList<SharedDocumentFile>();

    private Context context;

    public SharedDocumentFileManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(ROOTS_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.context = context;
        loadRoots();
    }

    public SharedDocumentFile addRoot(SharedDocumentFile sharedDocumentFile, String atPath) {
        sharedDocumentFile = sharedDocumentFile.withPathPrefix(atPath);
        roots.add(sharedDocumentFile);

        return sharedDocumentFile;
    }

    public List<SharedDocumentFile> getRoots() {
        return Collections.unmodifiableList(roots);
    }

    public void removeRoot(SharedDocumentFile sharedDocumentFile) {
        if (!sharedDocumentFile.getPathPrefix().startsWith("/")) {
            throw new IllegalArgumentException("Cannot remove root from document file with no path prefix");
        }
        ListIterator<SharedDocumentFile> iterator = roots.listIterator();
        while (iterator.hasNext()) {
            SharedDocumentFile file = iterator.next();
            if (file.getPathPrefix().equals(sharedDocumentFile.getPathPrefix())) {
                iterator.remove();
            }
        }
    }

    public SharedDocumentFile find(String absolutePath) {

        if (!absolutePath.startsWith("/")) {
            throw new IllegalArgumentException("find() requires an absolute path");
        }
        //takePersistablePermissions();
        for (SharedDocumentFile root : roots) {
            if (absolutePath.equals(root.getPath())) {
                return root;
            }

            if (absolutePath.startsWith(root.getPath() + "/")) {
                if (root.isFile()) {
                    return root;
                }
                return findChild(root, absolutePath.substring(root.getPath().length()+1));
            }
        }

        return null;
    }

    public void saveRoots() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (SharedDocumentFile root : roots) {
            //context.getContentResolver().takePersistableUriPermission(root.getUri(),
            //        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            editor.putString(root.getPathPrefix(), root.getUri().toString());
        }

        editor.apply();
    }
    public SharedDocumentFileManager loadRoots() {
        roots.clear();

        for (String path : sharedPreferences.getAll().keySet()) {
            String uriString = sharedPreferences.getString(path, null);
            if (uriString != null) {
                Uri uri = Uri.parse(uriString);
                SharedDocumentFile sharedDocumentFile = new SharedDocumentFile(context, uri, path);
                roots.add(sharedDocumentFile);

            }
        }

        return this;
    }

    private SharedDocumentFile findChild(SharedDocumentFile parent, String relativePath) {
        if (relativePath.contains("/")) {
            String childName = relativePath.substring(0, relativePath.indexOf("/"));
            String remaining = relativePath.substring(relativePath.indexOf("/")+1);
            SharedDocumentFile childFile = new SharedDocumentFile(parent, childName);
            return findChild(childFile, remaining);
        } else {
            return new SharedDocumentFile(parent, relativePath);
        }
    }

    private void takePersistablePermissions() {
        List<UriPermission> persistedUriPermissions = context.getContentResolver().getPersistedUriPermissions();

        for (UriPermission permission : persistedUriPermissions) {
            // For each persisted Uri, take persistable URI permission
            context.getContentResolver().takePersistableUriPermission(
                    permission.getUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        }
    }


}
