package com.codenameone.ext.io.sharedfiles;


public interface SharedFile {

	public java.io.InputStream openInputStream();

	public java.io.OutputStream openOutputStream(String mimetype);

	public SharedFile[] listFiles();

	public boolean copyTo(SharedFile destination);

	public SharedFile createBookmark();

	public boolean delete();

	public SharedFile deleteBookmark();

	public boolean exists();

	public String getMimeType();

	public String getName();

	public SharedFile getOrCreateBookmark();

	public SharedFile getParent();

	public String getPath();

	public String getPathPrefix();

	public long getSize();

	public boolean isBookmarked();

	public boolean isDir();

	public boolean isFile();

	public void renameTo(SharedFile destination);

	public SharedFile getChild(String fileName);
}
