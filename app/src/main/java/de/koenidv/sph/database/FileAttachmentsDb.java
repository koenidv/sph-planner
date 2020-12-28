package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Attachment;
import de.koenidv.sph.objects.FileAttachment;

public class FileAttachmentsDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static FileAttachmentsDb instance;

    private FileAttachmentsDb() {
    }

    public static FileAttachmentsDb getInstance() {
        if (FileAttachmentsDb.instance == null) {
            FileAttachmentsDb.instance = new FileAttachmentsDb();
        }
        return FileAttachmentsDb.instance;
    }

    /**
     * This will save or update a list of post file attachments to the db
     * Does not update pinned attribute
     *
     * @param fileAttachments list of post file attachments to save
     */
    public void save(List<FileAttachment> fileAttachments) {
        for (FileAttachment fileAttachment : fileAttachments) {
            save(fileAttachment);
        }
    }

    /**
     * This will save or update a post file attachment to the db
     * Does not update pinned attribute
     *
     * @param fileAttachment post file attachment to save
     */
    public void save(FileAttachment fileAttachment) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("attachment_id", fileAttachment.getAttachmentId());
        cv.put("id_course", fileAttachment.getId_course());
        cv.put("id_post", fileAttachment.getId_post());
        cv.put("name", fileAttachment.getName());
        cv.put("date", fileAttachment.getDate().getTime() / 1000);
        cv.put("url", fileAttachment.getUrl());
        cv.put("size", fileAttachment.getFileSize());
        cv.put("type", fileAttachment.getFileType());
        cv.put("pinned", fileAttachment.getPinned());
        if (fileAttachment.getLastUse() != null)
            cv.put("lastUse", fileAttachment.getLastUse().getTime() / 1000);

        Cursor cursor = db.rawQuery("SELECT * FROM fileAttachments WHERE attachment_id = '" + fileAttachment.getAttachmentId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("fileAttachments", null, cv);
        } else {
            // Don't overwrite name, pinned and lastUse attribute
            // If pinned status should be changed, use #setPinned
            // If last use should be updated, use #used
            cv.remove("pinned");
            cv.remove("lastUse");
            cv.remove("name");
            db.update("fileAttachments", cv, "attachment_id = '" + fileAttachment.getAttachmentId() + "'", null);
        }
        cursor.close();
    }

    /**
     * Updates an attached file's last use to the current date
     *
     * @param attachmentId Id of the file to change
     */
    public void used(String attachmentId) {
        dbhelper.getWritableDatabase().execSQL("UPDATE fileAttachments SET lastUse="
                + new Date().getTime() / 1000 + " WHERE attachment_id=\"" + attachmentId + "\"");
    }

    /**
     * Mark an attached file as (not) pinned
     *
     * @param attachmentId Id of the file to change
     * @param pinned       Whether the file is pinned
     */
    public void setPinned(String attachmentId, boolean pinned) {
        dbhelper.getWritableDatabase().execSQL("UPDATE fileAttachments SET pinned="
                + (pinned ? 1 : 0) + " WHERE attachment_id=\"" + attachmentId + "\"");
    }

    /**
     * Check if an attached file in the db is pinned
     *
     * @param attachmentId Id of the file to check
     * @return true, if the attachment is pinned
     */
    public boolean isPinned(String attachmentId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery(
                "SELECT pinned FROM fileAttachments WHERE attachment_id=\""
                        + attachmentId + "\"", null);
        cursor.moveToFirst();
        boolean isPinned = cursor.getInt(0) == 1;
        cursor.close();
        return isPinned;
    }

    /**
     * Get a list of attachments with files for a course
     *
     * @param course_id Course to find attached files for
     * @return List of Attachments with FileAttachments
     */
    public List<Attachment> getByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        String queryString = "SELECT * FROM fileAttachments WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        return cursorToAttachment(cursor);
    }

    /**
     * Get a list of pinned attachments with files for a course
     * The list will be sorted by last use
     *
     * @param course_id Course to find pinned attached files for
     * @return List of pinned dttachments with FileAttachments, sorted by last use
     */
    public List<Attachment> getPinsByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        String queryString = "SELECT * FROM fileAttachments WHERE pinned=1 " +
                "AND id_course = '" + course_id + "' ORDER BY lastUse DESC";
        Cursor cursor = db.rawQuery(queryString, null);
        return cursorToAttachment(cursor);
    }

    public List<FileAttachment> getByPostId(String postid) {
        List<FileAttachment> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM fileAttachments WHERE id_post = '" + postid + "'";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String attachmentId = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String name = cursor.getString(3);
                Date date = new Date(cursor.getInt(4) * 1000L);
                String url = cursor.getString(5);
                String size = cursor.getString(6);
                String type = cursor.getString(7);
                boolean pinned = cursor.getInt(8) == 1;
                Date lastUse = null;
                if (!cursor.isNull(9)) lastUse = new Date(cursor.getInt(9) * 1000);

                FileAttachment newFileAttachment = new FileAttachment(attachmentId, id_course, id_post, name, date, url, size, type, pinned, lastUse);

                returnList.add(newFileAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }

    public List<Attachment> cursorToAttachment(Cursor cursor) {
        List<Attachment> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String attachmentId = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String name = cursor.getString(3);
                Date date = new Date(cursor.getInt(4) * 1000L);
                String url = cursor.getString(5);
                String size = cursor.getString(6);
                String type = cursor.getString(7);
                boolean pinned = cursor.getInt(8) == 1;
                Date lastUse = null;
                if (!cursor.isNull(9)) lastUse = new Date(cursor.getInt(9) * 1000);

                Attachment newAttachment = new Attachment(new FileAttachment(attachmentId, id_course, id_post, name, date, url, size, type, pinned, lastUse));

                returnList.add(newAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }
}
