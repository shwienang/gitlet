package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class BlobTree implements Serializable {

    private HashMap<String, String> treeBranche;

    private String header;

    private HashMap<String, String> stagingFiles;

    private ArrayList<String> untrackedFiles;



    public String getHeader() {
        return treeBranche.get(header);
    }


    public BlobTree() {
        header = "master";
        treeBranche = new HashMap<String, String>();
        stagingFiles = new HashMap<String, String>();
        untrackedFiles = new ArrayList<String>();

        genGitletFile();
        Commit initCommit = new Commit("initial commit");
        String id = initCommit.getCommitID();
        File initialFile = new File(Constant.Commit + id);

        Utils.writeContents(initialFile, Utils.serialize(initCommit));
        treeBranche.put("master", initCommit.getCommitID());
    }

    public void genGitletFile(){
        File gitlet = new File(Constant.GitLet);
        gitlet.mkdir();
        File commits = new File(Constant.Commit);
        commits.mkdir();
        File staging = new File(Constant.Stag);
        staging.mkdir();
    }


    public void add(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            throw new GitletException();
        }

        Commit recentCommit = getCommit(getHeader());

        HashMap<String, String> commitFiles = recentCommit.getFiles();

        String hash = Utils.sha1(Utils.readContentsAsString(file));

        File stagingBlob = genFile(".gitlet/staging/" + hash);

        if (commitFiles == null || !commitFiles.containsKey(fileName) || !commitFiles.get(fileName).equals(hash)  ) {

            stagingFiles.put(fileName, hash);
            String fileContents = Utils.readContentsAsString(file);
            Utils.writeContents(stagingBlob, fileContents);
        } else {

            if (stagingBlob.exists()) {
                stagingFiles.remove(fileName);
            }
        }

        if (untrackedFiles.contains(fileName)) {
            untrackedFiles.remove(fileName);
        }
    }

    public File genFile(String fileName){
        return new File(fileName);
    }

    public void commit(String message) {
        if (message.trim().equals("")) {
            System.out.println("Please enter a commit message.");
            throw new GitletException();
        }

        Commit lastCommit = getCommit(getHeader());

        HashMap<String, String> lastCommitFiles = lastCommit.getFiles();

        if (lastCommitFiles == null) {
            lastCommitFiles = new HashMap<String, String>();
        }


        if (stagingFiles.size() != 0 || untrackedFiles.size() != 0) {
            for (String fileName : stagingFiles.keySet()) {
                lastCommitFiles.put(fileName, stagingFiles.get(fileName));
            }
            for (String fileName : untrackedFiles) {
                lastCommitFiles.remove(fileName);
            }
        } else {
            System.out.println("No changes added to the commit.");
            throw new GitletException();
        }
        String[] parent = new String[]{lastCommit.getCommitID()};
        Commit newCommit = new Commit(message, lastCommitFiles, parent);
        String s = newCommit.getCommitID();
        File newCommFile = new File(Constant.Commit + s);
        Utils.writeObject(newCommFile, newCommit);

        stagingFiles = new HashMap<String, String>();
        untrackedFiles = new ArrayList<String>();

        treeBranche.put(header, newCommit.getCommitID());
    }


    public void commit(String msg, String[] parents) {
        if (msg.trim().equals("")) {
            System.out.println("Please enter a commit message.");
            throw new GitletException();
        }
        Commit mostRecent = getCommit(getHeader());
        HashMap<String, String> trackedFiles = mostRecent.getFiles();

        if (trackedFiles == null) {
            trackedFiles = new HashMap<String, String>();
        }

        if (stagingFiles.size() != 0 || untrackedFiles.size() != 0) {
            for (String file : stagingFiles.keySet()) {
                trackedFiles.put(file, stagingFiles.get(file));
            }
            for (String file : untrackedFiles) {
                trackedFiles.remove(file);
            }
        }

        Commit genCommit = new Commit(msg, trackedFiles, parents);
        String commitID = genCommit.getCommitID();
        File genCommitFile = new File(Constant.Commit + commitID);
        Utils.writeObject(genCommitFile, genCommit);

        untrackedFiles = new ArrayList<String>();
        stagingFiles = new HashMap<String, String>();
        treeBranche.put(header, genCommit.getCommitID());
    }



    public void logCommits() {

        String header = getHeader();
        while (header != null) {
            Commit headerCommit = getCommit(header);

            commitInfo(header).forEach(a -> {
                System.out.println(a);
            });
            System.out.println();

            header = headerCommit.getParentID();
        }
    }


    public ArrayList<String> commitInfo(String commitId) {
        Commit comm = getCommit(commitId);
        ArrayList<String> aL = new ArrayList<String>();

        if (comm.getParents() != null && comm.getParents().length > 1) {
            aL.add("===");
            aL.add("commit " + commitId);
            String short1 = comm.getParents()[0].substring(0, 7);
            String short2 = comm.getParents()[1].substring(0, 7);
            aL.add("Merge: " + short1 + " " + short2);
            aL.add("Date: " + comm.getTimestamp());
            aL.add(comm.getMessage());
        } else {
            aL.add("===");
            aL.add("commit " + commitId);
            aL.add("Date: " + comm.getTimestamp());
            aL.add(comm.getMessage());
        }
        return aL;
    }


    public void rm(String rmFile) {

        File file = new File(rmFile);

        Commit lastCommit = getCommit(getHeader());

        HashMap<String, String> lastCommitFiles = lastCommit.getFiles();
        if (!file.exists() && !lastCommitFiles.containsKey(rmFile)) {
            System.out.println("File does not exist.");
            throw new GitletException();
        }
        int c = 1;

        if (stagingFiles.containsKey(rmFile)) {
            stagingFiles.remove(rmFile);
            c = 0;
         }

        if (lastCommitFiles != null && lastCommitFiles.containsKey(rmFile)) {
            untrackedFiles.add(rmFile);

            Utils.restrictedDelete( new File(rmFile));
            c = 0;
         }

        if (c == 1) {
            System.out.println("No reason to remove the file.");
            throw new GitletException();
        }
    }


    public void globalLog() {
        File commitFolder = new File(".gitlet/commits");
        File[] commits = commitFolder.listFiles();

        if(commits != null)
            for( int i = 0;i < commits.length;i++){
                File file = commits[i];
                commitInfo(file.getName()).forEach(a -> {
                    System.out.println(a);
                });
                System.out.println();

            }
    }

    public void find(String message) {

        File commitDir = new File(Constant.Commit);
        File[] commitFiles = commitDir.listFiles();
        int f = 0;

        for(int i = 0;i < commitFiles.length ; i++ ){
            File file = commitFiles[i];
            Commit comm = getCommit(file.getName());
            String commitMeg = comm.getMessage();
            if (message.equals(commitMeg)) {
                System.out.println(file.getName());
                f = 1;
            }
        }

        if (f == 0) {
            System.out.println("Found no commit with that message.");
            throw new GitletException();
        }
    }


    public void status() {
        System.out.println("=== Branches ===");
        Set<String> treeBrSet = treeBranche.keySet();
        Iterator<String> treeBrSetIt = treeBrSet.iterator();

        while(treeBrSetIt.hasNext()){
            String branchName = treeBrSetIt.next();
            if (branchName.equals(header)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }

        System.out.println();
        System.out.println("=== Staged Files ===");

        Set<String> stageSet = stagingFiles.keySet();
        Iterator<String> stageSetIt = stageSet.iterator();
        while(stageSetIt.hasNext()){
            System.out.println(stageSetIt.next());
        }

        System.out.println();
        System.out.println("=== Removed Files ===");

        ArrayList<String> rmFiles = untrackedFiles;
        if(untrackedFiles != null) {
            for (int i = 0; i < untrackedFiles.size(); i++) {
                String tem = untrackedFiles.get(i);
                System.out.println(tem);
            }
        }

        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }



    public void checkout(String[] actionCommand) {
        String commID = "";
        String fileName = "";
        if(actionCommand.length == 1){
            checkout(actionCommand[0]);
            return ;
        } else if (actionCommand.length == 2 && actionCommand[0].equals("--")) {

            fileName = actionCommand[1];

            commID = getHeader();
        } else if (actionCommand.length == 3 && actionCommand[1].equals("--")) {
            commID = actionCommand[0];
            fileName = actionCommand[2];
        } else {
            System.out.println("Incorrect operands");
            throw new GitletException();

        }

        commID = shortId2CommitId(commID);

        Commit comm = getCommit(commID);

        HashMap<String, String> trackedFiles = comm.getFiles();

        if (!trackedFiles.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            throw new GitletException();
        } else {

            File uFile = new File(fileName);


            String blobFileName = Constant.Stag + trackedFiles.get(fileName);
            File stagFile = new File(blobFileName);
            String contents = Utils.readContentsAsString(stagFile);

            Utils.writeContents(uFile, contents);

        }
    }


    public void checkout(String branchN) {

        if (!treeBranche.containsKey(branchN)) {
            System.out.println("No such branch exists.");
            throw new GitletException();
        }

        if (header.equals(branchN)) {
            String s = "No need to checkout the current branch.";
            System.out.println(s);
            throw new GitletException();
        }

        String commID = treeBranche.get(branchN);
        Commit comm = getCommit(commID);

        HashMap<String, String> files = comm.getFiles();

        String userDirrStr = System.getProperty("user.dir");
        File userDirr = new File(userDirrStr);
        checkUserDir(userDirr);
        File[] listFile = userDirr.listFiles();

        for ( int i = 0;i < listFile.length;i++){
            File file = listFile[i];
            if (files == null) {
                Utils.restrictedDelete(file);
            } else {
                if (!files.containsKey(file.getName()) && !file.getName().equals(".gitlet")) {
                    Utils.restrictedDelete(file);
                }
            }
        }


        if (files != null) {
            Iterator<String> strIt = files.keySet().iterator();
            while(strIt.hasNext()){
                String file = strIt.next();
                File stagFile = new File(Constant.Stag + files.get(file));
                String contents = Utils.readContentsAsString(stagFile);
                Utils.writeContents(new File(file), contents);
            }
        }
        stagingFiles = new HashMap<String, String>();
        untrackedFiles = new ArrayList<String>();
        header = branchN;

    }

    private String shortId2CommitId(String commitId) {
        if (commitId.length() == Utils.UID_LENGTH) {
            return commitId;
        }
        File commitFolder = new File(".gitlet/commits");
        File[] commitsFiles = commitFolder.listFiles();
        for(int i = 0;i < commitsFiles.length;i++){
            File file = commitsFiles[i];
            if (file.getName().contains(commitId)) {
                return file.getName();
            }
        }
        System.out.println("No commit with that id exists.");
        throw new GitletException();

    }



    private void checkUserDir(File userDir) {
        Commit mostRecent = getCommit(getHeader());
        HashMap<String, String> trackedFiles = mostRecent.getFiles();
        File[] userDirs =  userDir.listFiles();
        for( int i = 0;i < userDirs.length;i++){
            File file = userDirs[i];
            if (trackedFiles == null) {

                if (userDir.listFiles().length > 1) {
                    System.out.println("There is an untracked file in the way; delete it or add it first.");
                    throw new GitletException();
                }
            } else {

                //工作目录有未包含上次提交的文件
                boolean b = !trackedFiles.containsKey(file.getName());
                boolean c = !stagingFiles.containsKey(file.getName());
                if (b && !file.getName().equals(".gitlet") && c) {
                    System.out.println("There is an untracked file in the way; delete it or add it first.");
                    throw new GitletException();
                }
            }
        }

    }


    public void branch(String branchN) {
        if (!treeBranche.containsKey(branchN)) {
            treeBranche.put(branchN, getHeader());
        } else {
            System.out.println("A branch with that name already exists.");
            throw new GitletException();
        }
    }


    public void rmBranch(String branchN) {
        if (branchN.equals(header)) {
            System.out.println("Cannot remove the current branch.");
            throw new GitletException();
        }
        if (treeBranche.containsKey(branchN)) {
            treeBranche.remove(branchN);
        } else {
            System.out.println("A branch with that name does not exist.");
            throw new GitletException();
        }
    }


    public void reset(String commID) {
        commID = shortId2CommitId(commID);
        Commit comm = getCommit(commID);
        HashMap<String, String> files = comm.getFiles();

        //获取用户工作目录
        String userDirStr = System.getProperty("user.dir");
        File userDir = new File(userDirStr);
        checkUserDir(userDir);
        File[] listFile = userDir.listFiles();
        for(int i = 0;i < listFile.length;i++){
            File file = listFile[i];
            if (!files.containsKey(file.getName())) {
                Utils.restrictedDelete(file);
            }
        }
       Iterator<String> commitFiles = files.keySet().iterator();
        while (commitFiles.hasNext()){
            String file = commitFiles.next();
            File f = new File(Constant.Stag + files.get(file));
            String contents = Utils.readContentsAsString(f);
            Utils.writeContents(new File(file), contents);
        }

        stagingFiles = new HashMap<String, String>();
        treeBranche.put(header, commID);
    }
    public void merge(String branchN) {

        String split = getSplit(branchN, header);

        checkMerge(branchN,split);


        Commit splitCom = getCommit(split);
        HashMap<String, String> splitComFiles = splitCom.getFiles();


        Commit headerCom = getCommit(getHeader());
        HashMap<String, String> headerComFiles = headerCom.getFiles();


        Commit branchCom = getCommit(treeBranche.get(branchN));

        HashMap<String, String> branchComFiles = branchCom.getFiles();



        String pwdString = System.getProperty("user.dir");
        File pwd = new File(pwdString);


        Iterator<String> branchComFilesIt = branchComFiles.keySet().iterator();
        while(branchComFilesIt.hasNext()){
            String branchFile = branchComFilesIt.next();
            boolean splitAndBranch = compFileExis(branchFile,splitComFiles,branchComFiles);
            boolean splitAndHeader = compFileExis(branchFile,splitComFiles,headerComFiles);


            if(!splitComFiles.containsKey(branchFile) && !headerComFiles.containsKey(branchFile)){

                String branComId = treeBranche.get(branchN);
                String[] strArr = new String[3];
                strArr[0] = branComId;
                strArr[1] = "--";
                strArr[2] = branchFile;
                checkout(strArr);
                add(branchFile);
                Utils.restrictedDelete(branchFile);
                continue;
            }

            if(!splitComFiles.containsKey(branchFile) && compFile(branchFile,branchComFiles,headerComFiles)){

                String p = ".gitlet/staging/";
                File c = new File(p + headerComFiles.get(branchFile));
                File g = new File(p + branchComFiles.get(branchFile));
                String contents = "<<<<<<< HEAD\n";
                contents += Utils.readContentsAsString(c);
                contents += "=======\n";
                contents += Utils.readContentsAsString(g) + ">>>>>>>";
                Utils.writeContents(new File(branchFile), contents);
                add(branchFile);
                System.out.println("Encountered a merge conflict.");
                continue;
            }

            if(splitAndBranch && !splitAndHeader){
                stagingFiles.put(branchFile,branchComFiles.get(branchFile));
                continue;
            }

            if(!splitAndBranch && splitAndHeader){
                continue;
            }

            if(!splitAndBranch && !splitAndHeader){
                continue;
            }

        }

        Iterator<String> splitComFilesIt = splitComFiles.keySet().iterator();
        while(splitComFilesIt.hasNext()){
            String splitFile = splitComFilesIt.next();

            if(!branchComFiles.containsKey(splitFile) && !compFileExis(splitFile,splitComFiles,headerComFiles)){
                rm(splitFile);
                continue;
            }

            if(!headerComFiles.containsKey(splitFile) && compFileExis(splitFile,splitComFiles,branchComFiles)){
                continue;
            }

            if(true){
                continue;
            }
        }

        String[] parents = new String[]{getHeader(), treeBranche.get(branchN)};

        String commitMsg = "Merged " + branchN + " into " + header + ".";
        commit(commitMsg, parents);
    }


    public void checkMerge(String branch,String split){

        if (stagingFiles.size() != 0 || untrackedFiles.size() != 0) {
            System.out.println("You have uncommitted changes.");
            throw new GitletException();
        }

        if (!treeBranche.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            throw new GitletException();
        }

        if (branch.equals(header)) {
            System.out.println("Cannot merge a branch with itself.");
            throw new GitletException();
        }


        if (split.equals(treeBranche.get(branch))) {
            System.out.println("Given branch is an ancestor of the current branch.");
            throw new GitletException();
        }

        if (split.equals(treeBranche.get(header))) {
            treeBranche.put(header, treeBranche.get(branch));
            System.out.println("Current branch fast-forwarded.");
            throw new GitletException();
        }
    }




    private String getSplit(String b1, String b2) {
        if (b1.equals(b2)) {
            System.out.println("Cannot merge a branch with itself.");
            throw new GitletException();
        }
        ArrayList<String> b1Parents = new ArrayList<String>();
        HashSet<String> b2Parents = new HashSet<String>();

        String b2Par = treeBranche.get(b2);
        String b1Par = treeBranche.get(b1);
        while (b2Par != null) {
            b2Parents.add(b2Par);
            Commit comm2 = getCommit(b2Par);
            b2Par = comm2.getParentID();
        }

        while (b1Par != null) {
            b1Parents.add(b1Par);
            Commit comm1 = getCommit(b1Par);
            b1Par = comm1.getParentID();
        }

        for (String commit : b1Parents) {
            if (b2Parents.contains(commit)) {
                return commit;
            }
        }
        return "";
    }


    boolean compFile(String f, HashMap<String, String> h, HashMap<String, String> i) {
        if (h.containsKey(f) && i.containsKey(f)) {
            String hashF1 = h.get(f);
            String hashF2 = i.get(f);
            if (!hashF1.equals(hashF2)) {
                return true;
            }
        } else if (h.containsKey(f) || i.containsKey(f)) {
            return true;
        }
        return false;
    }

    boolean compFileExis(String f, HashMap<String, String> h, HashMap<String, String> i) {
        if (h.containsKey(f) && i.containsKey(f)) {
            String hashF1 = h.get(f);
            String hashF2 = i.get(f);
            if (!hashF1.equals(hashF2)) {
                return true;
            }
        }
        return false;
    }


    public Commit getCommit(String commitId) {
        File f = new File(Constant.Commit + commitId);
        if (!f.exists()) {
            System.out.println("No commit with that id exists.");
            throw new GitletException();
        } else {
            return Utils.readObject(f, Commit.class);
        }
    }









}
