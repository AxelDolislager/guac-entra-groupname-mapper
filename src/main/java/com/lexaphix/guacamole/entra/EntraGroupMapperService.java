public class EntraGroupMappingService {

    private final GraphClient graph = new GraphClient();

    public Set<String> mapGuidsToNames(Set<String> guids) {
        Set<String> names = new HashSet<>();
        for (String guid : guids) {
            String name = graph.getGroupName(guid);
            if (name != null)
                names.add(name);
        }
        return names;
    }
}
