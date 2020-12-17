package de.koenidv.sph.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

import de.koenidv.sph.MainActivity;
import de.koenidv.sph.SphPlanner;
import de.koenidv.sph.objects.Course;

//  Created by R-Theis on 8.12.2020.
public class DatabaseHelper extends SQLiteOpenHelper {

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


    //create Tables for Database
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCoursesTable = "CREATE TABLE courses(course_id TEXT UNIQUE PRIMARY KEY, gmb_id TEXT UNIQUE," +
                " sph_id TEXT UNIQUE, named_id TEXT UNIQUE, number_id TEXT UNIQUE, fullname TEXT," +
                " id_teacher TEXT, isFavorite INTEGER, isLK INTEGER)";
        String createChangesTable = "CREATE TABLE changes( change_id TEXT UNIQUE PRIMARY KEY," +
                " id_course TEXT UNIQUE, id_course_external TEXT UNIQUE, date TEXT , lessons TEXT," +
                " type TEXT, id_course_external_before TEXT, className TEXT, className_before TEXT," +
                " id_teacher TEXT, id_subsTeacher TEXT, room TEXT, room_before TEXT, description TEXT)";
        String createTilesTable = "CREATE TABLE Tiles( name TEXT PRIMARY KEY," +
                " location TEXT, type TEXT, icon TEXT, color TEXT )";

        db.execSQL(createCoursesTable);
        db.execSQL(createChangesTable);
        db.execSQL(createTilesTable);
    }

    //upgrade Database
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    /**
     * Clears all courses from course db
     */
    public void clear() {
        SQLiteDatabase db = this.getWritableDatabase();
        //db.rawQuery("DELETE FROM courses", null).close();
        db.delete("courses", null, null);
    }

    /**
     * Adds or updates courses in the database
     * Will override everything for the same course_id if it's not null
     *
     * @param courses List of courses to be added or updated
     */
    public void save(List<Course> courses) {
        for (Course course : courses) {
            save(course);
        }
    }

    /**
     * Adds or updates a course in the database
     * Will override everything for the same course_id if it's not null
     *
     * @param course course to be added or updated
     */
    public void save(Course course) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        // Only use values that are non-null
        cv.put("course_id", course.getCourseId());
        if (course.getGmb_id() != null) cv.put("gmb_id", course.getGmb_id());
        if (course.getSph_id() != null) cv.put("sph_id", course.getSph_id());
        if (course.getNamed_id() != null) cv.put("named_id", course.getNamed_id());
        if (course.getNumber_id() != null) cv.put("number_id", course.getNumber_id());
        if (course.getFullname() != null) cv.put("fullname", course.getFullname());
        cv.put("id_teacher", course.getId_teacher()); // Will never be null
        if (course.isFavorite() != null) cv.put("isFavorite", course.isFavorite());
        if (course.isLK() != null) cv.put("isLK", course.isLK());


        /* Aaaah nevermind

        StringBuilder keys = new StringBuilder();
        StringBuilder createValues = new StringBuilder();
        StringBuilder updateValues = new StringBuilder();

        String prefix = "";
        for (String key : cv.keySet()) {
            keys.append(prefix).append(key);
            createValues.append(prefix).append("'").append(cv.get(key)).append("'");
            updateValues.append(prefix).append(key).append("='").append(cv.get(key)).append("'");
            prefix = ",";
        }

        // Create row or ignore if it already exists
        db.rawQuery("INSERT OR IGNORE INTO courses (" + keys.toString() + ") VALUES (" + createValues.toString() + ")", null);
        // Update row with our data
        db.rawQuery("UPDATE courses SET " + updateValues.toString() + " WHERE course_id='" + course.getCourseId() + "'", null);
         */


        // Check if row exists and insert or update accordingly
        Cursor cursor = db.rawQuery("SELECT * FROM courses WHERE course_id = '" + course.getCourseId() + "'", null);
        if (cursor.getCount() == 0)
            db.insert("courses", null, cv);
        else
            db.update("courses", cv, "course_id = '" + course.getCourseId() + "'", null);
        cursor.close();
    }

    /**
     * This will set all courses with isFavorite = null in the db to isFavorite = false
     * Used in course indexing where we know which courses are favorites, but not which are not
     */
    public void setNulledNotFavorite() {
        this.getReadableDatabase().execSQL("UPDATE courses SET isFavorite = 0 WHERE isFavorite IS NULL");
    }

    /**
     * get all courses in database
     *
     * @return all Courses
     */
    public List<Course> getAllCourses() {

        List<Course> returnList = new ArrayList<>();

        String queryString = "SELECT * FROM courses";

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String CourseId = cursor.getString(0);
                String gmb_id = cursor.getString(1);
                String sph_id = cursor.getString(2);
                String named_id = cursor.getString(3);
                String number_id = cursor.getString(4);
                String fullname = cursor.getString(5);
                String id_teacher = cursor.getString(6);
                boolean isFavorite = cursor.getInt(7) == 1;
                boolean isLK = cursor.getInt(8) == 1;

                Course newCourse = new Course(CourseId, gmb_id, sph_id, named_id, number_id, fullname, id_teacher, isFavorite, isLK);
                returnList.add(newCourse);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    /**
     * search for courses
     *
     * @param condition will sort for courses which match condition
     * @return List of all matching courses
     */
    public List<Course> getCourseByInternalPrefix(String condition) {
        List<Course> returnList = new ArrayList<>();
        //Filter Course from Database
        String queryString = "SELECT * FROM courses WHERE course_id LIKE \"" + condition + "%\" ORDER BY course_id ASC";

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    /**
     * Get a course by named_id
     *
     * @param namedId External named id to look for
     * @return Course with specified named id or null if none was found
     */
    public Course getCourseByNamedId(String namedId) {
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM courses WHERE named_id='" + namedId + "'", null);
        // Return first row
        cursor.moveToFirst();
        if (cursor.getCount() == 0) return null;

        return cursorToCourse(cursor);

    }

    /**
     * Get favorite Courses
     *
     * @return List of favorite courses in db
     */
    public List<Course> getFavoriteCourses() {
        List<Course> returnList = new ArrayList<>();
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM courses WHERE isFavorite=1", null);

        // Add each row to returnList
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;

    }

    /**
     * @param course course which should be deleted
     * @return True, if course was deleted
     */
    public boolean deleteCourse(Course course) {

        String queryString = "DELETE FROM courses WHERE course_id =" + course.getCourseId();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return true;
        } else {
            cursor.close();
            db.close();
            return false;
        }


    }

    public Course getCourseByGmb_id(String Gmb_id) {
        String queryString = "SELECT * FROM courses WHERE gmb_id = " + Gmb_id;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        cursor.moveToFirst();

        String CourseId = cursor.getString(0);
        String gmb_id = cursor.getString(1);
        String sph_id = cursor.getString(2);
        String named_id = cursor.getString(3);
        String number_id = cursor.getString(4);
        String fullname = cursor.getString(5);
        String id_teacher = cursor.getString(6);
        boolean isFavorite = cursor.getInt(7) == 1;
        boolean isLK = cursor.getInt(8) == 1;

        Course newCourse = new Course(CourseId, gmb_id, sph_id, named_id, number_id, fullname, id_teacher, isFavorite, isLK);

        cursor.close();
        db.close();

        return newCourse;
    }


    Course cursorToCourse(Cursor cursor) {
        String CourseId = cursor.getString(0);
        String gmb_id = cursor.getString(1);
        String sph_id = cursor.getString(2);
        String named_id = cursor.getString(3);
        String number_id = cursor.getString(4);
        String fullname = cursor.getString(5);
        String id_teacher = cursor.getString(6);
        boolean isFavorite = cursor.getInt(7) == 1;
        boolean isLK = cursor.getInt(8) == 1;

        return new Course(CourseId, gmb_id, sph_id, named_id, number_id, fullname, id_teacher, isFavorite, isLK);
    }
}