package wtf.listenia.citronDB.utils;

public class Instancier {
    /**
     * Allows to recover the default data of the chosen structure
     * @param structure chosen structure
     * @param <D> the type of structure
     * @return the default data of the structure (instance)
     */
    public static <D> D createInstance (final Class<D> structure) {
        try {
            return structure.newInstance();
        } catch (final IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }

}
