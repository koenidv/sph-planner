package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.PostAttachment;

public class PostAttachmentsDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostAttachmentsDb instance;

    private PostAttachmentsDb() {
    }

    public static PostAttachmentsDb getInstance() {
        if (PostAttachmentsDb.instance == null) {
            PostAttachmentsDb.instance = new PostAttachmentsDb();
        }
        return PostAttachmentsDb.instance;
    }

    /**
     * This will save or update a list of post file attachments to the db
     * Does not update pinned attribute
     *
     * @param postAttachments list of post file attachments to save
     */
    public void save(List<PostAttachment> postAttachments) {
        for (PostAttachment postAttachment : postAttachments) {
            save(postAttachment);
        }
    }

    /**
     * This will save or update a post file attachment to the db
     * Does not update pinned attribute
     *
     * @param postAttachment post file attachment to save
     */
    public void save(PostAttachment postAttachment) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("attachment_id", postAttachment.getAttachmentId());
        cv.put("id_course", postAttachment.getId_course());
        cv.put("id_post", postAttachment.getId_post());
        cv.put("name", postAttachment.getName());
        cv.put("date", postAttachment.getDate().getTime() / 1000);
        cv.put("url", postAttachment.getUrl());
        cv.put("size", postAttachment.getFileSize());
        cv.put("type", postAttachment.getFileType());
        cv.put("pinned", postAttachment.getPinned());
        if (postAttachment.getLastUse() != null)
            cv.put("lastUse", postAttachment.getLastUse().getTime() / 1000);

        Cursor cursor = db.rawQuery("SELECT * FROM postAttachments WHERE attachment_id = '" + postAttachment.getAttachmentId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("postAttachments", null, cv);
        } else {
            // Don't overwrite pinned attribute
            // If pinned status should be changed, use #setPinned
            cv.remove("pinned");
            db.update("postAttachments", cv, "attachment_id = '" + postAttachment.getAttachmentId() + "'", null);
        }
        cursor.close();
    }

    /**
     * Updates an attached file's last use to the current date
     *
     * @param attachmentId Id of the file to change
     */
    public void used(String attachmentId) {
        dbhelper.getWritableDatabase().execSQL("UPDATE postAttachments SET lastUse="
                + new Date().getTime() / 1000 + " WHERE attachment_id=\"" + attachmentId + "\"");
    }

    /**
     * Mark an attached file as (not) pinned
     *
     * @param attachmentId Id of the file to change
     * @param pinned       Whether the file is pinned
     */
    public void setPinned(String attachmentId, boolean pinned) {
        dbhelper.getWritableDatabase().execSQL("UPDATE postAttachments SET pinned="
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
                "SELECT pinned FROM postAttachments WHERE attachment_id=\""
                        + attachmentId + "\"", null);
        cursor.moveToFirst();
        boolean isPinned = cursor.getInt(0) == 1;
        cursor.close();
        return isPinned;
    }

    public List<PostAttachment> getPostByCourseId(String course_id) {
        List<PostAttachment> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM postAttachments WHERE id_course = '" + course_id + "'";

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

                PostAttachment newPostAttachment = new PostAttachment(attachmentId, id_course, id_post, name, date, url, size, type, pinned, lastUse);

                returnList.add(newPostAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }

    public List<PostAttachment> getPostByPostId(String postid) {
        List<PostAttachment> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM postAttachments WHERE id_post = '" + postid + "'";

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

                PostAttachment newPostAttachment = new PostAttachment(attachmentId, id_course, id_post, name, date, url, size, type, pinned, lastUse);

                returnList.add(newPostAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }
}
