package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.PostLink;

public class PostLinksDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostLinksDb instance;

    private PostLinksDb() {
    }

    public static PostLinksDb getInstance() {
        if (PostLinksDb.instance == null) {
            PostLinksDb.instance = new PostLinksDb();
        }
        return PostLinksDb.instance;
    }

    public void save(List<PostLink> postlinks) {
        for (PostLink postlink : postlinks) {
            save(postlink);
        }
    }

    public void save(PostLink postlink) {
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
        Cursor cursor = db.rawQuery("SELECT * FROM postLinks WHERE post_id = '" + postlink.getId_post() + "'AND url = '" + postlink.getUrl() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("postLinks", null, cv);
        } else {
            // Don't update pinned attribute
            cv.remove("pinned");
            db.update("postLinks", cv, "post_id = '" + postlink.getId_post() + "'AND url = '" + postlink.getUrl() + "'", null);
        }
        cursor.close();
    }


    public List<PostLink> getByCourseId(String course_id) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postLinks WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<PostLink> getByPostId(String post_id) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postLinks WHERE id_post = '" + post_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<PostLink> getByDate(String date) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postLinks WHERE date = '" + date + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }


    private List<PostLink> getWithCursor(Cursor cursor) throws MalformedURLException {
        List<PostLink> returnList = new ArrayList<>();
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

                PostLink newPostLink = new PostLink(id_course, id_post, name, date, url, pinned, lastUse);

                returnList.add(newPostLink);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }
}

