package vogar;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import vogar.commands.Command;

/**
 * A bug database that shells out to another process.
 */
class CommandBugDatabase implements ExpectationStore.BugDatabase {
    private final Log log;
    private final String openBugsCommand;

    public CommandBugDatabase(Log log, String openBugsCommand) {
        this.log = log;
        this.openBugsCommand = openBugsCommand;
    }

    @Override public Set<Long> bugsToOpenBugs(Set<Long> bugs) {
        // query the external app for open bugs
        List<String> openBugs = new Command.Builder(log)
                .args(openBugsCommand)
                .args(bugs)
                .execute();
        Set<Long> openBugsSet = new LinkedHashSet<Long>();
        for (String bug : openBugs) {
            openBugsSet.add(Long.parseLong(bug));
        }
        return openBugsSet;
    }
}
