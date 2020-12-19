package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Post;
import de.koenidv.sph.objects.PostAttachment;

public class PostAttachmentDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostAttachmentDb instance;

    private PostAttachmentDb() {
    }

    public static PostAttachmentDb getInstance() {
        if (PostAttachmentDb.instance == null) {
            PostAttachmentDb.instance = new PostAttachmentDb();
        }
        return PostAttachmentDb.instance;
    }

    public void save(List<PostAttachment> PostAttachments) {
        for (PostAttachment postAttachment : PostAttachments) {
            save(PostAttachments);
        }
    }


    public void save(PostAttachment postAttachment) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("attachmentId",postAttachment.getAttachmentId());
        cv.put("id_course",postAttachment.getId_course());
        cv.put("id_post",postAttachment.getId_post());
        cv.put("name",postAttachment.getName());
        cv.put("date",postAttachment.getDate().getTime());
        cv.put("url",postAttachment.getUrl().toString());
        if(postAttachment.getDeviceLocation()!=null)cv.put("deviceLocation",postAttachment.getDeviceLocation().toString());




        Cursor cursor = db.rawQuery("SELECT * FROM postAttachment WHERE name = '" + postAttachment.getAttachmentId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("tiles", null, cv);
        } else {
            db.update("tiles", cv, "name = '" + postAttachment.getAttachmentId() + "'", null);
        }
        cursor.close();
        db.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<PostAttachment> getPostByCourseId(String course_id) throws MalformedURLException {
        List<PostAttachment> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM post WHERE course_id = '" + course_id + "'";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String attachmentId = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String name = cursor.getString(3);
                Date date = new Date(cursor.getInt(4));
                URL url = new URL(cursor.getString(5));
                Path deviceLocation = Paths.get(cursor.getString(6));


                PostAttachment newPostAttachment = new PostAttachment(attachmentId, id_course, id_post, name, date,url,deviceLocation);

                returnList.add(newPostAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return returnList;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<PostAttachment> getPostByPostId(String postid) throws MalformedURLException {
        List<PostAttachment> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM post WHERE postid = '" + postid + "'";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String attachmentId = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String name = cursor.getString(3);
                Date date = new Date(cursor.getInt(4));
                URL url = new URL(cursor.getString(5));
                Path deviceLocation = Paths.get(cursor.getString(6));


                PostAttachment newPostAttachment = new PostAttachment(attachmentId, id_course, id_post, name, date,url,deviceLocation);

                returnList.add(newPostAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return returnList;
    }
}
