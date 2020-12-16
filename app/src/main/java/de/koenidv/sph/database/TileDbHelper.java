package de.koenidv.sph.database;

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


}
