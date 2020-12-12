package de.koenidv.sph.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import de.koenidv.sph.objects.Course;

//  Created by R-Theis on 8.12.2020.
public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(@Nullable Context context) {
        super(context, "database", null, 1);
    }


    // todo use singleton
    // todo split into meaningful classes
    // todo save List<Course>
    // todo save: update instead of insert if dataset already exists


    //create Tables for Database
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCoursesTable = "CREATE TABLE courses(  course_id TEXT UNIQUE PRIMARY KEY, gmb_id TEXT UNIQUE, sph_id TEXT UNIQUE, named_id TEXT UNIQUE, number_id TEXT UNIQUE, fullname TEXT, id_Teacher TEXT, isFavorite BOOL, isLK Bool)";
        String createChangesTable = "";

        db.execSQL(createCoursesTable);
    }

    //upgrade Database
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    /**
     * Clears all courses from course db
     */
    public void clear() {
        SQLiteDatabase  db = this.getWritableDatabase();
        //db.rawQuery("DELETE FROM courses", null).close();
        db.delete("courses", null, null);
    }

    /**
     * @param course course which should be added
     * @return True, if course was added
     */
    public boolean addCourse(Course course) {
        SQLiteDatabase  db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("course_id",course.getCourseId());
        cv.put("gmb_id",course.getGmb_id());
        cv.put("sph_id",course.getSph_id());
        cv.put("named_id",course.getNamed_id());
        cv.put("number_id",course.getNumber_id());
        cv.put("fullname",course.getFullname());
        cv.put("id_Teacher",course.getId_teacher());
        cv.put("isFavorite",course.isFavorite());
        cv.put("isLK",course.isLK());

        long insert=db.insert("courses", null, cv);
        return insert != -1;
    }

    /**
     * get all courses in database
     * @return all Courses
     */
    public List<Course> getAllCourses(){

        List<Course> returnList = new ArrayList<>();

        String queryString = "SELECT * FROM courses";

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor= db.rawQuery(queryString,null);
        if(cursor.moveToFirst()) {
            do{
                String CourseId = cursor.getString(0);
                String gmb_id = cursor.getString(1);
                String sph_id = cursor.getString(2);
                String named_id = cursor.getString(3);
                String number_id = cursor.getString(4);
                String fullname = cursor.getString(5);
                String id_teacher = cursor.getString(6);
                boolean isFavorite = cursor.getInt(7) == 1;
                boolean isLK = cursor.getInt(8) == 1;

                Course newCourse = new Course(CourseId,gmb_id,sph_id,named_id,number_id,fullname,id_teacher,isFavorite,isLK);
                returnList.add(newCourse);
            }while(cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }
    /** search for courses
     * @param condition will sort for courses which match condition
     * @return List of all matching courses
     */
    public List<Course> getCourseByInternalPrefix(String condition){
        List<Course> returnList = new ArrayList<>();
        //Filter Course from Database
        String queryString = "SELECT * FROM courses WHERE course_id LIKE \"" + condition + "%\" ORDER BY course_id ASC";

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor= db.rawQuery(queryString,null);
        if(cursor.moveToFirst()) {
            do{
                //convert cursor to List<Course>
                String CourseId = cursor.getString(0);
                String gmb_id = cursor.getString(1);
                String sph_id = cursor.getString(2);
                String named_id = cursor.getString(3);
                String number_id = cursor.getString(4);
                String fullname = cursor.getString(5);
                String id_teacher = cursor.getString(6);
                boolean isFavorite = cursor.getInt(7) == 1;
                boolean isLK = cursor.getInt(8) == 1;

                Course newCourse = new Course(CourseId,gmb_id,sph_id,named_id,number_id,fullname,id_teacher,isFavorite,isLK);
                returnList.add(newCourse);
            }while(cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    /**
     * @param course course which should be deleted
     * @return True, if course was deleted
     */
    public boolean deleteCourse(Course course){

        String queryString = "DELETE FROM courses WHERE course_id ="+ course.getCourseId();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor =db.rawQuery(queryString,null);

        if(cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return true;
        }else{
            cursor.close();
            db.close();
            return false;
        }


    }

    public Course getCourseByGmb_id(String Gmb_id){
        String queryString = "SELECT * FROM courses WHERE gmb_id = "+ Gmb_id;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor =db.rawQuery(queryString,null);
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

        Course newCourse = new Course(CourseId,gmb_id,sph_id,named_id,number_id,fullname,id_teacher,isFavorite,isLK);

        cursor.close();
        db.close();

        return newCourse;
    }
}