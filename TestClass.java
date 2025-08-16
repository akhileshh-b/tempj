// Change 1: Replaced custom hash table with HashMap
// javastatic HashMap<String, Node> finder;  // Instead of NodeFinder
// Change 2: Added helper method for parent chain updates
// javastatic void updateParentChain(Node node, int delta) {
//     Node temp = node.parent;
//     while (temp != null) {
//         temp.downCount += delta;
//         temp = temp.parent;
//     }
// }
// Changes 3 & 4: Use helper method in lock/unlock
// javaupdateParentChain(var, 1);   // Instead of manual while loop
// updateParentChain(b, -1);    // Instead of manual while loop
// Change 5: Optimized upgrade operation

// Batch unlock without calling doUnlock() (which causes redundant parent updates)
// Single parent chain update with net change
// Eliminates the major TLE bottleneck

// Change 6: Initialize HashMap instead of custom NodeFinder
// Removed Code:

// Entire NodeFinder class (60+ lines of inefficient code)

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

class TestClass {
    static Node root;
    static HashMap<String, Node> finder;  // CHANGE 1: Use HashMap instead of custom implementation
    
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
    
    // CHANGE 2: Add helper method for batch parent updates
    static void updateParentChain(Node node, int delta) {
        Node temp = node.parent;
        while (temp != null) {
            temp.downCount += delta;
            temp = temp.parent;
        }
    }
    
    // Lock operation - check user first, then ancestors/descendants
    static boolean doLock(String name, int uid) {
        Node var = finder.get(name);
        if (var == null) return false; // Handle invalid node names
        
        // Quick fail checks
        if (var.isLocked) return false;
        if (var.upCount > 0 || var.downCount > 0) return false;
        
        // Update parent chain
        updateParentChain(var, 1);  // CHANGE 3: Use helper method
        
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
        updateParentChain(b, -1);  // CHANGE 4: Use helper method
        
        // Fix descendants
        fixDescendants(b, -1);
        
        // Remove lock
        b.isLocked = false;
        b.userId = 0;
        
        return true;
    }
    
    // CHANGE 5: Optimized upgrade operation
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
        
        // OPTIMIZED: Batch unlock without individual parent updates
        for (Node temp : three) {
            temp.isLocked = false;
            temp.userId = 0;
            fixDescendants(temp, -1);  // Fix descendants for each unlocked node
        }
        
        // Update parent chain once with net change
        updateParentChain(c, three.size());
        
        // Fix descendants and lock current node
        fixDescendants(c, 1);
        c.isLocked = true;
        c.userId = uid;
        
        return true;
    }
    
    public static void main(String args[]) throws Exception {
        Scanner sc = new Scanner(System.in);
        
        int n = sc.nextInt();  // total nodes
        int m = sc.nextInt();  // children per node
        int q = sc.nextInt();  // queries
        
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
        
        // Setup finder
        finder = new HashMap<>();  // CHANGE 6: Initialize HashMap
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
