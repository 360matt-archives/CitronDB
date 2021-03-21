import wtf.listenia.citronDB.api.Database;
import wtf.listenia.citronDB.api.annotations.Size;
import wtf.listenia.citronDB.api.annotations.Unique;
import wtf.listenia.citronDB.api.builders.RowBuilder;
import wtf.listenia.citronDB.api.TableManager;
import wtf.listenia.citronDB.utils.DebugTime;

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
