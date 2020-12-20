package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Post;
import de.koenidv.sph.objects.PostLink;
import de.koenidv.sph.objects.PostTask;

public class PostTaskDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostTaskDb instance;

    private PostTaskDb() {
    }

    public static PostTaskDb getInstance() {
        if (PostTaskDb.instance == null) {
            PostTaskDb.instance = new PostTaskDb();
        }
        return PostTaskDb.instance;
    }

    public void save(List<PostTask> posttasks) {
        for (PostTask posttask : posttasks) {
            save(posttask);
        }
    }

    public void save(PostTask postTask) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        // Put values into ContentValues
        cv.put("taskid", postTask.getTaskId());
        cv.put("id_course", postTask.getId_course());
        cv.put("id_post",postTask.getId_post());
        cv.put("description", postTask.getDescription());
        cv.put("date", postTask.getDate().getTime() / 1000);
        cv.put("isDone",postTask.isDone());
        // Add or update post in db
        Cursor cursor = db.rawQuery("SELECT * FROM postlink WHERE post_id = '" + postTask.getTaskId() + "'" , null);
        if (cursor.getCount() == 0) {
            db.insert("posttask", null, cv);
        } else {
            db.update("posttask", cv, "post_id = '" + postTask.getTaskId() + "'", null);
        }
        cursor.close();
        db.close();
    }

    public List<PostTask> getAll() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posttask";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }


    public List<PostTask> getByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posttask WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostTask> getByPostId(String post_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posttask WHERE id_post = '" + post_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostTask> getByDate(String date) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posttask WHERE date = '" + date + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostTask> getByTask(String Task) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posttask WHERE taskid = '" + Task + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }


    public List<PostTask> getByIsDone() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posttask WHERE isDone = 1";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }


    private List<PostTask> getWithCursor(Cursor cursor, SQLiteDatabase db) {
        List<PostTask> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String taskid = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String description = cursor.getString(3);
                Date date = new Date(cursor.getInt(4) * 1000L);
                boolean isdone = cursor.getInt(5) == 1;

                PostTask newPostTask = new PostTask(taskid, id_course, id_post, description, date, isdone);

                returnList.add(newPostTask);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

}
