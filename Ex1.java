import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.Arrays;
import java.util.ArrayList;
/*
 * Ex1 Preamble:
 * Here, the robot will firstly check how many non-Wall exits are surrounding itself, using the nonwallExits method.
 * The # of non-wall exits is determined by the nonwallExits method, which looks around in 4 directions to count
 * the number of squares which are not equal to IRobot.WALL i.e. not a wall exit.
 * This will help to determine what kind of square it is in(e.g. corridor, junction e.t.c.) and will call the relevant
 * function for the sepcified type of square.
 *
 * These controller methods have been stored in a modularised manner, where dead-end() will have its own method, same with corridor()
 * However, for junctions and crossroad, both have a single funcction called junctionAndCrossroads()
 *
 * This is because you can treat junctions and crossroads in the same way, and there is no difference apart from # of exits
 * So this method will look in every open direction, and if there is no passage exit, it will choose a random beenBefore square.
 * If a single passage exit exists, it will go there, otherwise choose randomly between them if there are multiple passage exits.
 * The # of passage exits is determined by the passageExit method, which looks around in 4 directions to count
 * the number of passage squares.
 *
 * In order to make Ex1 more efficient, I have modularised the code into more manageable, individual methods to avoid repetition of code
 * This helps to efficiently execute the program, as well as helping the source code viewer to follow the logic flow.
 * I have also used break in several places in order to avoid the iterations going for longer than neccessary.
 * The program is easy to read because I have not done more than a single operation per line, and trailing comments have been indented.
 *
 * The robot makes use of the RobotData class by creating a new instance of the class whenever it is in a new maze
 * This means that the JunctionInfo arraylist will be new for each maze, and no previous data will remain, in order to save memory.
 * The RobotData class is only used in three instances
 * Firstly, when the robot encounters a new junction or crossroad, and stores the coordinates and heading using the recordJunction method in RobotData
 * It will then also call teh printJunction method of RobotData to print out all the attributes of the new junction.
 * Secondly, when the robot is at a junction/crossroad(in backtracking mode) which has been previuosly fully explored
 * it will call the searchJunction method of the RobotData class to obtain the initial arrived from heading
 * This is vital simce the robot will be backtracking showing that the junction is full explored and does not lead to the target
 * Lastly, when the maze is reset, the resetJunctionCounter method of the RObotData class is called to reset junction counter
 *
 * These are the only three cases where RobotData is used, and it is used in an efficient manner in order to keep memory usage to a minimum.
 * The exploreControl and backtrackControl have been designed so that they only operate the junctionAndCrossroads method differnetly
 * while the corridor and dead end methods are treated the same way in both exploreControl and backtrackControl.
 * The functionality could have been improved for the above control method, i.e. the corridor and dead end methods could be
 * called in controlRobot instead of being called both in exploreControl and backtrackControl since they are treated simliarly.
 *
 * Worst Case analysis:
 * The explorer robot will always find the target in a prim maze, provided that the target is reachable from the spawn point.
 * I tested the robot ~50 times and it always found the target in the prim maze.
 * The worst case scenario in a prim maze is that teh robot will visit each corridor twice (once in explore and once backtracking)
 * and each dead end once, and each junction three times, and each crossroad four times.
 * This means the maximum number of steps it will take in a prim maze will occur when every square has been traversed apart from target
 * The maximum number of steps can be obtained as such
 * ((# of dead-ends) + 2*(# of corridors) + 3*(# of junctions) + 4*(# of crossroads))
 * Howver, the robot is not designed for loopy mazes, and sometimes may solve them, but unlikely to, and this is fixed in later Ex.
 */
/**
 * This is the main explorer (Ex1)class which controls the logic flow, and also calls the RobotData class when needed.
 * This class guides the explorer robot to an appropriate direction every time it is polled.
 * All the methods and global variables are set to private when it is not required that they be accesssed by other classes
 *
 * @author Param Bhatia
 * @since 10-12-2021
 */
public class Ex1 {
    private int pollRun = 0;            /*Incremented after each pass*/
    private RobotData robotData;
    private int explorerMode = 1;       /*Set robot into exploring mode at the beginning*/


    /**
     * randomDirection is used to check all four directions and determine how many non-wall exits exist
     * If there is more than one exit, it chooses randomly between all exits
     *
     * @param robot     Object of the IRobot class
     * @return          randomly chosen non-wall direction for robot to move in
     */
    private int randomDirection(IRobot robot){

        /*Arraylist is used as it has a flexible size and number of exits can vary making this desirable*/
        ArrayList < Integer > emptyWalls = new ArrayList <Integer>();
        int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
        int states = 0;

        /*Looks in all directions and increments states variable if there is no wall in that direction*/
        for(int j = 0; j<directions.length; j++) {
            if (robot.look(directions[j]) != IRobot.WALL) {
                emptyWalls.add(directions[j]);  //adding non-wall direction to emptyWalls arraylist
                states++;
            }
        }
        int randomNum = randomNumber(states);

        /*Allocates a random direction to face depending on number of non-wall exits*/
        if (states == 2){
            if(randomNum == 0) return emptyWalls.get(0);
            else return emptyWalls.get(1);
        }
        else if (states == 3){
            if(randomNum == 0) return emptyWalls.get(0);
            else if (randomNum == 1) return emptyWalls.get(1);
            else return emptyWalls.get(2);
        }
        else{
            if(randomNum == 0) return emptyWalls.get(0);
            else if (randomNum == 1) return emptyWalls.get(1);
            else if (randomNum == 2) return emptyWalls.get(2);
            else return emptyWalls.get(3);
        }

    }

    /**
     * randomNumber is used to generate a random integer
     *
     * @param range     The number of possible randomly generated integers
     * @return          random integer between 0 and ('range' - 1)
     */
    private int randomNumber(int range){
        int newRand = (int) (Math.random()*range);  //typecast Math.random()*range as int
        return newRand;
    }

    /**
     * beenBeforeExits checks in all four directions to count the number of beenBefore squares
     *
     * @param robot     Object of the IRobot class
     * @return          Number of beenBefore squares surrounding the robot currently
     */
    private int beenBeforeExits(IRobot robot){
        int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
        int beenBeforeSquares = 0;

        /*Looks around to count how many beenBefore squares there are*/
        for(int i = 0; i < 4; i++){
            if(robot.look(directions[i]) == IRobot.BEENBEFORE) {
                beenBeforeSquares++;
            }
        }
        return beenBeforeSquares;
    }

    /**
     * nonWallExits checks in all four directions to count the number of non-Wall exits
     *
     * @param robot     Object of the IRobot class
     * @return          Number of non-Wall exits surrounding the robot currently
     */
    private int nonwallExits(IRobot robot){
        int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
        int availableSpaces = 0;

        /*Looks around the robot in to count how many non-wall spaces there are*/
        for(int i = 0; i < 4; i++){
            if(robot.look(directions[i]) != IRobot.WALL) {
                availableSpaces++;
            }
        }
        return availableSpaces;
    }

    /**
     * passageExits checks in all four directions to count the number of passage exits
     *
     * @param robot     Object of the IRobot class
     * @return          Number of passage exits currently surrounding the robot
     */
    private int passageExits(IRobot robot){
        int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
        int passages = 0;

        /*Looks around the robot in to count how many passages there are*/
        for(int i = 0; i < 4; i++){
            if(robot.look(directions[i]) == IRobot.PASSAGE)
                passages++;
        }
        return passages;
    }

    /**
     * deadEnd is called if the robot is at a dead-end
     * Look around for the only open exit and move in that direction
     *
     * @param robot     Object of the IRobot class
     * @return          Direction for robot to move in
     */
    private int deadEnd(IRobot robot) {
        int result = 0;
        int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};

        /*Looks around the robot to find the only open exit*/
        for (int i = 0; i < 4; i++) {
            if (robot.look(directions[i]) != IRobot.WALL) {
                result = directions[i];
            }
        }
        return result;
    }

    /**
     * corridor is called if the robot is in a corridor
     * If the robot can move ahead without crashing, then move ahead,
     * Otherwise move left or right, whichever does not have a wall
     *
     * @param robot     Object of the IRobot class
     * @return          Direction for robot to move when in a corridor
     */
    private int corridor(IRobot robot) {
        int result = 0;
        if (robot.look(IRobot.AHEAD) != IRobot.WALL) {
            result = IRobot.AHEAD;
        }
        else {
            if (robot.look(IRobot.LEFT) != IRobot.WALL){
                result = IRobot.LEFT;   /*If robot cannot move forward, then either right or left is non-Wall*/
            }
            else result = IRobot.RIGHT;
        }
        return result;
    }

    /**
     * junctionAndCrossroads is called if the robot is either in a junction or crossroad
     * If it is a new junction/crossroad add it to the JunctionInfo arraylist in RobotData class
     * The method will then check if any passages exit surrounding the robot, and choose between those
     * Otherwise it will choose randomly between all non-Wall exits
     *
     * @param robot     Object of the IRobot class
     * @return          Direction for robot to move when in a junction or crossroad
     */
    private int junctionAndCrossroads(IRobot robot){

        /*
        If it is a new junction/crossroad then store it in JunctionInfo arraylist
        * Use the .getLocation() functions as input arguments for the current coordinates
        */
        if(beenBeforeExits(robot) == 1){
            robotData.recordJunction(robot.getLocation().x, robot.getLocation().y, robot.getHeading());
            robotData.printJunction();
        }
        int resultDirection=0;
        int nonPassageExits = 0;
        int[] directions = {IRobot.AHEAD, IRobot.RIGHT, IRobot.LEFT, IRobot.BEHIND};
        ArrayList < Integer > passageExits = new ArrayList <Integer>();

        /*Looks around the robot and stores the relative passage exits along with the number of non-passage spaces*/
        for(int i = 0; i<directions.length; i++) {
            if (robot.look(directions[i]) == IRobot.PASSAGE)
                passageExits.add(directions[i]);
            else if (robot.look(directions[i]) == IRobot.BEENBEFORE)
                nonPassageExits++;
            else continue;
        }

        /*If the robot is in a crossroad*/
        if (nonwallExits(robot) == 4) {
            if(nonPassageExits == 4){

                /*If all directions are beenBefores then choose randomly*/
                resultDirection = randomDirection(robot);
                return resultDirection;     /*return statement so that method is exited here*/
            }
            if (passageExits.size() == 1)
                resultDirection = passageExits.get(0);      /*If only one passage exit is surrounding the robot*/
            else{

                /*If multiple passage exits exist, choose randomly between them*/
                int randomInt = randomNumber(passageExits.size());
                if (randomInt == 0) resultDirection = passageExits.get(0);
                else if (randomInt == 1) resultDirection = passageExits.get(1);
                else if (randomInt == 2 ) resultDirection = passageExits.get(2);
                else resultDirection = passageExits.get(3);
            }
        }

        /*If the robot is in a juction*/
        if (nonwallExits(robot) == 3) {
            if(nonPassageExits == 3){

                /*If all directions are beenBefores then choose randomly*/
                resultDirection = randomDirection(robot);
                return resultDirection;     /*return statement so that method is exited here*/
            }
            if (passageExits.size() == 1)
                resultDirection = passageExits.get(0);      /*If only one passage exit is surrounding the robot*/
            else{

                /*If multiple passage exits exist, choose randomly between them*/
                int randomInt = randomNumber(passageExits.size());
                if (randomInt == 0) resultDirection = passageExits.get(0);
                else if (randomInt == 1) resultDirection = passageExits.get(1);
                else resultDirection = passageExits.get(2);
            }
        }
        return resultDirection;
    }


    /**
     * reset will set the value of explorerMode to 1 to allow robot to explore every time maze is reset
     * It will also set pollRun back to 0
     */
    public void reset() {
        robotData.resetJunctionCounter();
        explorerMode = 1;
        pollRun = 0;
    }

    /**
     *controlRobot is the main method of this IRobot class
     * Creates a new instance of RobotData class if it is the first run of a new maze
     * Decides whether the robot should be exploring or backtracking
     * Increments pollRun every time it is polled
     *
     * @param robot     object of IRobot class
     */
    public void controlRobot(IRobot robot){

        /*This allows for the arraylist of JunctionRecorder objects to be cleared every time a new maze is called*/
        if ((robot.getRuns() == 0) && (pollRun == 0)){
            robotData = new RobotData();
        }
        if(explorerMode == 1)
            exploreControl(robot);
        else backtrackControl(robot);
        pollRun++;
    }

    /**
     *exploreControl is called if robot is in exploring mode
     * It calls the relevant method based on if it is at a dead-end, corridor, junction or a crossroad
     * If it is in a dead-end at the beginning it does not revert into backtracking mode
     * Faces the direction obtained by methods specified above
     *
     * @param robot     Object of IRobot class
     */
    private void exploreControl(IRobot robot){
        int exits = nonwallExits(robot);
        int direction = 0;

        /*Calling relevant method depending on number of non-Wall exits*/
        switch(exits) {
            case (1):
                direction = deadEnd(robot);
                if (pollRun != 0) {
                    explorerMode = 0;       /*Will only backtrack from a dead-end if it is not the beginning*/
                }
                break;
            case (2):
                direction = corridor(robot);
                break;
            case (3):
                direction = junctionAndCrossroads(robot);
                break;
            case (4):
                direction = junctionAndCrossroads(robot);
                break;
        }
        robot.face(direction);
    }

    /**
     *backtrackControl is called if the robot is in backtracking mode
     * If robot is at a dead-end or corridor, it behaves as normal by calling the dead-end/corridor function
     * If the robot begins a maze in a corridor, it will choose a passage exit and set into exploring mode
     * If the robot is at a junction/crossroad and there is a passage exit available, it will choose that and change into exploring mode
     * If the robot is at a junction/crossroad and there isn't a passage exit, it will search for thejunction in the JunctionInfo arraylist
     * and retrieve the heading it arrived from in that juction, and will move in the opposite heading.
     *
     * @param robot
     */
    private void backtrackControl(IRobot robot){
        int initialHeading, newHeading;
        int openExits = nonwallExits(robot);
        int direction = 0;

        /*Calling relevant method depending on number of non-Wall exits*/
        if (openExits == 1) {
            direction = deadEnd(robot);
        }
        else if(openExits == 2){
            direction = corridor(robot);
            if (passageExits(robot) > 0) {      /*This allows the robot to function on a maze where it spawns in a corridor*/
                explorerMode = 1;
            }
        }
        else{
            if (passageExits(robot) > 0) {
                explorerMode = 1;               /*If a passage exit exists then it has not been explored*/
                direction = junctionAndCrossroads(robot);
            }
            else{

                /*If no passage exit exists then it the junction has been fully explored and has been stored in JunctionInfo*/
                initialHeading = robotData.searchJunction(robot.getLocation().x, robot.getLocation().y);
                newHeading = oppositeHeading(initialHeading);       /*Move in opposite heading relative to arrived heading*/
                robot.setHeading(newHeading);
                direction = IRobot.AHEAD;
            }
        }
        robot.face(direction);
    }

    /**
     *oppositeHeading will output the opppsite of the input heading
     *
     * @param heading       Arrived-from Heading
     * @return              Opposite of input heading
     */
    private int oppositeHeading(int heading){
        int finalHeading;

        /*Making use of the integer values of the headings, and the mathematical relationship between them*/
        if (heading == 1000 || heading == 1001) {
            finalHeading = heading + 2;
        }
        else{
            finalHeading = heading - 2;
        }
        return finalHeading;
    }

    /**
     *This is the RobotData class, which contains an arraylist of JunctionRecorder objects
     * Whenever a junction/crossroad is visited for the first time, a new object of type JunctionRecorder is created
     * This object is then added to the JunctionInfo arraylist
     * In this class, the robot can also parse through all existing junction/crossroads using the set of unique co-ordinates
     * in order to find the arrived-from heading for the specified junction/crossroad
     */
    class RobotData{
        private int junctionCounter;
        private ArrayList<JunctionRecorder> junctionInfo = new ArrayList<JunctionRecorder>();

        /**
         * When a new instance of RobotData is created the junctionCounter is reset
         */
        private RobotData(){
            junctionCounter = 0;
        }

        /**
         * When maze has been reset, resetJunctionCounter is called to reset JunctionCounter
         */
        public void resetJunctionCounter() {
            junctionCounter = 0;
        }

        /**
         *recordJunction is called when the robot encounters a new junction/crossroad
         *It creates a new object of type JunctionRecorder every time
         *Next it stores this object in the arraylist JunctionInfo
         * Then it increments junctionCounter to store the number of unique junctions encountered
         *
         * @param xLoc      Current x-coordinate
         * @param yLoc      Current y-coordinate
         * @param head      Arrived-from heading
         */
        private void recordJunction(int xLoc, int yLoc, int head){
            JunctionRecorder junction = new JunctionRecorder(xLoc, yLoc, head);
            junctionInfo.add(junction);
            junctionCounter++;
        }

        /**
         *printJunction is called right after a new junction/crossroad is stored in JunctionInfo arraylist
         * This method takes the x-coordinate, y-coordinate, and the arrived-from heading of the last junction stored in JunctionInfo
         * Then it converts the int value of arrived-from heading into a string equivalent
         * Lastly it prints the coordinate and heading of the the latest unique junction that the robot explored
         */
        private void printJunction(){
            JunctionRecorder latestJunction = junctionInfo.get((junctionInfo.size()-1));    /*The latest junction will have index one less than size*/

            /*Using the getter methods in JunctionRecorder class*/
            int x = latestJunction.getX();
            int y = latestJunction.getY();
            int head = latestJunction.getHeading();

            /*Converting the integer arrived-from headings into equivalent string values*/
            String headingString = "";
            if(head == 1000) headingString = "NORTH";
            else if(head == 1001) headingString = "EAST";
            else if(head == 1002) headingString = "SOUTH";
            else headingString = "WEST";

            /*Print using concatenation*/
            System.out.println("Junction " + junctionCounter + " (x=" + x + ",y=" + y + ") " + "heading " + headingString);
    
        }

        /**
         *searchJunction is called when the robot has no passage exits at junction/crossroad
         * It will use the input coordinates to search the JunctionInfo arraylist for an object with the same x and y
         * Then the value of arrivedHeading for that object will be returned
         *
         * @param x     x-coordinate of robot at fully-explored junction/crossroad
         * @param y     y-coordinate of robot at fully-explored junction/crossroad
         * @return      arrivedHeading for the junctio/Crossroad at specified coordinates
         */
        private int searchJunction(int x, int y){
            for(int i = 0; i<junctionInfo.size(); i++){

                /*Check JunctionInfo arraylist for object with the same x and y values as the input*/
                if((x == junctionInfo.get(i).x) && (y == junctionInfo.get(i).y)){
                    return junctionInfo.get(i).arrivedHeading;
                }
            }

            /*This statement is only to complete the method but will never be executed as the x and y coordinate will always be stored in JunctionInfo*/
            return 0;
        }
    }

    /**
     * Each JunctionRecorder object contains the local state information for each unique junction/crossroad visited
     * This class is called every time a new junction/crossroad is reached, and the getter methods are used to provide the
     * JunctionRecorder object with attributes such as coordinates and the arrived from heading.
     *
     */
    class JunctionRecorder {
        int x;
        int y;
        int arrivedHeading;

        /**
         *JunctionRecorder stores the value of x-coordinate, y-coordinate, and arrivedHeading using this keyword
         *
         * @param x     Value of x-coordinate
         * @param y     Value of y-coordinate
         * @param arrivedHeading        Value of heading first arrived in a junction or crossroad
         */
        public JunctionRecorder(int x, int y, int arrivedHeading){
            this.x = x;
            this.y = y;
            this.arrivedHeading = arrivedHeading;
        }

        /**
         * Getter method to return x-coordinate
         * @return      this.x-coordinate
         */
        public int getX(){
            return this.x;
        }

        /**
         * Getter method to return y-coordinate
         * @return      this.y-coordinate
         */
        public int getY(){
            return this.y;
        }

        /**
         * Getter method to return arrivedHeading
         * @return      this.arrivedHeading
         */
        public int getHeading(){
            return this.arrivedHeading;
        }
    }
}
