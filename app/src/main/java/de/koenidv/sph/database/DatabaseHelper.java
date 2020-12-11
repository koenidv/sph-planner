package de.koenidv.sph.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.koenidv.sph.objects.Course;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(@Nullable Context context) {
        super(context, "database", null, 1);
    }

    //create Tables for Database
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCoursesTable = "CREATE TABLE COURSES(  course_id TEXT UNIQUE PRIMARY KEY, gmb_id TEXT UNIQUE, sph_id TEXT UNIQUE, named_id TEXT UNIQUE, number_id TEXT UNIQUE, fullname TEXT, id_Teacher TEXT, isFavorite BOOL, isLK Bool)";
        String createChangesTable = "";

        db.execSQL(createCoursesTable);

    }

    //upgrade Database
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    //add to Database
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

        long insert=db.insert("COURSES", null, cv);
        return insert != -1;
    }
    public List<Course> getAll(){

        List<Course> returnList = new ArrayList<>();
        //get Data from Database
        String queryString = "SELECT * FROM COURSES";

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
}