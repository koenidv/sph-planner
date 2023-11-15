package de.koenidv.sph.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import de.koenidv.sph.SphPlanner;

//  Created by R-Theis on 8.12.2020.
//  Adapted StKl
public class DatabaseHelper extends SQLiteOpenHelper {

    // todo close db on exit

    private static DatabaseHelper instance;

    private DatabaseHelper(@Nullable Context context) {
        super(context, "database", null, 5);
    }

    public static DatabaseHelper getInstance() {
        if (DatabaseHelper.instance == null) {
            DatabaseHelper.instance = new DatabaseHelper(SphPlanner.Companion.appContext());
        }
        return DatabaseHelper.instance;
    }

    // Create a new instance, even if one already exists
    // Used to recreate database after deletion
    public static DatabaseHelper newInstance() {
        DatabaseHelper.instance = new DatabaseHelper(SphPlanner.Companion.appContext());
        return DatabaseHelper.instance;
    }


    // On create database
    @Override
    public void onCreate(SQLiteDatabase db) {

        /*
         * Create tables
         */

        // Create schedules table
        db.execSQL("CREATE TABLE schedules(nameS TEXT UNIQUE PRIMARY KEY, startS INTEGER," +
                " endeS INTEGER, hourS TEXT, durationS INTEGER, textS TEXT," +
                " courseS TEXT, categoryS TEXT, sourceS TEXT, shareS INTEGER, locationS TEXT, respS TEXT)");
        // Create courses table
        db.execSQL("CREATE TABLE courses(course_id TEXT UNIQUE PRIMARY KEY, gmb_id TEXT," +
                " sph_id TEXT UNIQUE, named_id TEXT, number_id TEXT UNIQUE, fullname TEXT," +
                " id_teacher TEXT, isFavorite INTEGER, isLK INTEGER, color INTEGER)");
        // Create changes table
        db.execSQL("CREATE TABLE changes(change_id INTEGER UNIQUE PRIMARY KEY AUTOINCREMENT," +
                " id_course TEXT, id_course_external TEXT, date TEXT, lessons TEXT," +
                " type INTEGER, id_course_external_before TEXT, className TEXT, className_before TEXT," +
                " id_teacher TEXT, id_subsTeacher TEXT, room TEXT, room_before TEXT, description TEXT," +
                " sortLesson INTEGER)");
        // Create function tiles table
        db.execSQL("CREATE TABLE tiles(name TEXT PRIMARY KEY, " +
                " location TEXT, type TEXT, icon TEXT, color INTEGER)");
        // Create course posts table
        db.execSQL("CREATE TABLE posts(post_id TEXT PRIMARY KEY, id_course TEXT, " +
                "date INTEGER,title TEXT, description TEXT, unread INTEGER)");
        // Create file attachments table
        db.execSQL("CREATE TABLE fileAttachments(attachment_id TEXT PRIMARY KEY, " +
                "id_course TEXT, id_post TEXT, name TEXT, date INTEGER, url TEXT, size TEXT, " +
                "type TEXT, pinned INTEGER, lastUse INTEGER)");
        // Create tasks table
        db.execSQL("CREATE TABLE tasks(task_id TEXT PRIMARY KEY, id_course TEXT, id_post TEXT," +
                "description TEXT, date INTEGER, isdone INTEGER, pinned INTEGER, dueDate INTEGER)");
        // Create link attachments table
        db.execSQL("CREATE TABLE linkAttachments(attachment_id TEXT PRIMARY KEY, " +
                "id_course TEXT, id_post TEXT, name TEXT, " +
                "date INTEGER, url TEXT, pinned INTEGER, lastUse INTEGER)");
        // Create timetable table
        db.execSQL("CREATE TABLE timetable(id_course TEXT, day INTEGER, hour INTEGER, room TEXT)");
        // Create users (teachers) table
        db.execSQL("CREATE TABLE users(user_id TEXT, teacher_id TEXT, firstname TEXT, " +
                "lastname TEXT, type TEXT, pinned INTEGER)");
        // Create holidays table
        db.execSQL("CREATE TABLE holidays(id TEXT PRIMARY KEY, start INTEGER," +
                "endtime INTEGER, name TEXT, year TEXT)");
        // Create messages tables
        createMessagesTables(db);
    }

    //upgrade Database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
        if (oldversion == 1) {
            db.execSQL("ALTER TABLE postTasks RENAME TO tasks");
        }
        if (oldversion < 3) {
            // Create users (teachers) table
            db.execSQL("CREATE TABLE users(user_id TEXT, teacher_id TEXT, firstname TEXT, " +
                    "lastname TEXT, type TEXT, pinned INTEGER)");
        }
        if (oldversion < 4) {
            // Create holidays table
            db.execSQL("CREATE TABLE holidays(id TEXT PRIMARY KEY, start INTEGER," +
                    "endtime INTEGER, name TEXT, year TEXT)");
        }
        if (oldversion < 5) {
            createMessagesTables(db);
            // Create schedules table
            db.execSQL("CREATE TABLE schedules(nameS TEXT UNIQUE PRIMARY KEY, startS INTEGER," +
                    " endeS INTEGER, hourS TEXT, durationS INTEGER, textS TEXT," +
                    " courseS TEXT, categoryS TEXT, sourceS TEXT, shareS INTEGER, locationS TEXT, respS TEXT)");
            // Create timebar table
            db.execSQL("CREATE TABLE timebar(someinfo TEXT)");
        }
    }

    // Create conversations and messages tables
    private void createMessagesTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE conversations (" +
                "conversation_id TEXT PRIMARY KEY," +
                "first_id_message TEXT," +
                "subject TEXT," +
                "recipient_count INTEGER," +
                "answertype TEXT," +
                "original_id_sender TEXT," +
                "lastdate INTEGER," +
                "unread INTEGER," +
                "archived INTEGER DEFAULT 0);");
        db.execSQL("CREATE TABLE messages (" +
                "message_id TEXT PRIMARY KEY," +
                "id_conversation TEXT," +
                "id_sender TEXT," +
                "sendername TEXT," +
                "sendertype TEXT," +
                "date INTEGER," +
                "subject TEXT," +
                "content TEXT," +
                "unread INTEGER," +
                "recipients TEXT," +
                "recipientsCount INTEGER);");
    }

    /**
     * Delete ALL entries from ALL tables
     */
    public void deleteAll() {
        // List of tables to be deleted
        String[] tables = {"changes", "conversations", "courses", "fileAttachments", "holidays",
                "linkAttachments", "messages", "posts", "tasks", "tiles", "timetable", "users"};
        SQLiteDatabase db = getWritableDatabase();

        // Delete each table
        for (String table : tables) {
            db.delete(table, null, null);
        }
    }
}