package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Task;

public class TasksDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static TasksDb instance;

    private TasksDb() {
    }

    public static TasksDb getInstance() {
        if (TasksDb.instance == null) {
            TasksDb.instance = new TasksDb();
        }
        return TasksDb.instance;
    }

    public void save(List<Task> posttasks) {
        for (Task posttask : posttasks) {
            save(posttask);
        }
    }

    public void save(Task task) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        // Put values into ContentValues
        cv.put("task_id", task.getTaskId());
        cv.put("id_course", task.getId_course());
        cv.put("id_post", task.getId_post());
        cv.put("description", task.getDescription());
        cv.put("date", task.getDate().getTime() / 1000);
        cv.put("isdone", task.isDone());

        // Add or update task in db
        Cursor cursor = db.rawQuery("SELECT * FROM tasks WHERE task_id = '" + task.getTaskId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("tasks", null, cv);
        } else {
            // Only update done status if it is true
            if (!task.isDone()) cv.remove("isdone");
            db.update("tasks", cv, "task_id = '" + task.getTaskId() + "'", null);
        }
        cursor.close();
    }

    /**
     * Gets all tasks
     *
     * @return List of all PostTasks
     */
    public List<Task> getAll() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM tasks ORDER BY pinned DESC, date DESC";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    /**
     * Gets all undone tasks
     *
     * @return List of all undone tasks
     */
    public List<Task> getUndone() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM tasks WHERE isdone=0 ORDER BY pinned DESC, date DESC";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<Task> getByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query tasks
        String queryString = "SELECT * FROM tasks WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public int getUndoneByCourseIdCount(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        String queryString = "SELECT COUNT(*) FROM tasks WHERE id_course = '" + course_id + "' AND isdone=0";
        Cursor cursor = db.rawQuery(queryString, null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public List<Task> getByPostId(String post_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM tasks WHERE id_post = '" + post_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<Task> getByDate(String date) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM tasks WHERE date = '" + date + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<Task> getByTask(String Task) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM tasks WHERE task_id = '" + Task + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }


    public List<Task> getByIsDone(Boolean isDone) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM tasks WHERE isDone = " + (isDone ? "1" : "0");
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public boolean existAny() {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT * FROM tasks LIMIT 1", null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    /**
     * Gets whether a post has a task and if it's done
     *
     * @param postId Post to check for
     * @return null if the post does not have a task, true if it is done, false if not
     */
    public Boolean taskDone(String postId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT isdone FROM tasks WHERE id_post=\"" + postId + "\"", null);
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

    public void setDone(String postId, boolean isDone) {
        dbhelper.getReadableDatabase()
                .execSQL("UPDATE tasks SET isdone = " + (isDone ? "1" : "0") + " WHERE id_post IS \"" + postId + "\"");
    }

    /**
     * Specify if a task is pinned or not
     *
     * @param taskId   The task's task id (not post id)
     * @param isPinned Whether or not the task should be pinned
     */
    public void setPinned(String taskId, boolean isPinned) {
        dbhelper.getWritableDatabase().execSQL("UPDATE tasks SET pinned="
                + (isPinned ? 1 : 0) + " WHERE task_id=\"" + taskId + "\"");
    }

    private List<Task> getWithCursor(Cursor cursor) {
        List<Task> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String taskid = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String description = cursor.getString(3);
                Date date = new Date(cursor.getInt(4) * 1000L);
                boolean isDone = cursor.getInt(5) == 1;
                boolean isPinned = cursor.getInt(6) == 1;

                Task newTask = new Task(taskid, id_course, id_post, description, date, isDone, isPinned);

                returnList.add(newTask);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }

}
