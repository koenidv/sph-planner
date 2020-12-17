package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.koenidv.sph.objects.Tile;

public class TilesDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static TilesDb instance;

    private TilesDb() {

    }

    public static TilesDb getInstance() {
        if (TilesDb.instance == null) {
            TilesDb.instance = new TilesDb();
        }
        return TilesDb.instance;
    }


    public List<Tile> getAllTiles() {

        List<Tile> returnList = new ArrayList<>();

        String queryString = "SELECT * FROM courses";

        SQLiteDatabase db = dbhelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                String location = cursor.getString(2);
                String type = cursor.getString(3);
                String icon = cursor.getString(4);
                int color = cursor.getInt(5);

                Tile newTile = new Tile(name, location, type, icon, color);

                returnList.add(newTile);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    /**
     * Adds or updates feature tiles in the database
     * Will override everything with the same name if it's not null
     *
     * @param tiles List of tiles to be added or updated
     */
    public void save(List<Tile> tiles) {
        for (Tile tile : tiles) {
            save(tile);
        }
    }


    public void save(Tile tile) {
        SQLiteDatabase db = DatabaseHelper.getInstance().getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("name", tile.getName());
        if (tile.getLocation() != null) cv.put("location", tile.getLocation());
        if (tile.getType() != null) cv.put("type", tile.getType());
        if (tile.getColor() != null) cv.put("color", tile.getColor());
        if (tile.getIcon() != null) cv.put("icon", tile.getIcon());


        Cursor cursor = db.rawQuery("SELECT * FROM tiles WHERE name = '" + tile.getName() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("tiles", null, cv);
        } else {
            db.update("courses", cv, "course_id = '" + tile.getName() + "'", null);
        }
        cursor.close();
    }


}
