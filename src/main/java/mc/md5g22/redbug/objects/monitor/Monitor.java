package mc.md5g22.redbug.objects.monitor;

import java.util.ArrayList;

public class Monitor {

    private final ArrayList<String> monitorSources = new ArrayList<>();

    private final String name;

    public Monitor(String name) {
        this.name = name;
    }

    public void removeMonitorSource(String nodeName) {
        monitorSources.remove(nodeName);
    }

    public boolean addMonitorSource(String nodeName) {
        if (hasMonitorSource(nodeName)) {
            return false;
        }

        return monitorSources.add(nodeName);
    }

    public boolean hasMonitorSource(String nodeName) {
        return monitorSources.contains(nodeName);
    }

    public ArrayList<String> getMonitorSources() {
        return monitorSources;
    }

    public void clearMonitors() {
        monitorSources.clear();
    }

    public String getName() {
        return name;
    }
}
