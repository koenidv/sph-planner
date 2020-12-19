package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Post;
import de.koenidv.sph.objects.Tile;

public class PostDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostDb instance;

    private PostDb() {
    }

    public static PostDb getInstance() {
        if (PostDb.instance == null) {
            PostDb.instance = new PostDb();
        }
        return PostDb.instance;
    }



    public void save(List<Post> posts) {
        for (Post post : posts) {
            save(posts);
        }
    }


    public void save(Post post) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("postid",post.getPostId());
        cv.put("course_id",post.getId_course());
        cv.put("date",post.getDate().getTime());
        cv.put("title",post.getTitle());
        if(post.getDescription()!=null)cv.put("description",post.getDescription());
        cv.put("unread",post.getUnread());



        Cursor cursor = db.rawQuery("SELECT * FROM post WHERE name = '" + post.getPostId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("tiles", null, cv);
        } else {
            db.update("tiles", cv, "name = '" + post.getPostId() + "'", null);
        }
        cursor.close();
        db.close();
    }

    public List<Post> getPostByCourseId(String course_id) {
        List<Post> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM post WHERE course_id = '" + course_id + "'";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String Postid = cursor.getString(0);
                String id_course = cursor.getString(1);
                Date date = new Date(cursor.getInt(2));
                String title = cursor.getString(3);
                String description = cursor.getString(4);
                boolean unread = cursor.getInt(5)==1;


                Post newPost = new Post(Postid, id_course, date, title, description,unread);

                returnList.add(newPost);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return returnList;
    }

    public List<Post> getPostByIsUnread() {
        List<Post> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM post WHERE unread = 1";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String Postid = cursor.getString(0);
                String id_course = cursor.getString(1);
                Date date = new Date(cursor.getInt(2));
                String title = cursor.getString(3);
                String description = cursor.getString(4);
                boolean unread = cursor.getInt(5)==1;


                Post newPost = new Post(Postid, id_course, date, title, description,unread);

                returnList.add(newPost);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return returnList;
    }


}
