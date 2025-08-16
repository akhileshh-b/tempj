import java.util.*;
import java.io.*;

class Node {
    String label;
    ArrayList<Node> children;
    Node parent;
    int upCount, downCount, userId;
    boolean isLocked;
    
    Node(String s, Node p) {
        label = s;
        parent = p;
        children = new ArrayList<>();
        upCount = downCount = userId = 0;
        isLocked = false;
    }
}

// Custom hash table implementation instead of HashMap
class NodeFinder {
    private ArrayList<ArrayList<Node>> buckets;
    private int size;
    
    NodeFinder() {
        size = 10007; // prime number for better distribution
        buckets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            buckets.add(new ArrayList<>());
        }
    }
    
    private int hash(String key) {
        int hashVal = 0;
        for (int i = 0; i < key.length(); i++) {
            hashVal = hashVal * 31 + key.charAt(i);
        }
        return Math.abs(hashVal) % size;
    }
    
    void put(String key, Node value) {
        int idx = hash(key);
        ArrayList<Node> bucket = buckets.get(idx);
        // Check if key already exists
        for (int i = 0; i < bucket.size(); i++) {
            if (bucket.get(i).label.equals(key)) {
                bucket.set(i, value);
                return;
            }
        }
        bucket.add(value);
    }
    
    Node get(String key) {
        int idx = hash(key);
        ArrayList<Node> bucket = buckets.get(idx);
        for (Node node : bucket) {
            if (node.label.equals(key)) {
                return node;
            }
        }
        return null;
    }
}

class TestClass {
    static Node root;
    static NodeFinder finder;
    
    // Build mapping using iterative DFS approach
    static void setupFinder(Node start) {
        Stack<Node> a = new Stack<>();
        a.push(start);
        while (!a.isEmpty()) {
            Node temp = a.pop();
            finder.put(temp.label, temp);
            // Push children in reverse order for correct traversal
            for (int i = temp.children.size() - 1; i >= 0; i--) {
                a.push(temp.children.get(i));
            }
        }
    }
    
    // Update descendants iteratively using BFS
    static void fixDescendants(Node start, int delta) {
        Queue<Node> one = new LinkedList<>();
        // Initialize with immediate children
        for (Node child : start.children) {
            one.offer(child);
        }
        while (!one.isEmpty()) {
            Node temp = one.poll();
            temp.upCount += delta;
            // Add next level children
            for (Node child : temp.children) {
                one.offer(child);
            }
        }
    }
    
    // Validate descendant ownership using BFS
    static boolean validateLocks(Node start, int uid, ArrayList<Node> found) {
        Queue<Node> two = new LinkedList<>();
        two.offer(start);
        while (!two.isEmpty()) {
            Node temp = two.poll();
            if (temp.isLocked) {
                if (temp.userId != uid) return false;
                found.add(temp);
            }
            // Continue traversal only if descendants are locked
            if (temp.downCount > 0) {
                for (Node child : temp.children) {
                    two.offer(child);
                }
            }
        }
        return true;
    }
    
    // Lock operation - check user first, then ancestors/descendants
    static boolean doLock(String name, int uid) {
        Node var = finder.get(name);
        if (var == null) return false; // Handle invalid node names
        // Quick fail checks
        if (var.isLocked) return false;
        if (var.upCount > 0 || var.downCount > 0) return false;
        // Update parent chain
        Node temp1 = var.parent;
        while (temp1 != null) {
            temp1.downCount++;
            temp1 = temp1.parent;
        }
        // Fix all descendants
        fixDescendants(var, 1);
        // Set lock
        var.isLocked = true;
        var.userId = uid;
        return true;
    }
    
    // Unlock - verify ownership first
    static boolean doUnlock(String name, int uid) {
        Node b = finder.get(name);
        if (b == null) return false; // Handle invalid node names
        // Check ownership before anything else
        if (!b.isLocked || b.userId != uid) return false;
        // Update parent chain
        Node temp2 = b.parent;
        while (temp2 != null) {
            temp2.downCount--;
            temp2 = temp2.parent;
        }
        // Fix descendants
        fixDescendants(b, -1);
        // Remove lock
        b.isLocked = false;
        b.userId = 0;
        return true;
    }
    
    // Upgrade - different order of checks
    static boolean doUpgrade(String name, int uid) {
        Node c = finder.get(name);
        if (c == null) return false; // Handle invalid node names
        // Check if upgrade is even possible
        if (c.downCount == 0) return false;
        if (c.isLocked) return false;
        if (c.upCount > 0) return false;
        // Validate all locked descendants
        ArrayList<Node> three = new ArrayList<>();
        if (!validateLocks(c, uid, three)) {
            return false;
        }
        // Unlock all descendants first
        for (Node temp : three) {
            doUnlock(temp.label, uid);
        }
        // Then lock current node
        return doLock(name, uid);
    }
    
    public static void main(String args[]) throws Exception {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt(); // total nodes
        int m = sc.nextInt(); // children per node
        int q = sc.nextInt(); // queries
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            names[i] = sc.next();
        }
        // Build tree level by level
        root = new Node(names[0], null);
        Queue<Node> buildQueue = new LinkedList<>();
        buildQueue.add(root);
        int pos = 1;
        while (!buildQueue.isEmpty() && pos < n) {
            Node temp3 = buildQueue.poll();
            // Add m children or until we run out of names
            for (int j = 0; j < m && pos < n; j++) {
                Node child = new Node(names[pos], temp3);
                temp3.children.add(child);
                buildQueue.add(child);
                pos++;
            }
        }
        // Setup custom node finder
        finder = new NodeFinder();
        setupFinder(root);
        // Process queries
        for (int i = 0; i < q; i++) {
            int op = sc.nextInt();
            String nodeName = sc.next();
            int userId = sc.nextInt();
            boolean result = false;
            // Handle operations
            if (op == 1) {
                result = doLock(nodeName, userId);
            } else if (op == 2) {
                result = doUnlock(nodeName, userId);
            } else { // op == 3
                result = doUpgrade(nodeName, userId);
            }
            // Output result immediately
            System.out.println(result ? "true" : "false");
        }
        sc.close();
    }
}