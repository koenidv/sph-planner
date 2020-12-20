package de.koenidv.sph.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import de.koenidv.sph.SphPlanner;

//  Created by R-Theis on 8.12.2020.
public class DatabaseHelper extends SQLiteOpenHelper {

    // todo close db on exit
    // todo escape everything we put in the db

    private static DatabaseHelper instance;

    private DatabaseHelper(@Nullable Context context) {
        super(context, "database", null, 1);
    }

    public static DatabaseHelper getInstance() {
        if (DatabaseHelper.instance == null) {
            DatabaseHelper.instance = new DatabaseHelper(SphPlanner.Companion.applicationContext());
        }
        return DatabaseHelper.instance;
    }

    // Create a new instance, even if one already exists
    // Used to recreate database after deletion
    public static DatabaseHelper newInstance() {
        DatabaseHelper.instance = new DatabaseHelper(SphPlanner.Companion.applicationContext());
        return DatabaseHelper.instance;
    }


    //create Tables for Database
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCoursesTable = "CREATE TABLE courses(course_id TEXT UNIQUE PRIMARY KEY, gmb_id TEXT UNIQUE," +
                " sph_id TEXT UNIQUE, named_id TEXT UNIQUE, number_id TEXT UNIQUE, fullname TEXT," +
                " id_teacher TEXT, isFavorite INTEGER, isLK INTEGER, color INTEGER)";
        String createChangesTable = "CREATE TABLE changes(change_id TEXT UNIQUE PRIMARY KEY," +
                " id_course TEXT UNIQUE, id_course_external TEXT UNIQUE, date TEXT , lessons TEXT," +
                " type TEXT, id_course_external_before TEXT, className TEXT, className_before TEXT," +
                " id_teacher TEXT, id_subsTeacher TEXT, room TEXT, room_before TEXT, description TEXT)";
        String createTilesTable = "CREATE TABLE tiles(name TEXT PRIMARY KEY," +
                " location TEXT, type TEXT, icon TEXT, color INTEGER)";
        String createPostTable = "CREATE TABLE posts(post_id TEXT PRIMARY KEY, id_course TEXT," +
                "date INTEGER,title TEXT, description TEXT, unread INTEGER)";
        String createPostAttachmentTable = "CREATE TABLE postAttachments(attachment_id TEXT PRIMARY KEY," +
                "id_course TEXT, id_post TEXT, name TEXT, date INTEGER, url TEXT, deviceLocation TEXT, size TEXT)";
        String createPostLinkTable = "CREATE TABLE postlink(id_course TEXT, id_post TEXT, name TEXT, date INTEGER, url TEXT)";
        String createPostTaskTable = "CREATE TABLE posttask(taskid TEXT, id_course TEXT, id_post TEXT, description TEXT, date INTEGER, isdone TEXT)";

        db.execSQL(createCoursesTable);
        db.execSQL(createChangesTable);
        db.execSQL(createTilesTable);
        db.execSQL(createPostTable);
        db.execSQL(createPostAttachmentTable);
        db.execSQL(createPostLinkTable);
        db.execSQL(createPostTaskTable);
    }

    //upgrade Database
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}