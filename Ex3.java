import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Stack;
/*
 * Preamble for Ex3:
 * In this Ex3, having built on Ex2, the code now works for loopy mazes in addition to prim maze.
 * Since, it is based off Ex2, it is very memory efficient, and uses less data storage than Ex1, as mentioned in the previous preamble
 * The reason Ex2 was unable to solve loopy mazes was that it only turned back from a junction/crossroad which it had previously visted
 * only if it was in backtracking mode
 * However in this exercise, I have adapted so that even if a junction has been visited more than once(even if it is not fully explored)
 * the robot will turn back, as long as it is in exploring mode.
 * This means e.g. in a loopy maze, if the robot reaches a crossroad in exploring mode, but it has visited two exits of this crossroad
 * previously, then the robot will move away from this crossroad to explore other parts of the maze.
 * Another adaptation is that if the robot is at a crossroad which has been fully explored(i.e. beenBefore = 4), then
 * the robot will move out of the crossroad with the opposite heading that it entered with.
 * This is why the Ex2 robot was unable to solve loopy maze, as it would have chosen a random direction in this case, instead of backing.
 */
/**
 *This is the main and only class in Ex2
 *It controls all the logic flow, and guides the robot through the maze without collisions, successfully to the target.
 *The robot can solve prim mazes and loopy.
 * All the methods/global variables are set to private since there is only one class
 * This means that they will not be needed to be accessed by other classes, hence keeping them private
 *
 * @author Param Bhatia
 * @since 10-12-2021
 */
public class Ex3 {
    private int pollRun = 0;                /*Incremented after each pass*/
    private int explorerMode = 1;           /*Set robot into exploring mode at the beginning*/

    /*Create a stack to store the arrived-from heading at a junction/crossroad*/
    Stack<Integer> recentHeading = new Stack<>();


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
     * If it is a new junction/crossroad push the current heading to the top of recent heading
     * This will mean that when a junction or crossroad is first encountered, it will add it to the stack
     * Otherwise, if it is a junction that has been visited before, and the robot is in exploring mode
     * it will go in the opposite direction to the heading on the top of the stack
     * It will also set the robot into backtracking mode since it will go backwards
     * If the robot is not at a junction/crossroad (in exploring mode) which has been vistied before
     * it will then check if any passages exit surrounding the robot, and choose between those
     * Otherwise it will choose randomly between all non-Wall exits
     *
     * @param robot     Object of the IRobot class
     * @return          Direction for robot to move when in a junction or crossroad
     */
    private int junctionAndCrossroads(IRobot robot){
        int resultDirection = 0;
        int nonPassageExits =0;
        int[] directions = {IRobot.AHEAD, IRobot.RIGHT, IRobot.LEFT, IRobot.BEHIND};
        ArrayList < Integer > passageExits = new ArrayList <Integer>();

        if(beenBeforeExits(robot) == 1){
            recentHeading.push(robot.getHeading());         /*Adds the heading to the recentHeading stack*/
        }
        else{
            if (explorerMode == 1) {
                robot.setHeading(oppositeHeading(robot.getHeading()));
                resultDirection = IRobot.AHEAD;             /*Goes back if the junction has been visted previously*/
                explorerMode = 0;
                return resultDirection;   
            }
        }

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

                /*If all directions are beenBefores then turn around*/
                robot.setHeading(oppositeHeading(robot.getHeading()));      /*Set robot heading to opposite of current*/
                resultDirection = IRobot.AHEAD;
                return resultDirection;                 /*return statement so that method is exited here*/
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
                return resultDirection;                 /*return statement so that method is exited here*/
            }
            if (passageExits.size() == 1)
                resultDirection = passageExits.get(0);  /*If only one passage exit is surrounding the robot*/
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
     * It will also clear the recentHeading stack every time a maze is reset
     * This will save memory as the stack will be cleared whenever maze is reset
     */
    public void reset() {
        recentHeading.clear();
        explorerMode = 1;
        pollRun = 0;
    }

    /**
     *controlRobot is the main method of this IRobot class
     * Decides whether the robot should be exploring or backtracking
     * Increments pollRun every time it is polled
     *
     * @param robot     Object of IRobot class
     */
    public void controlRobot(IRobot robot){
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
     * If the robot is at a junction/crossroad and there isn't a passage exit, it will retrieve the heading at the top of the recentHeading stack
     * because once the robot fully explores a junction and backtracked to that same junction again it shows that junction does not lead to the target
     * so the robot will take the arrivedHeading for the specified junction/crossroad and head out using the opposite Heading
     * The heading at the top of the recentHeading stack will also be removed in this case because the junction does not lead to the target
     *
     * @param robot         Object of IRobot calss
     */
    private void backtrackControl(IRobot robot){
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
                direction = junctionAndCrossroads(robot);
                explorerMode = 1;               /*If a passage exit exists then it has not been explored*/
            }
            else{

                /*Here the robot has explored a junction/crossroad fully and backtracked to it, so the relevant heading must be removed*/
                int newHeading = oppositeHeading(recentHeading.peek());
                recentHeading.pop();
                robot.setHeading(newHeading);   /*Move in opposite heading relative to arrived heading*/
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
}