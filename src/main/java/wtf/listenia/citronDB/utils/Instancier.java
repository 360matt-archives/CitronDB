package wtf.listenia.citronDB.utils;

public class Instancier {
    /**
     * Allows to recover the default data of the chosen structure
     * @param structure chosen structure
     * @param <D> the type of structure
     * @return the default data of the structure (instance)
     */
    public static <D> D getEmptyRaw (final Class<D> structure) {
        if (!ExpirableCache.types.containsKey(structure)) {
            try {
                final D res = structure.newInstance();
                ExpirableCache.types.put(structure, res);
                return res;
            } catch (InstantiationException | IllegalAccessException e) {
                return null;
            }
        }
        return (D) ExpirableCache.types.get(structure);
    }
}
