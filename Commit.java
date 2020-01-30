package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Commit implements Serializable {

    private String commitMessage;

    private String commitTimestamp;

    private HashMap<String, String> commitfiles;

    private String[] commitParents;

    private String commitID;
    public String[] getParents() {
        return commitParents;
    }

    public String getParentID() {
        if (commitParents != null) {
            return commitParents[0];
        }
        return null;
    }

    public String getTimestamp() {
        return commitTimestamp;
    }

    public HashMap<String, String> getFiles() {
        return commitfiles;
    }

    public String getMessage() {
        return commitMessage;
    }


    public Commit(String message, HashMap files, String[] parents) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        commitMessage = message;
        commitfiles = files;
        commitParents = parents;
        commitTimestamp = formatter.format(date);
        commitID = hashCommit();
    }
    public Commit(String message) {
        commitMessage = message;
        commitfiles = null;
        commitParents = null;
        commitTimestamp = "Wed Dec 31 16:00:00 1969 -0800";
        commitID = hashCommit();
    }



    public String hashCommit() {
        return Utils.sha1(commitMessage, commitTimestamp);
    }

    public String getCommitID() {
        return commitID;
    }


}
