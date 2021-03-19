import wtf.listenia.citronDB.api.Database;
import wtf.listenia.citronDB.api.annotations.Unique;
import wtf.listenia.citronDB.api.builders.RowBuilder;
import wtf.listenia.citronDB.api.TableManager;

public class SomeTests {

    public static class ExampleStucture {
        @Unique(size = 50)
        public String test = "truc";
        public String truc = "jaaj";
        public int caillou = 8;
    }

    public static void main (final String[] args) {

        Database db2 = new Database() {{
            setHost("sql11.freesqldatabase.com");
            setPort(3306);
            setDbName("sql11398741");
            setUsername("sql11398741");
            setPassword("sTQFvgEqLW");
            connect();
        }};

        TableManager<ExampleStucture> tableManager = db2.getTable("wsh", ExampleStucture.class);

        tableManager.updateStructure();
        // will update the table schema (add new columns (from ExpirableCache field), et remove deprecated columns)


        tableManager.createTable();
        // will create the table, or if existe, update these.


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
