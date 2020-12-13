package wtf.listenia.citronDB.internal;

public class ConvertTypes {

    public static Object get (Object obj, String type) {
        switch (type) {
            case "int":
                return Integer.parseInt(obj.toString());
            default:
                return obj;
        }
    }
}
