package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Attachment;
import de.koenidv.sph.objects.LinkAttachment;

public class LinkAttachmentsDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static LinkAttachmentsDb instance;

    private LinkAttachmentsDb() {
    }

    public static LinkAttachmentsDb getInstance() {
        if (LinkAttachmentsDb.instance == null) {
            LinkAttachmentsDb.instance = new LinkAttachmentsDb();
        }
        return LinkAttachmentsDb.instance;
    }

    public void save(List<LinkAttachment> postlinks) {
        for (LinkAttachment postlink : postlinks) {
            save(postlink);
        }
    }

    public void save(LinkAttachment postlink) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        // Put values into ContentValues
        cv.put("attachment_id", postlink.getAttachment_id());
        cv.put("id_course", postlink.getId_course());
        cv.put("id_post", postlink.getId_post());
        cv.put("name", postlink.getName());
        cv.put("date", postlink.getDate().getTime() / 1000);
        cv.put("url", postlink.getUrl());
        cv.put("pinned", postlink.getPinned());
        if (postlink.getLastUse() != null)
            cv.put("lastUse", postlink.getLastUse().getTime() / 1000);

        // Add or update post in db
        Cursor cursor = db.rawQuery("SELECT * FROM linkAttachments WHERE attachment_id = \"" + postlink.getAttachment_id() + "\"", null);
        if (cursor.getCount() == 0) {
            db.insert("linkAttachments", null, cv);
        } else {
            // Don't update pinned attribute
            cv.remove("pinned");
            db.update("linkAttachments", cv, "attachment_id = \"" + postlink.getAttachment_id() + "\"", null);
        }
        cursor.close();
    }

    /**
     * Updates an attached link's last use to the current date
     *
     * @param attachmentId Id of the link to change
     */
    public void used(String attachmentId) {
        dbhelper.getWritableDatabase().execSQL("UPDATE linkAttachments SET lastUse="
                + new Date().getTime() / 1000 + " WHERE attachment_id=\"" + attachmentId + "\"");
    }

    /**
     * Mark an attached link as (not) pinned
     *
     * @param attachmentId Id of the attachment to change
     * @param pinned       Whether the link is pinned
     */
    public void setPinned(String attachmentId, boolean pinned) {
        dbhelper.getWritableDatabase().execSQL("UPDATE linkAttachments SET pinned="
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
                "SELECT pinned FROM linkAttachments WHERE attachment_id=\""
                        + attachmentId + "\"", null);
        cursor.moveToFirst();
        boolean isPinned = cursor.getInt(0) == 1;
        cursor.close();
        return isPinned;
    }

    /**
     * Get a list of pinned attachments with links for a course
     *
     * @param course_id Course to find attached links for
     * @return List of pinned Attachments with LinkAttachments
     */
    public List<Attachment> getPinnedByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM linkAttachments WHERE pinned=1 AND id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<Attachment> getByPostId(String post_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM linkAttachments WHERE id_post = '" + post_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<Attachment> getByDate(String date) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM linkAttachments WHERE date = '" + date + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }


    private List<Attachment> getWithCursor(Cursor cursor) {
        List<Attachment> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {

                String attachment_id = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String name = cursor.getString(3);
                Date date = new Date(cursor.getInt(4) * 1000L);
                String url = cursor.getString(5);
                boolean pinned = cursor.getInt(6) == 1;
                Date lastUse = null;
                if (!cursor.isNull(7)) lastUse = new Date(cursor.getInt(7) * 1000);

                Attachment newAttachment = new Attachment(new LinkAttachment(attachment_id, id_course, id_post, name, date, url, pinned, lastUse));

                returnList.add(newAttachment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }
}

