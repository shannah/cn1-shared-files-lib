package com.codenameone.ext.io.sharedfiles;


public abstract class SharedFileManager {

	public SharedFileManager() {
	}

	public static void setInstance(SharedFileManager mgr) {
	}

	public static SharedFileManager getInstance() {
	}

	public <any> openFile() {
	}

	public abstract <any> openFile(String mimeType) {
	}

	public abstract <any> openDirectory() {
	}

	public abstract SharedFile openBookmark(String bookmark) {
	}

	public abstract SharedFile[] getBookmarks() {
	}
}
