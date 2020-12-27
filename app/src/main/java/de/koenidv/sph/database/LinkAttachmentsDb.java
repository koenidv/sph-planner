package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        cv.put("id_course", postlink.getId_course());
        cv.put("id_post", postlink.getId_post());
        cv.put("name", postlink.getName());
        cv.put("date", postlink.getDate().getTime() / 1000);
        cv.put("url", postlink.getUrl().toString());
        cv.put("pinned", postlink.getPinned());
        if (postlink.getLastUse() != null)
            cv.put("lastUse", postlink.getLastUse().getTime() / 1000);

        // Add or update post in db
        Cursor cursor = db.rawQuery("SELECT * FROM linkAttachments WHERE post_id = '" + postlink.getId_post() + "'AND url = '" + postlink.getUrl() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("linkAttachments", null, cv);
        } else {
            // Don't update pinned attribute
            cv.remove("pinned");
            db.update("linkAttachments", cv, "post_id = '" + postlink.getId_post() + "'AND url = '" + postlink.getUrl() + "'", null);
        }
        cursor.close();
    }


    public List<LinkAttachment> getByCourseId(String course_id) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM linkAttachments WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<LinkAttachment> getByPostId(String post_id) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM linkAttachments WHERE id_post = '" + post_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<LinkAttachment> getByDate(String date) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM linkAttachments WHERE date = '" + date + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }


    private List<LinkAttachment> getWithCursor(Cursor cursor) throws MalformedURLException {
        List<LinkAttachment> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {

                String id_course = cursor.getString(0);
                String id_post = cursor.getString(1);
                String name = cursor.getString(2);
                Date date = new Date(cursor.getInt(4) * 1000L);
                URL url = new URL(cursor.getString(5));
                boolean pinned = cursor.getInt(6) == 1;
                Date lastUse = null;
                if (!cursor.isNull(7)) lastUse = new Date(cursor.getInt(7) * 1000);

                LinkAttachment newPostLink = new LinkAttachment(id_course, id_post, name, date, url, pinned, lastUse);

                returnList.add(newPostLink);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }
}

