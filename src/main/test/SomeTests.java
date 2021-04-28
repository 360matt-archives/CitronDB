import fr.i360matt.citronDB.api.Database;
import fr.i360matt.citronDB.api.annotations.Size;
import fr.i360matt.citronDB.api.annotations.Unique;
import fr.i360matt.citronDB.api.builders.RowBuilder;
import fr.i360matt.citronDB.api.TableManager;
import fr.i360matt.citronDB.utils.DebugTime;

import java.util.ArrayList;
import java.util.Arrays;

public class SomeTests {

    public static class ExampleStucture {
        @Unique(size = 50)
        public String test = "truc";
        public String truc = "jaaj";
        @Size(size = 20)
        public int caillou = 0;
    }

    public static void main (final String[] args) throws NoSuchFieldException {

        Database db2 = new Database() {{
            setHost("freedb.tech");
            setPort(3306);
            setDbName("freedbtech_souris");
            setUsername("freedbtech_hey");
            setPassword("cmabite");
            connect();
        }};

        TableManager<ExampleStucture> tableManager = db2.getTable("wsh", ExampleStucture.class);

        DebugTime debugTime = new DebugTime();
        debugTime.start();

        tableManager.createTable(true);
        // will create the table, or if existe, update these.

        debugTime.printElapsed();

        RowBuilder line = new RowBuilder() {{
            define("test", 100);
            define("truc", "20~~é-|--éé~~##");
        }};

        RowBuilder replacement = new RowBuilder() {{
            define("test", "caillou");
        }};


        tableManager.update(line, replacement);



    }

}
