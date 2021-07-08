import fr.i360matt.citronDB.api.Database;
import fr.i360matt.citronDB.api.annotations.Size;
import fr.i360matt.citronDB.api.annotations.Unique;
import fr.i360matt.citronDB.api.TableManager;

public class SomeTests {

    public static class ExampleStucture {
        @Unique()
        @Size
        public String test = "truc";
        public String truc = "jaaj";
    }

    public static void main (final String[] args) {

        Database db2 = new Database() {{
            setHost("127.0.0.1");
            setPort(3306);
            setDbName("social");
            setUsername("root");
            setPassword("");
            connect();
        }};




        /*
        ExampleStucture ah = new ExampleStucture();
        ah.caillou = 56;

        tableManager.insert(ah);

         */



        TableManager<ExampleStucture> tableManager = db2.getTable("uneTable", ExampleStucture.class);

        tableManager.createTable(true);
        // will create the table, or if existe, update these.

        DebugTime debugTime = new DebugTime();
        debugTime.start();

        for (int i = 0; i < 100_000; i++) {
            ExampleStucture inst = new ExampleStucture();
            inst.test = "Woaaw";

            tableManager.insert(inst);
        }

        debugTime.printElapsed();







    }

}
