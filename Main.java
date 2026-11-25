import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

/*
 * PS 37 - Employee Attendance Tracker
 * Language : Java (single .java file, run from the console)
 * Concepts : ArrayList (array), HashMap, Searching, Methods, File Handling
 *
 * How to run:
 *     javac AttendanceTracker.java
 *     java AttendanceTracker
 *
 * Data is stored in attendance.txt (one employee per line):
 *     ID,Name,Present,Absent        e.g.  EMP101,Deepak Kumar,0,0
 */

// ---------------------------------------------------------------------------
// AttendanceTracker : menu (main class)
// ---------------------------------------------------------------------------
public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final AttendanceManager manager = new AttendanceManager();

    public static void main(String[] args) {
        manager.loadFromFile();

        boolean running = true;
        while (running) {
            showMenu();
            String line = readLine("Enter choice: ");
            if (line == null) {          // input ended (for example Ctrl+D) -> save and quit
                break;
            }
            switch (readChoice(line)) {
                case 1: addEmployee();     break;
                case 2: markAttendance();  break;
                case 3: searchEmployee();  break;
                case 4: manager.generateReport(); break;
                case 5: running = false;   break;
                default:
                    System.out.println("Invalid choice. Enter a number from 1 to 5.");
            }
        }

        manager.saveToFile();
        System.out.println("Data saved to attendance.txt. Goodbye!");
    }

    private static void showMenu() {
        System.out.println();
        System.out.println("========== EMPLOYEE ATTENDANCE TRACKER ==========");
        System.out.println("1. Add Employee");
        System.out.println("2. Mark Attendance (P/A)");
        System.out.println("3. Search Employee");
        System.out.println("4. Attendance Report");
        System.out.println("5. Exit");
    }

    private static int readChoice(String line) {
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // prints a prompt and returns the typed line (null if there is no more input)
    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!sc.hasNextLine()) {
            System.out.println();
            return null;
        }
        return sc.nextLine();
    }

    // ---------------- menu actions ----------------

    private static void addEmployee() {
        String name = readLine("Enter employee name: ");
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        if (name.contains(",")) {
            System.out.println("Name cannot contain a comma.");
            return;
        }
        Employee e = manager.addEmployee(name);
        System.out.println("Employee added. " + manager.counts(e));
    }

    private static void markAttendance() {
        String id = readLine("Enter employee ID: ");
        if (id == null) return;
        String status = readLine("Enter status (P/A): ");
        if (status == null) return;

        status = status.trim().toUpperCase();
        if (!status.equals("P") && !status.equals("A")) {
            System.out.println("Invalid status. Enter P or A.");
            return;
        }
        Employee e = manager.markAttendance(id, status.charAt(0));
        if (e == null) {
            System.out.println("Employee not found.");
        } else {
            System.out.println("Attendance marked. " + manager.counts(e));
        }
    }

    private static void searchEmployee() {
        String key = readLine("Enter employee ID or name: ");
        if (key == null) return;
        if (key.trim().isEmpty()) {
            System.out.println("Please enter an ID or a name.");
            return;
        }
        Employee e = manager.search(key);
        if (e == null) {
            System.out.println("Employee not found.");
        } else {
            System.out.println("Found: " + manager.describe(e));
        }
    }
}

// ---------------------------------------------------------------------------
// Employee : one attendance record
// ---------------------------------------------------------------------------
class Employee {
    private final String id;
    private final String name;
    private int present;
    private int absent;

    Employee(String id, String name, int present, int absent) {
        this.id = id;
        this.name = name;
        this.present = present;
        this.absent = absent;
    }

    String getId()      { return id; }
    String getName()    { return name; }
    int getPresent()    { return present; }
    int getAbsent()     { return absent; }

    void markPresent()  { present++; }
    void markAbsent()   { absent++; }

    boolean hasAttendance() {
        return (present + absent) > 0;
    }

    // attendance % = present / (present + absent) x 100
    double getPercentage() {
        if (!hasAttendance()) {
            return 0.0;
        }
        return (present * 100.0) / (present + absent);
    }

    String formatPercentage() {
        return String.format(Locale.US, "%.2f%%", getPercentage());
    }

    // line stored in attendance.txt
    String toFileString() {
        return id + "," + name + "," + present + "," + absent;
    }
}

// ---------------------------------------------------------------------------
// AttendanceManager : ArrayList + HashMap + file handling + searching/sorting
// ---------------------------------------------------------------------------
class AttendanceManager {
    private static final String FILE_NAME = "attendance.txt";
    private static final int FIRST_ID = 101;

    // true  -> report is sorted by attendance % (highest first) with our own sort
    // false -> report is printed in ID (insertion) order
    private static final boolean SORT_REPORT = true;

    private final ArrayList<Employee> employeeList = new ArrayList<>();
    private final HashMap<String, Employee> employeeMap = new HashMap<>();
    private int nextId = FIRST_ID;

    // ---------------- file handling ----------------

    // Load data from the file at start. A missing file just means "no data yet".
    void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    continue; // skip a damaged line
                }
                try {
                    String id = parts[0].trim().toUpperCase();
                    String name = parts[1].trim();
                    int present = Integer.parseInt(parts[2].trim());
                    int absent = Integer.parseInt(parts[3].trim());
                    if (!id.startsWith("EMP") || employeeMap.containsKey(id)) {
                        continue;
                    }
                    Employee e = new Employee(id, name, present, absent);
                    employeeList.add(e);
                    employeeMap.put(id, e);

                    int number = Integer.parseInt(id.substring(3));
                    if (number >= nextId) {
                        nextId = number + 1;
                    }
                } catch (NumberFormatException ex) {
                    // skip a line with bad numbers
                }
            }
        } catch (IOException ex) {
            System.out.println("Could not read " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    // Save every employee to the file (called on exit).
    void saveToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (int i = 0; i < employeeList.size(); i++) {
                bw.write(employeeList.get(i).toFileString());
                bw.newLine();
            }
        } catch (IOException ex) {
            System.out.println("Could not save " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    // ---------------- features ----------------

    // 1 - Add Employee
    Employee addEmployee(String name) {
        String id = "EMP" + nextId;
        nextId++;
        Employee e = new Employee(id, name, 0, 0);
        employeeList.add(e);      // ArrayList keeps the order
        employeeMap.put(id, e);   // HashMap gives quick lookup by ID
        return e;
    }

    // 2 - Mark Attendance. Returns the employee, or null if the ID does not exist.
    // The caller must already have checked that the status is 'P' or 'A'.
    Employee markAttendance(String id, char status) {
        Employee e = searchById(id);
        if (e == null) {
            return null;
        }
        if (status == 'P') {
            e.markPresent();
        } else {
            e.markAbsent();
        }
        return e;
    }

    // 3a - Search by ID : HashMap lookup, O(1) on average
    Employee searchById(String id) {
        return employeeMap.get(id.trim().toUpperCase());
    }

    // 3b - Search by name : our own linear search over the list, O(n)
    Employee searchByName(String name) {
        String key = name.trim().toLowerCase();
        for (int i = 0; i < employeeList.size(); i++) {
            Employee e = employeeList.get(i);
            if (e.getName().toLowerCase().equals(key)) {
                return e;
            }
        }
        // no exact match: try part of a name (for example "kavita")
        for (int i = 0; i < employeeList.size(); i++) {
            Employee e = employeeList.get(i);
            if (e.getName().toLowerCase().contains(key)) {
                return e;
            }
        }
        return null;
    }

    // Search by ID first, otherwise by name
    Employee search(String key) {
        Employee e = searchById(key);
        if (e == null) {
            e = searchByName(key);
        }
        return e;
    }

    // 4 - Attendance Report
    void generateReport() {
        if (employeeList.isEmpty()) {
            System.out.println("No employees added yet.");
            return;
        }

        // work on a copy so the original (ID) order in the file never changes
        ArrayList<Employee> view = new ArrayList<>(employeeList);
        if (SORT_REPORT) {
            sortByPercentage(view);
        }

        System.out.println("---------------- ATTENDANCE REPORT ----------------");
        for (int i = 0; i < view.size(); i++) {
            System.out.println(describe(view.get(i)));
        }
        System.out.println("---------------------------------------------------");
    }

    // ---------------- helpers ----------------

    // Own selection sort (no Collections.sort / Arrays.sort):
    // highest attendance % first, employees with no attendance last, ties keep ID order.
    private void sortByPercentage(ArrayList<Employee> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            int best = i;
            for (int j = i + 1; j < list.size(); j++) {
                if (comesBefore(list.get(j), list.get(best))) {
                    best = j;
                }
            }
            if (best != i) {
                Employee temp = list.get(i);
                list.set(i, list.get(best));
                list.set(best, temp);
            }
        }
    }

    // true if a should be listed before b
    private boolean comesBefore(Employee a, Employee b) {
        if (a.hasAttendance() != b.hasAttendance()) {
            return a.hasAttendance();               // records first, "no attendance" last
        }
        if (a.getPercentage() != b.getPercentage()) {
            return a.getPercentage() > b.getPercentage();
        }
        return idNumber(a) < idNumber(b);           // equal -> lower ID first
    }

    private int idNumber(Employee e) {
        return Integer.parseInt(e.getId().substring(3));
    }

    // One-line description used by Report and Search
    String describe(Employee e) {
        if (!e.hasAttendance()) {
            return e.getId() + " | " + e.getName() + " | No attendance recorded";
        }
        return e.getId() + " | " + e.getName()
                + " | Present: " + e.getPresent()
                + " | Absent: " + e.getAbsent()
                + " | Attendance: " + e.formatPercentage();
    }

    String counts(Employee e) {
        return e.getId() + " | " + e.getName()
                + " | Present: " + e.getPresent() + " | Absent: " + e.getAbsent();
    }
}