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


    // On create database
    @Override
    public void onCreate(SQLiteDatabase db) {

        /*
         * Create tables
         */

        // Create courses table
        db.execSQL("CREATE TABLE courses(course_id TEXT UNIQUE PRIMARY KEY, gmb_id TEXT UNIQUE," +
                " sph_id TEXT UNIQUE, named_id TEXT UNIQUE, number_id TEXT UNIQUE, fullname TEXT," +
                " id_teacher TEXT, isFavorite INTEGER, isLK INTEGER, color INTEGER)");
        // Create changes table
        db.execSQL("CREATE TABLE changes(change_id INTEGER UNIQUE PRIMARY KEY AUTOINCREMENT," +
                " id_course TEXT, id_course_external TEXT, date TEXT, lessons TEXT," +
                " type INTEGER, id_course_external_before TEXT, className TEXT, className_before TEXT," +
                " id_teacher TEXT, id_subsTeacher TEXT, room TEXT, room_before TEXT, description TEXT," +
                " sortLesson INTEGER)");
        // Create function tiles table
        db.execSQL("CREATE TABLE tiles(name TEXT PRIMARY KEY," +
                " location TEXT, type TEXT, icon TEXT, color INTEGER)");
        // Create course posts table
        db.execSQL("CREATE TABLE posts(post_id TEXT PRIMARY KEY, id_course TEXT," +
                "date INTEGER,title TEXT, description TEXT, unread INTEGER)");
        // Create file attachments table
        db.execSQL("CREATE TABLE fileAttachments(attachment_id TEXT PRIMARY KEY," +
                "id_course TEXT, id_post TEXT, name TEXT, date INTEGER, url TEXT, size TEXT," +
                "type TEXT, pinned INTEGER, lastUse INTEGER)");
        // Create post tasks table
        db.execSQL("CREATE TABLE postTasks(task_id TEXT PRIMARY KEY, id_course TEXT, id_post TEXT," +
                "description TEXT, date INTEGER, isdone INTEGER, dueDate INTEGER)");
        // Create link attachments table
        db.execSQL("CREATE TABLE linkAttachments(attachment_id TEXT PRIMARY KEY," +
                "id_course TEXT, id_post TEXT, name TEXT," +
                "date INTEGER, url TEXT, pinned INTEGER, lastUse INTEGER)");
        // Create timetable table
        db.execSQL("CREATE TABLE timetable(id_course TEXT, day INTEGER, hour INTEGER, room TEXT)");
    }

    //upgrade Database
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldversion, int newversion) {

    }
}