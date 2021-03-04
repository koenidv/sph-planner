package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.adapters.CompactPostsAdapter;
import de.koenidv.sph.objects.Post;

public class PostsDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostsDb instance;

    private PostsDb() {
    }

    public static PostsDb getInstance() {
        if (PostsDb.instance == null) {
            PostsDb.instance = new PostsDb();
        }
        return PostsDb.instance;
    }

    public void save(@NotNull List<Post> posts) {
        for (Post post : posts) {
            save(post);
        }
    }

    public void save(@NotNull Post post) {
        final SQLiteDatabase db = dbhelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        // Put values into ContentValues
        cv.put("post_id", post.getPostId());
        cv.put("id_course", post.getId_course());
        cv.put("date", post.getDate().getTime() / 1000);
        cv.put("title", post.getTitle());
        if (post.getDescription() != null) cv.put("description", post.getDescription());
        cv.put("unread", post.getUnread());

        // Add or update post in db
        Cursor cursor = db.rawQuery("SELECT * FROM posts WHERE post_id = '" + post.getPostId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("posts", null, cv);
        } else {
            db.update("posts", cv, "post_id = '" + post.getPostId() + "'", null);
        }
        cursor.close();
    }

    public List<Post> getAll() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<Post> getAll(int limit) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts ORDER BY date DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public List<Post> getAllOrderedByUnread() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts ORDER BY unread DESC, date DESC";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    /**
     * Get posts data as CompactPostsAdapter.PostData
     */
    public List<CompactPostsAdapter.PostData> getData(String where) {
        return getData(where, "ORDER BY posts.date DESC");
    }

    /**
     * Get posts data as CompactPostsAdapter.PostData
     */
    public List<CompactPostsAdapter.PostData> getData(String where, String appendQuery) {
        // Query posts
        String queryString = "SELECT post_id, fullname, title, posts.description, COUNT(files.attachment_id) + COUNT(links.attachment_id), unread, color, isdone, posts.date" +
                " FROM posts LEFT JOIN courses ON posts.id_course = course_id" +
                " LEFT JOIN fileAttachments files ON post_id = files.id_post" +
                " LEFT JOIN linkAttachments links ON post_id = links.id_post" +
                " LEFT JOIN tasks ON post_id = tasks.id_post" +
                " WHERE " + where +
                " GROUP BY post_id " + appendQuery;
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery(queryString, null);
        List<CompactPostsAdapter.PostData> list = new ArrayList<>();

        // If cursor is empty, return empty list
        if (!cursor.moveToFirst()) {
            cursor.close();
            return list;
        }

        do {
            list.add(new CompactPostsAdapter.PostData(
                    cursor.getString(0),
                    cursor.getString(1) + "", // Non-Null String!
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getInt(4),
                    cursor.getInt(5) == 1,
                    cursor.getInt(6),
                    cursor.getInt(7) == 1,
                    new Date(cursor.getInt(8) * 1000L)
            ));
        } while (cursor.moveToNext());

        cursor.close();
        return list;
    }


    public List<Post> getByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts WHERE id_course = '" + course_id + "' ORDER BY date DESC";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    /**
     * Gets the latest n entries for a specific course
     *
     * @param course_id Course to get posts for
     * @param limit     Maximum amount of posts
     * @return List of n newest posts for a course
     */
    public List<Post> getByCourseId(String course_id, int limit) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts WHERE id_course = '" + course_id + "' " +
                "AND post_id NOT NULL ORDER BY date DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public Post getByPostId(String post_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts WHERE post_id = \"" + post_id + "\"";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        List<Post> list = getWithCursor(cursor);
        return !list.isEmpty() ? list.get(0) : null;
    }

    public List<Post> getUnread() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts WHERE unread = 1 ORDER BY date DESC";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get post objects and return them
        return getWithCursor(cursor);
    }

    public List<Post> getRead(int limit) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts WHERE unread = 0 ORDER BY date DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor);
    }

    public int getUnreadByCourseIdCount(String courseId) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT COUNT(*) FROM posts WHERE id_course = \"" + courseId + "\" AND unread = 1 ORDER BY date DESC";
        Cursor cursor = db.rawQuery(queryString, null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    protected Post cursorToPost(Cursor cursor) {
        return new Post(
                cursor.getString(0),
                cursor.getString(1),
                new Date(cursor.getInt(2) * 1000L),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getInt(5) == 1);
    }

    private List<Post> getWithCursor(Cursor cursor) {
        List<Post> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToPost(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }

    /**
     * Mark all posts for a course as read
     *
     * @param courseId Internal Id of the course where the posts belong
     */
    public void markCourseAsRead(String courseId) {
        dbhelper.getReadableDatabase().execSQL("UPDATE posts SET unread = 0 WHERE id_course IS \"" + courseId + "\"");
    }

    /**
     * Mark all posts with specific post ids as read
     *
     * @param postIds Internal Ids of the posts to mark read
     */
    public void markAsRead(String... postIds) {
        for (String postId : postIds) {
            dbhelper.getReadableDatabase().execSQL("UPDATE posts SET unread = 0 WHERE post_id = \"" + postId + "\"");
        }
    }

    /**
     * Clears all posts from posts db
     */
    public void clear() {
        SQLiteDatabase db = dbhelper.getWritableDatabase();
        db.delete("posts", null, null);
    }

}
