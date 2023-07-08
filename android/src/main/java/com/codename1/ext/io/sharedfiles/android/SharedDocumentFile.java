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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import androidx.annotation.RequiresApi;

import com.codename1.impl.android.AndroidNativeUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class SharedDocumentFile {
    private Context context;
    private Uri uri;
    private ContentResolver contentResolver;

    private String name;
    private SharedDocumentFile parent;

    private String pathPrefix = "";

    public static String BOOKMARK_PREFIX = "/cn1-bookmarks/";

    public SharedDocumentFile(Context context, Uri uri, String pathPrefix) {
        this.context = context;
        this.uri = uri;
        this.contentResolver = context.getContentResolver();
        this.pathPrefix = pathPrefix;
    }

    public SharedDocumentFile(SharedDocumentFile parentDirectory, String fileName) {

        this.context = parentDirectory.context;
        this.contentResolver = parentDirectory.contentResolver;
        this.parent = parentDirectory;
        this.name = fileName;
        this.pathPrefix = parentDirectory.pathPrefix;
    }

    public SharedDocumentFile withPathPrefix(String prefix) {
        SharedDocumentFile clone = clone();
        clone.pathPrefix = prefix;

        return clone;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public String getPath() {
        String internalPath = getPathInternal();
        if (!internalPath.isEmpty() && !internalPath.startsWith("/")) {
            internalPath = "/" + internalPath;
        }
        return pathPrefix + internalPath;
    }

    private String getPathInternal() {
        SharedDocumentFile parent = getParent();
        if (parent == null) {
            return getName();
        }

        String parentPath = parent.getPathInternal();
        if (parentPath == "") {
            parentPath = "/";
        }

        if (!parentPath.endsWith("/")) {
            parentPath += "/";
        }
        return parentPath + getName();
    }

    public SharedDocumentFile getParent() {
        if (parent != null) {
            return parent;
        }
        Uri uri = getUri();
        if (uri == null) {
            return null;
        }
        String documentId;
        try {
            documentId = DocumentsContract.getDocumentId(uri);

        } catch (IllegalArgumentException ex) {
            documentId = DocumentsContract.getTreeDocumentId(uri);
            if (documentId.indexOf(":") == documentId.lastIndexOf(":")) {
                return null;
            }
        }

        int lastColon = documentId.lastIndexOf(":");
        if (lastColon == -1) {
            return null;
        }
        String parentDocumentId = documentId.substring(0, lastColon);
        Uri parentUri;
        try {
            parentUri = DocumentsContract.buildDocumentUriUsingTree(uri, parentDocumentId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return new SharedDocumentFile(context, parentUri, pathPrefix);
    }


    public boolean exists() {
        if (uri != null) {
            return true;
        }
        if (parent != null && !parent.exists()) {
            return false;
        }
        // Obtain the children Uri of the parent directory
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent.getUri(),
                DocumentsContract.getTreeDocumentId(parent.getUri()));

        // Query for the document with the specified display name
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(childrenUri,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME + "=?",
                    new String[]{name}, null);

            // If the query returned a result, the file exists
            while (cursor != null && cursor.moveToNext()) {
                String childDocumentId = cursor.getString(Math.max(0, cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)));
                String childName = cursor.getString(Math.max(0, cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)));
                if (name.equals(childName)) {
                    uri = DocumentsContract.buildDocumentUriUsingTree(parent.uri, childDocumentId);
                    return true;
                }

            }
        } catch (Exception e) {
            // Ignore exception and return false
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // If no result was returned or an error occurred, the file does not exist
        return false;
    }


    public boolean isDir() {
        if (!exists()) {
            return false;
        }
        Uri uri = getUri();
        if (uri == null) {
            return false;
        }
        try {
            String docId = DocumentsContract.getDocumentId(uri);
            return docId.endsWith(":");
        } catch (IllegalArgumentException ex) {
            DocumentsContract.getTreeDocumentId(uri);
            return true;
        }
    }

    public boolean isFile() {
        return exists() && !isDir();
    }

    public SharedDocumentFile[] listFiles() {
        if (!isDir()) {
            throw new IllegalArgumentException("Only directories support listFiles()");
        }
        Uri uri = getUri();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                DocumentsContract.getTreeDocumentId(uri));
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            }, null, null, null);

            if (cursor == null) {
                return new SharedDocumentFile[0];
            } else {
                int count = cursor.getCount();
                SharedDocumentFile[] files = new SharedDocumentFile[count];
                while (cursor.moveToNext()) {
                    String childDocumentId = cursor.getString(0);
                    Uri childUri = DocumentsContract.buildDocumentUriUsingTree(uri, childDocumentId);
                    files[cursor.getPosition()] = new SharedDocumentFile(context, childUri, pathPrefix);
                }
                return files;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    public InputStream openInputStream() throws FileNotFoundException {
        if (!isFile()) {
            throw new IllegalArgumentException("Uri has to represent a file, not a directory.");
        }
        return contentResolver.openInputStream(getUri());
    }

    public OutputStream openOutputStream(String mimeType) throws IOException {
        if (isDir()) {
            throw new IllegalArgumentException("Cannot write a directory");
        }
        Uri parentUri = getParent().getUri();
        if (parentUri == null) {
            throw new FileNotFoundException("Parent Uri is null.");
        }

        Uri fileUri;

        if (getUri() != null) {
            // File already exists, use the existing Uri
            fileUri = getUri();
        } else {
            // Create a new file within the parent directory
            String fileName = getName();
            String docId = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                docId = DocumentsContract.getTreeDocumentId(parentUri);
            }
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, docId);
            fileUri = DocumentsContract.createDocument(contentResolver, documentUri, mimeType, fileName);
            if (fileUri == null) {
                throw new IOException("Failed to create the file: " + fileName);
            }
        }

        OutputStream outputStream = contentResolver.openOutputStream(fileUri);
        if (outputStream == null) {
            throw new IOException("Failed to open the output stream for: " + fileUri);
        }

        return outputStream;

    }

    public String getName() {
        if (name != null) {
            return name;
        }
        Uri uri = getUri();
        if (uri == null) {
            return "";
        }
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(Math.max(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME), 0));
                return name;
            }
        } catch (UnsupportedOperationException ex) {
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }


    public long getSize() {
        Uri uri = getUri();
        if (uri == null) {
            return 0;
        }
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(Math.max(cursor.getColumnIndex(OpenableColumns.SIZE), 0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }


    public boolean delete() {
        Uri uri = getUri();
        if (uri == null) {
            System.out.println("No delete because no uri");
            return false;
        }
        try {
            return DocumentsContract.deleteDocument(contentResolver, uri);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Uri getUri() {

        if (uri == null) {
            String childDocumentId = null;

            // Obtain the children Uri of the parent directory
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent.getUri(),
                    DocumentsContract.getTreeDocumentId(parent.getUri()));

            // Query for the document with the specified display name
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(childrenUri,
                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME + "=?",
                        new String[]{name}, null);

                // If the query returned a result, obtain the document ID of the child file
                while (cursor != null && cursor.moveToNext()) {
                    childDocumentId = cursor.getString(Math.max(0, cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)));
                    String childName = cursor.getString(Math.max(0, cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)));
                    if (name.equals(childName)) {
                        uri = DocumentsContract.buildDocumentUriUsingTree(parent.uri, childDocumentId);
                        break;
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Error while trying to get the child document ID", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return uri;
    }

    public SharedDocumentFile clone() {

        if (uri != null) {
            return new SharedDocumentFile(context, uri, pathPrefix);
        }

        return new SharedDocumentFile(parent, name);
    }

    public void renameTo(SharedDocumentFile sharedDocumentFile) throws IOException{
        if (!exists()) {
            throw new FileNotFoundException("Cannot rename file that doesn't exist");
        }
        if (sharedDocumentFile.getParent() == null || !sharedDocumentFile.getParent().exists()) {
            throw new FileNotFoundException("Cannot find destination file");
        }
        if (sharedDocumentFile.exists()) {
            throw new IOException("Destination file already exists");
        }
        if (getParent().getUri().equals(sharedDocumentFile.getParent().getUri())) {
            DocumentsContract.renameDocument(contentResolver, getUri(), sharedDocumentFile.getName());
            return;
        }
        if (isFile()) {
            copyTo(sharedDocumentFile);
            delete();
        } else {
            throw new IOException("Cannot rename directory in a different destination directory.");
        }
    }

    public boolean copyTo(SharedDocumentFile destination) throws IOException {
        if (!exists()) {
            throw new FileNotFoundException("Source file does not exist");
        }
        if (destination.getParent() == null || !destination.getParent().exists()) {
            throw new FileNotFoundException("Destination directory not found");
        }

        // Get InputStream for source file
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = openInputStream();
            outputStream = destination.openOutputStream(getMimeType());

            // Ensure streams are available
            if (inputStream == null || outputStream == null) {
                return false;
            }

            // Copy source file to destination
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return true;
        } finally {
            // Close streams
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ex) {}
            }
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
        }
    }

    public String getMimeType() {
        if (!exists()) {
            return null;
        }

        return contentResolver.getType(getUri());
    }

    public boolean isBookmarked() {
        return pathPrefix.startsWith(BOOKMARK_PREFIX);
    }

    public SharedDocumentFile createBookmark() {
        String bookmarkId = UUID.randomUUID().toString();
        String newPathPrefix = BOOKMARK_PREFIX + bookmarkId;
        SharedDocumentFileManager mgr = new SharedDocumentFileManager(context);
        SharedDocumentFile out = mgr.addRoot(this, newPathPrefix);
        mgr.saveRoots();

        return out;
    }

    public SharedDocumentFile getOrCreateBookmark() {
        if (isBookmarked()) {
            return this;
        }

        return createBookmark();
    }

    public SharedDocumentFile deleteBookmark() {
        if (!isBookmarked()) {
            return this;
        }

        SharedDocumentFileManager mgr = new SharedDocumentFileManager(context);
        mgr.removeRoot(this);

        return withPathPrefix("");
    }

    private Uri getParentDocumentUri(Uri documentUri) {
        String docId;
        try {
            docId= DocumentsContract.getDocumentId(documentUri);
        } catch (IllegalArgumentException ex) {
            docId = DocumentsContract.getTreeDocumentId(documentUri);
        }
        String[] split = docId.split(":");
        String type = split[0];
        switch (type) {
            case "raw":
                return null; // Raw files don't have a parent
            case "tree":
                return DocumentsContract.buildDocumentUriUsingTree(documentUri, docId);
            default:
                try {
                    String parentDocumentId = DocumentsContract.getTreeDocumentId(documentUri);
                    if (parentDocumentId != null) {
                        return DocumentsContract.buildDocumentUriUsingTree(documentUri, parentDocumentId);
                    } else {
                        return null;
                    }
                } catch (IllegalArgumentException ex) {
                    return null;
                }

        }
    }
}
