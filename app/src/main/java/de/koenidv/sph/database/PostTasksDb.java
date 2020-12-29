package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.PostTask;

public class PostTasksDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostTasksDb instance;

    private PostTasksDb() {
    }

    public static PostTasksDb getInstance() {
        if (PostTasksDb.instance == null) {
            PostTasksDb.instance = new PostTasksDb();
        }
        return PostTasksDb.instance;
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
        cv.put("task_id", postTask.getTaskId());
        cv.put("id_course", postTask.getId_course());
        cv.put("id_post",postTask.getId_post());
        cv.put("description", postTask.getDescription());
        cv.put("date", postTask.getDate().getTime() / 1000);
        cv.put("isDone",postTask.isDone());
        // Add or update post in db
        Cursor cursor = db.rawQuery("SELECT * FROM postTasks WHERE id_post = '" + postTask.getTaskId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("postTasks", null, cv);
        } else {
            db.update("postTasks", cv, "post_id = '" + postTask.getTaskId() + "'", null);
        }
        cursor.close();
    }

    /**
     * Gets all tasks
     *
     * @return List of all PostTasks
     */
    public List<PostTask> getAll() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postTasks";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    /**
     * Gets all undone tasks
     *
     * @return List of all undone tasks
     */
    public List<PostTask> getUndone() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postTasks WHERE isdone=0";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }


    public List<PostTask> getByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postTasks WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostTask> getByPostId(String post_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postTasks WHERE id_post = '" + post_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostTask> getByDate(String date) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postTasks WHERE date = '" + date + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostTask> getByTask(String Task) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postTasks WHERE task_id = '" + Task + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }


    public List<PostTask> getByIsDone() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postTasks WHERE isDone = 1";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    /**
     * Gets whether a post has a task and if it's done
     *
     * @param postId Post to check for
     * @return null if the post does not have a task, true if it is done, false if not
     */
    public Boolean taskDone(String postId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT isdone FROM postTasks WHERE id_post=\"" + postId + "\"", null);
        if (cursor.getCount() == 0) {
            // No task for this post
            cursor.close();
            return null;
        } else {
            cursor.moveToFirst();
            boolean done = cursor.getInt(0) == 1;
            cursor.close();
            return done;
        }
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
        return returnList;
    }

}
