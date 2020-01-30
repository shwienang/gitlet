package gitlet;


import java.io.File;
import java.util.Arrays;



public class Main {


    public static void main(String... args) {
        // FILL THIS IN
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String command = args[0];

        if (!checkRepository()) {
            if(command.equals("init")){
                BlobTree blobTree = new BlobTree();
                File newMyR = new File(Constant.MyRepo);
                Utils.writeObject(newMyR, blobTree);
                return ;

            }else{
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
        }
        BlobTree blobTree = null;
        try {
            switch (command) {
                case "init":
                    System.out.println("A Gitlet version-control system already exists in the current directory.");
                    System.exit(0);
                    break;
                case "list":
                    // my own test. stop looking at me.
                    break;
                case "commit":
                    blobTree = getBlobTree();
                    blobTree.commit(args[1]);
                    break;
                case "add":
                    blobTree = getBlobTree();
                    blobTree.add(args[1]);
                    break;

                case "rm":
                    blobTree = getBlobTree();
                    blobTree.rm(args[1]);
                    break;
                case "log":
                    blobTree = getBlobTree();
                    blobTree.logCommits();
                    break;
                case "checkout":
                    String[] actionCommand = Arrays.copyOfRange(args, 1, args.length);
                    blobTree = getBlobTree();
                    blobTree.checkout(actionCommand);
                    break;
                case "global-log":
                    blobTree = getBlobTree();
                    blobTree.globalLog();
                    break;
                case "find":
                    blobTree = getBlobTree();
                    blobTree.find(args[1]);
                    break;
                case "status":
                    blobTree = getBlobTree();
                    blobTree.status();
                    break;
                case "reset":
                    blobTree = getBlobTree();
                    blobTree.reset(args[1]);
                    break;
                case "merge":
                    blobTree = getBlobTree();
                    blobTree.merge(args[1]);
                    break;
                case "branch":
                    blobTree = getBlobTree();
                    blobTree.branch(args[1]);
                    break;
                case "rm-branch":
                    blobTree = getBlobTree();
                    blobTree.rmBranch(args[1]);
                    break;

                default:
                    System.out.println("No command with that name exists.");
                    System.exit(0);
            }
            File newMyR = new File(Constant.MyRepo);
            Utils.writeObject(newMyR, blobTree);
        }catch (GitletException e){
            System.exit(0);
        }
    }

    public static boolean checkRepository(){
        File gitlet = new File(System.getProperty("user.dir") + "/.gitlet");
        if (gitlet.exists()) {
            return true;
        }
        return false;
    }

    public static BlobTree getBlobTree() {
        File mr =  new File(Constant.MyRepo);
        return Utils.readObject(mr, BlobTree.class);
    }
}
