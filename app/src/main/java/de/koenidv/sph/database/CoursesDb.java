package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.koenidv.sph.objects.Course;


public class CoursesDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static CoursesDb instance;

    private CoursesDb() {
    }

    public static CoursesDb getInstance() {
        if (CoursesDb.instance == null) {
            CoursesDb.instance = new CoursesDb();
        }
        return CoursesDb.instance;
    }


    /**
     * Clears all courses from course db
     */
    public void clear() {
        SQLiteDatabase db = dbhelper.getWritableDatabase();
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
        SQLiteDatabase db = dbhelper.getWritableDatabase();
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
        if (course.getColor() != null) cv.put("color", course.getColor());


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
        SQLiteDatabase db = dbhelper.getWritableDatabase();
        db.execSQL("UPDATE courses SET isFavorite = 0 WHERE isFavorite IS NULL");
    }

    /**
     * get all courses in database
     *
     * @return all Courses
     */
    public List<Course> getAll() {

        List<Course> returnList = new ArrayList<>();

        String queryString = "SELECT * FROM courses";

        SQLiteDatabase db = dbhelper.getReadableDatabase();

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
                int color = cursor.getInt(9);

                Course newCourse = new Course(CourseId, gmb_id, sph_id, named_id, number_id, fullname, id_teacher, isFavorite, isLK, color);
                returnList.add(newCourse);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }


    /**
     * Get a course by its internal id
     *
     * @param courseId Internal id of the course to find
     * @return Course with internal id or null
     */
    public Course getByInternalId(String courseId) {
        if (courseId == null) return null;
        List<Course> returnList = new ArrayList<>();
        // Query course from Database
        SQLiteDatabase db = dbhelper.getReadableDatabase();
        String queryString = "SELECT * FROM courses WHERE course_id = \"" + courseId + "\"";
        Cursor cursor = db.rawQuery(queryString, null);
        // Return result or null if there's none
        if (cursor.moveToFirst()) {
            Course toReturn = cursorToCourse(cursor);
            cursor.close();
            return toReturn;
        } else {
            cursor.close();
            return null;
        }
    }

    /**
     * search for courses
     *
     * @param condition will sort for courses which match condition
     * @return List of all matching courses
     */
    public List<Course> getByInternalPrefix(String condition) {
        List<Course> returnList = new ArrayList<>();
        //Filter Course from Database
        String queryString = "SELECT * FROM courses WHERE course_id LIKE \"" + condition + "%\" ORDER BY course_id ASC";

        SQLiteDatabase db = dbhelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }

    /**
     * Get a course by named_id
     *
     * @param namedId External named id to look for
     * @return Course with specified named id or null if none was found
     */
    public List<Course> getByNamedId(String namedId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT * FROM courses WHERE named_id LIKE '" + namedId + "%'", null);

        List<Course> returnList = new ArrayList<>();
        // Return first row
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }

    /**
     * Get favorite Courses
     *
     * @return List of favorite courses in db
     */
    public List<Course> getFavorites() {
        List<Course> returnList = new ArrayList<>();
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT * FROM courses WHERE isFavorite=1", null);

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
     * Get all courses that have a number id
     *
     * @return List of all courses where number_id is not null
     */
    public List<Course> getWithNumberId() {
        List<Course> returnList = new ArrayList<>();
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT * FROM courses WHERE number_id IS NOT NULL", null);

        // Add each row to returnList
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }

    public String getNumberId(String internalId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT number_id FROM courses WHERE course_id=\"" + internalId + "\"", null);
        cursor.moveToFirst();
        String numberId = cursor.getString(0);
        cursor.close();
        return numberId;
    }

    /**
     * @param course course which should be deleted
     * @return True, if course was deleted
     */
    public boolean delete(Course course) {

        String queryString = "DELETE FROM courses WHERE course_id =" + course.getCourseId();
        SQLiteDatabase db = dbhelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }


    }

    public Course getByGmbId(String Gmb_id) {
        String queryString = "SELECT * FROM courses WHERE gmb_id = \"" + Gmb_id + "\"";
        SQLiteDatabase db = dbhelper.getReadableDatabase();

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
        int color = cursor.getInt(9);

        Course newCourse = new Course(CourseId, gmb_id, sph_id, named_id, number_id, fullname, id_teacher, isFavorite, isLK, color);

        cursor.close();

        return newCourse;
    }

    public String getCourseIdByGmbId(String gmbId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT course_id FROM courses "
                + "WHERE gmb_id = \"" + gmbId + "\"", null);
        if (cursor.moveToFirst()) {
            String id = cursor.getString(0);
            cursor.close();
            return id;
        } else {
            cursor.close();
            return null;
        }
    }

    /**
     * Returns a course id for a number id
     *
     * @param numberId SPH Number ID
     * @return Internal course id
     */
    public String getCourseIdByNumberId(String numberId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT course_id FROM courses "
                + "WHERE number_id = \"" + numberId + "\"", null);
        if (cursor.moveToFirst()) {
            String id = cursor.getString(0);
            cursor.close();
            return id;
        } else {
            cursor.close();
            return null;
        }
    }

    public Course getBySphId(String Sph_id) {
        String queryString = "SELECT * FROM courses WHERE sph_id = \"" + Sph_id + "\"";
        SQLiteDatabase db = dbhelper.getReadableDatabase();
        Course returnCourse = null;

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            returnCourse = cursorToCourse(cursor);
            cursor.close();
        }

        return returnCourse;
    }

    /**
     * Get a course's color by its internal id
     *
     * @param internalId Internal id of the course to look for
     * @return Its color als color int
     */
    public int getColor(String internalId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT color FROM courses WHERE course_id=\"" + internalId.toLowerCase() + "\"", null);
        if (!cursor.moveToFirst()) return 0;
        int color = cursor.getInt(0);
        cursor.close();
        return color;
    }

    /**
     * Set a course's color
     *
     * @param internalId The course's internal id
     * @param color      New color to set as color int
     */
    public void setColor(String internalId, int color) {
        dbhelper.getWritableDatabase().execSQL("UPDATE courses SET color=\""
                + color + "\" WHERE course_id = \"" + internalId + "\"");
    }

    /**
     * Get a course's full name by its internal id
     *
     * @param internalId Internal id of the course to look for
     * @return Its full name or its internal id if none is saved
     */
    public String getFullname(String internalId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT fullname FROM courses WHERE course_id=\"" + internalId.toLowerCase() + "\"", null);
        if (!cursor.moveToFirst()) return null;
        String name = cursor.getString(0);
        cursor.close();
        return name != null ? name : internalId;
    }

    /**
     * Check if a course is favorite
     *
     * @param internalId The course's internal id
     * @return null if the course is not in the db, true/false if it's favorite or not
     */
    public Boolean isFavorite(String internalId) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery("SELECT isFavorite FROM courses WHERE course_id=\"" + internalId.toLowerCase() + "\"", null);
        if (!cursor.moveToFirst()) return null;
        boolean favorite = cursor.getInt(0) == 1;
        cursor.close();
        return favorite;
    }


    private Course cursorToCourse(Cursor cursor) {
        String CourseId = cursor.getString(0);
        String gmb_id = cursor.getString(1);
        String sph_id = cursor.getString(2);
        String named_id = cursor.getString(3);
        String number_id = cursor.getString(4);
        String fullname = cursor.getString(5);
        String id_teacher = cursor.getString(6);
        boolean isFavorite = cursor.getInt(7) == 1;
        boolean isLK = cursor.getInt(8) == 1;
        int color = cursor.getInt(9);

        return new Course(CourseId, gmb_id, sph_id, named_id, number_id, fullname, id_teacher, isFavorite, isLK, color);
    }

}

