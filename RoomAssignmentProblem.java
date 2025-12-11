import java.util.*;
import java.io.*;

// Course class representing a university course
class Course {
    private int id;
    private String name;
    private int enrollment;
    private List<TimeSlot> schedule;

    public Course(int id, String name, int enrollment, List<TimeSlot> schedule) {
        this.id = id;
        this.name = name;
        this.enrollment = enrollment;
        this.schedule = schedule;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getEnrollment() { return enrollment; }
    public List<TimeSlot> getSchedule() { return schedule; }

    public boolean hasConflict(Course other) {
        for (TimeSlot t1 : this.schedule) {
            for (TimeSlot t2 : other.getSchedule()) {
                if (t1.overlapsWith(t2)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Course[id=%d, name=%s, enrollment=%d]", id, name, enrollment);
    }
}

// TimeSlot class representing when a course meets
class TimeSlot {
    private String day; // MON, TUE, WED, THU, FRI
    private int startTime; // minutes from midnight (e.g., 540 = 9:00 AM)
    private int endTime;

    public TimeSlot(String day, int startTime, int endTime) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getDay() { return day; }
    public int getStartTime() { return startTime; }
    public int getEndTime() { return endTime; }

    public boolean overlapsWith(TimeSlot other) {
        if (!this.day.equals(other.day)) {
            return false;
        }
        // Check if time ranges overlap
        return this.startTime < other.endTime && other.startTime < this.endTime;
    }

    @Override
    public String toString() {
        return String.format("%s %d:%02d-%d:%02d", day,
                startTime/60, startTime%60, endTime/60, endTime%60);
    }
}

// Room class representing a classroom
class Room {
    private int id;
    private String name;
    private int capacity;
    private List<TimeSlot> occupiedSlots;

    public Room(int id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.occupiedSlots = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }

    public boolean isAvailable(List<TimeSlot> requestedSlots) {
        for (TimeSlot requested : requestedSlots) {
            for (TimeSlot occupied : occupiedSlots) {
                if (requested.overlapsWith(occupied)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void occupy(List<TimeSlot> slots) {
        occupiedSlots.addAll(slots);
    }

    public void reset() {
        occupiedSlots.clear();
    }

    @Override
    public String toString() {
        return String.format("Room[id=%d, name=%s, capacity=%d]", id, name, capacity);
    }
}

// Assignment result
class Assignment {
    private Map<Course, Room> courseToRoom;
    private int totalRoomsUsed;
    private boolean feasible;

    public Assignment() {
        this.courseToRoom = new HashMap<>();
        this.feasible = true;
    }

    public void assign(Course course, Room room) {
        courseToRoom.put(course, room);
    }

    public Room getRoom(Course course) {
        return courseToRoom.get(course);
    }

    public void setFeasible(boolean feasible) {
        this.feasible = feasible;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public void computeTotalRooms() {
        Set<Room> uniqueRooms = new HashSet<>(courseToRoom.values());
        totalRoomsUsed = uniqueRooms.size();
    }

    public int getTotalRoomsUsed() {
        return totalRoomsUsed;
    }

    public Map<Course, Room> getAssignments() {
        return courseToRoom;
    }
}

// Main Greedy Algorithm
class RoomAssignmentSolver {

    public Assignment solve(List<Course> courses, List<Room> rooms) {
        Assignment assignment = new Assignment();

        // Reset all rooms
        for (Room room : rooms) {
            room.reset();
        }

        // Step 1: Sort courses by enrollment size (descending)
        List<Course> sortedCourses = new ArrayList<>(courses);
        sortedCourses.sort((c1, c2) -> Integer.compare(c2.getEnrollment(), c1.getEnrollment()));

        // Step 2: Greedy assignment
        for (Course course : sortedCourses) {
            Room assignedRoom = assignCourseToRoom(course, rooms);

            if (assignedRoom == null) {
                assignment.setFeasible(false);
                return assignment;
            }

            assignment.assign(course, assignedRoom);
            assignedRoom.occupy(course.getSchedule());
        }

        assignment.computeTotalRooms();
        return assignment;
    }

    private Room assignCourseToRoom(Course course, List<Room> rooms) {
        // Find eligible rooms (capacity constraint)
        List<Room> eligibleRooms = new ArrayList<>();
        for (Room room : rooms) {
            if (room.getCapacity() >= course.getEnrollment()) {
                eligibleRooms.add(room);
            }
        }

        // Sort eligible rooms by capacity (ascending) - best fit strategy
        eligibleRooms.sort(Comparator.comparingInt(Room::getCapacity));

        // Find first available room
        for (Room room : eligibleRooms) {
            if (room.isAvailable(course.getSchedule())) {
                return room;
            }
        }

        return null; // No room available
    }
}

// Experimental validation
class ExperimentRunner {
    private Random random;

    public ExperimentRunner(long seed) {
        this.random = new Random(seed);
    }

    public List<Course> generateCourses(int numCourses) {
        List<Course> courses = new ArrayList<>();
        String[] days = {"MON", "TUE", "WED", "THU", "FRI"};

        // Pre-define time slots to reduce conflicts
        int[][] timeSlots = {
                {480, 530},   // 8:00-8:50
                {540, 590},   // 9:00-9:50
                {600, 650},   // 10:00-10:50
                {660, 710},   // 11:00-11:50
                {720, 770},   // 12:00-12:50
                {780, 830},   // 13:00-13:50
                {840, 890},   // 14:00-14:50
                {900, 950},   // 15:00-15:50
                {960, 1010},  // 16:00-16:50
                {1020, 1070}  // 17:00-17:50
        };

        for (int i = 0; i < numCourses; i++) {
            // More reasonable enrollment distribution
            int enrollment = 15 + random.nextInt(136); // 15-150 students

            // Generate 2-3 meeting times per week (MWF or TR pattern)
            List<TimeSlot> schedule = new ArrayList<>();

            // Choose a pattern
            boolean mwfPattern = random.nextBoolean();
            int timeSlotIndex = random.nextInt(timeSlots.length);
            int startTime = timeSlots[timeSlotIndex][0];
            int endTime = timeSlots[timeSlotIndex][1];

            if (mwfPattern) {
                // Monday, Wednesday, Friday
                schedule.add(new TimeSlot("MON", startTime, endTime));
                schedule.add(new TimeSlot("WED", startTime, endTime));
                schedule.add(new TimeSlot("FRI", startTime, endTime));
            } else {
                // Tuesday, Thursday
                schedule.add(new TimeSlot("TUE", startTime, endTime));
                schedule.add(new TimeSlot("THU", startTime, endTime));
            }

            courses.add(new Course(i, "CS" + i, enrollment, schedule));
        }

        return courses;
    }

    public List<Room> generateRooms(int numRooms) {
        List<Room> rooms = new ArrayList<>();

        // Better distribution: more variety in room sizes
        // Ensure we have enough large rooms
        int smallRooms = (int)(numRooms * 0.3);     // 30% small (20-40)
        int mediumRooms = (int)(numRooms * 0.4);    // 40% medium (50-80)
        int largeRooms = numRooms - smallRooms - mediumRooms; // 30% large (100-200)

        int roomId = 0;

        // Small rooms
        for (int i = 0; i < smallRooms; i++) {
            int capacity = 20 + random.nextInt(21); // 20-40
            rooms.add(new Room(roomId++, "Room" + roomId, capacity));
        }

        // Medium rooms
        for (int i = 0; i < mediumRooms; i++) {
            int capacity = 50 + random.nextInt(31); // 50-80
            rooms.add(new Room(roomId++, "Room" + roomId, capacity));
        }

        // Large rooms
        for (int i = 0; i < largeRooms; i++) {
            int capacity = 100 + random.nextInt(101); // 100-200
            rooms.add(new Room(roomId++, "Room" + roomId, capacity));
        }

        return rooms;
    }

    public void runExperiment(int[] courseCounts) {
        System.out.println("Running experiments...");
        System.out.println("Courses\tRooms\tTime(ms)\tRoomsUsed\tSuccess");
        System.out.println("-------\t-----\t--------\t---------\t-------");

        RoomAssignmentSolver solver = new RoomAssignmentSolver();

        for (int numCourses : courseCounts) {
            // Ensure enough rooms: rule of thumb is ~40% of courses
            int numRooms = Math.max(10, (int)(numCourses * 0.4));

            List<Course> courses = generateCourses(numCourses);
            List<Room> rooms = generateRooms(numRooms);

            long startTime = System.nanoTime();
            Assignment assignment = solver.solve(courses, rooms);
            long endTime = System.nanoTime();

            double timeMs = (endTime - startTime) / 1_000_000.0;

            System.out.printf("%d\t%d\t%.2f\t\t%d\t\t%s%n",
                    numCourses,
                    numRooms,
                    timeMs,
                    assignment.getTotalRoomsUsed(),
                    assignment.isFeasible() ? "Yes" : "No");
        }
    }

    public void runDetailedExperiment(int numIterations, int[] courseCounts) {
        System.out.println("\n\nDetailed Performance Analysis:");
        System.out.println("Courses\tAvgTime(ms)\tStdDev\t\tAvgRooms\tSuccessRate");
        System.out.println("-------\t-----------\t------\t\t--------\t-----------");

        RoomAssignmentSolver solver = new RoomAssignmentSolver();

        for (int numCourses : courseCounts) {
            int numRooms = Math.max(10, (int)(numCourses * 0.4));

            List<Double> times = new ArrayList<>();
            List<Integer> roomsUsed = new ArrayList<>();
            int successCount = 0;

            for (int iter = 0; iter < numIterations; iter++) {
                List<Course> courses = generateCourses(numCourses);
                List<Room> rooms = generateRooms(numRooms);

                long startTime = System.nanoTime();
                Assignment assignment = solver.solve(courses, rooms);
                long endTime = System.nanoTime();

                double timeMs = (endTime - startTime) / 1_000_000.0;
                times.add(timeMs);

                if (assignment.isFeasible()) {
                    successCount++;
                    roomsUsed.add(assignment.getTotalRoomsUsed());
                }
            }

            double avgTime = times.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double stdDev = Math.sqrt(times.stream()
                    .mapToDouble(t -> Math.pow(t - avgTime, 2))
                    .average().orElse(0));
            double avgRooms = roomsUsed.isEmpty() ? 0 :
                    roomsUsed.stream().mapToInt(Integer::intValue).average().orElse(0);
            double successRate = (successCount * 100.0) / numIterations;

            System.out.printf("%d\t%.2f\t\t%.2f\t\t%.1f\t\t%.1f%%%n",
                    numCourses, avgTime, stdDev, avgRooms, successRate);
        }
    }
}

// Main class with demonstration
public class RoomAssignmentProblem {

    public static void main(String[] args) {
        // Demo with small example
        System.out.println("=== ROOM ASSIGNMENT PROBLEM SOLVER ===\n");

        System.out.println("Small Example:");
        runSmallExample();

        System.out.println("\n\n=== EXPERIMENTAL VALIDATION ===\n");

        // Run experiments
        ExperimentRunner runner = new ExperimentRunner(42);

        int[] courseCounts = {10, 50, 100, 200, 500, 1000};
        runner.runExperiment(courseCounts);

        // Detailed analysis with multiple iterations
        int[] detailedCounts = {10, 50, 100, 200, 500, 1000};
        runner.runDetailedExperiment(10, detailedCounts);

        System.out.println("\n\nExperiments completed!");
    }

    private static void runSmallExample() {
        // Create courses
        List<Course> courses = new ArrayList<>();
        courses.add(new Course(1, "CS101", 45, Arrays.asList(
                new TimeSlot("MON", 540, 650),  // 9:00-10:50
                new TimeSlot("WED", 540, 650)
        )));
        courses.add(new Course(2, "CS201", 80, Arrays.asList(
                new TimeSlot("TUE", 600, 710),  // 10:00-11:50
                new TimeSlot("THU", 600, 710)
        )));
        courses.add(new Course(3, "CS301", 30, Arrays.asList(
                new TimeSlot("MON", 540, 650),  // Conflicts with CS101
                new TimeSlot("WED", 540, 650)
        )));
        courses.add(new Course(4, "MATH101", 120, Arrays.asList(
                new TimeSlot("TUE", 600, 710),  // Conflicts with CS201
                new TimeSlot("THU", 600, 710)
        )));

        // Create rooms
        List<Room> rooms = new ArrayList<>();
        rooms.add(new Room(1, "RoomA", 50));
        rooms.add(new Room(2, "RoomB", 100));
        rooms.add(new Room(3, "RoomC", 150));

        // Solve
        RoomAssignmentSolver solver = new RoomAssignmentSolver();
        Assignment assignment = solver.solve(courses, rooms);

        // Print results
        if (assignment.isFeasible()) {
            System.out.println("Assignment successful!");
            System.out.println("Total rooms used: " + assignment.getTotalRoomsUsed());
            System.out.println("\nAssignments:");
            for (Map.Entry<Course, Room> entry : assignment.getAssignments().entrySet()) {
                System.out.printf("  %s -> %s%n", entry.getKey(), entry.getValue());
            }
        } else {
            System.out.println("No feasible assignment found!");
        }
    }
}