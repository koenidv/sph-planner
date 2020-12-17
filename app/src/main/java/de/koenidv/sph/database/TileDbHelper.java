package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.koenidv.sph.objects.Course;
import de.koenidv.sph.objects.Tile;

public class TileDbHelper {

    private DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static TileDbHelper instance;

    private TileDbHelper() {

    }

    public static TileDbHelper getInstance() {
        if (TileDbHelper.instance == null) {
            TileDbHelper.instance = new TileDbHelper();
        }
        return TileDbHelper.instance;
    }




    public List<Tile> getAllTiles() {

        List<Tile> returnList = new ArrayList<>();

        String queryString = "SELECT * FROM courses";

        SQLiteDatabase db = dbhelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                String location= cursor.getString(2);
                String type= cursor.getString(3);
                String icon= cursor.getString(4);
                String color = cursor.getString(5);

                Tile newTile= new Tile(name,location,type,icon,color);

                returnList.add(newTile);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    public void save(Tile tile){
        SQLiteDatabase db = DatabaseHelper.getInstance().getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("name", tile.getName());
        if (tile.getColor() != null) cv.put("gmb_id", tile.getColor());
        if (tile.getIcon() != null) cv.put("icon", tile.getIcon());
        if (tile.getLocation() != null) cv.put("location", tile.getLocation());
        if (tile.getType()!= null) cv.put("type", tile.getType());



        Cursor cursor =db.rawQuery("SELECT * FROM Tiles WHERE name = '" + tile.getName() + "'", null);
        if(cursor.getCount()==0){
            db.insert("Tiles", null, cv);
        }else{
            db.update("courses", cv, "course_id = '" + tile.getName() + "'", null);
        }
        cursor.close();
    }


}
