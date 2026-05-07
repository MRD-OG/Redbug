package mc.md5g22.redbug.objects.test;

import mc.md5g22.redbug.objects.monitor.MonitorManager;
import mc.md5g22.redbug.plugin.Redbug;
import mc.md5g22.redbug.objects.node.NodeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;

public class TestManager {

    HashMap<UUID, HashMap<String, Test>> playerTests = new HashMap<>();

    private Redbug plugin;
    private NodeManager nodeManager;
    private MonitorManager monitorManager;

    private static TestManager testManager;

    public static TestManager getInstance(Redbug plugin) {
        if (testManager == null) {
            testManager = new TestManager(plugin);
        }

        return testManager;
    }

    private TestManager(Redbug plugin) {
        this.plugin = plugin;
    }

    public void setNodeManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void setMonitorManager(MonitorManager monitorManager) {
        this.monitorManager = monitorManager;
    }

    public void start() {}

    public void stop() {}

    public Collection<HashMap<String, Test>> getAllTests() {
        return playerTests.values();
    }

    public Set<String> getPlayerTests(Player player) {

        UUID uuid = player.getUniqueId();

        if (!playerTests.containsKey(uuid)) {
            return new HashSet<>();
        }

        return playerTests.get(uuid).keySet();
    }

    public boolean playerHasTest(Player player, String testName) {
        return getPlayerTests(player).contains(testName);
    }

    public boolean addTest(Player player, String testName, HashMap<String, List<String>> csvData) {
        if (playerHasTest(player, testName))
            return false;

        HashMap<String, Test> tests = playerTests.get(player.getUniqueId());

        if (tests == null) {
            tests = new HashMap<>();
        }

        tests.put(testName, new Test(plugin, testName, csvData));
        playerTests.put(player.getUniqueId(), tests);
        return true;

    }

    public boolean removeTest(Player player, String testName) {
        if (!playerHasTest(player, testName))
            return false;

        playerTests.get(player.getUniqueId()).remove(testName);
        return true;
    }

    public boolean deleteAllTests(Player player) {
        for (String testName : getPlayerTests(player)) {
            removeTest(player, testName);
        }
        return true;
    }

    public boolean startTest(Player player, String testName, String monitorName, String clkPinName, int clkPeriod) {//, int clkOffset) {
        if (!playerHasTest(player, testName)) {
            player.sendMessage(Component.text("[RB]: Test " + testName + " does not exist", NamedTextColor.RED));
            return false;
        }
        Test test = playerTests.get(player.getUniqueId()).get(testName);

        if (test.isTestRunning()) {
            player.sendMessage(Component.text("[RB]: Test " + testName + " is already running", NamedTextColor.RED));
            return false;
        }

        test.start(player, monitorName, clkPinName, clkPeriod);//, clkOffset);
        return true;
    }

    public boolean stopTest(Player player, String testName) {
        if (!playerHasTest(player, testName)) {
            return false;
        }

        playerTests.get(player.getUniqueId()).get(testName).stop(player);
        return true;
    }

    public boolean printTestInfo(Player player, String testName) {
        if (!playerHasTest(player, testName)) {
            return false;
        }

        playerTests.get(player.getUniqueId()).get(testName).printInfo(player);
        return true;
    }
}
