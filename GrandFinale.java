import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Stack;
/*
 * Preamble for GrandFinale:
 * I adapted my Ex3 by using a similar, yet not exact method of route B, as I used another stack of the headings after a junction/crossroad
 * Also, if the robot is backtracking through a junction, the top-most heading in solutionHeading will be removed
 * because it is going in the wrong heading
 * In this adapted edition of Ex3, robot will successfully 'learn' the maze in the first run by exploring the maze
 * And in the next run, the robot will trace a path from the start to the target in a much more efficient manner than the first run
 * This will take less time on the second run, which is the criteria stated by NASA
 * The robot will always explore firstly, then it will provide a much shorter, direct even, approach no matter how many times it is reset
 * Even when you create new mazes, the solutionHeading stack will be cleared, and memory will be free to solve new maze.
 *
 * On Prim mazes, the robot will initially explore on the first run, and in the second run it will find the shortest direct route to the target
 * On loopy mazes, the robot will explore on the frist run, and in the second, it will provide a much, albeit not direct, path to the target
 *
 * After thorough testing and using weighted averages, I have concluded that
 * On a prim maze, in the second run, the steps fell to 5 - 10% of the steps in the first run.
 * On a loopy maze, in the second run, the steps fell to ~ 20% of the steps in the first run.
 * This shows that the robot will work on both Prim and loopy mazes without colliding.
 */
/**
 *This is the main and only class in Ex2
 *It controls all the logic flow, and guides the robot through the maze without collisions, successfully to the target.
 *The robot can solve, and find quick & efficient paths through prim mazes and loopy.
 * All the methods/global variables are set to private since there is only one class
 * This means that they will not be needed to be accessed by other classes, hence keeping them private
 *
 * @author Param Bhatia
 * @since 10-12-2021
 */
public class GrandFinale {

    /*Create a stack to store the arrived-from heading at a junction/crossroad*/
    Stack<Integer> recentHeading = new Stack<>();

    /*Create a stack to store the set heading at a junction/crossroad*/
    Stack<Integer> solutionHeading = new Stack<>();

    int index = 0;                          /*Increments at a junction or crossroad when tracing the shorter path*/
    private int pollRun = 0;                /*Incremented after each pass*/
    private int explorerMode = 1;           /*Set robot to exploring mode at the beginning*/

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
            recentHeading.push(robot.getHeading());             /*Adds the heading to the recentHeading stack*/
        }
        else{
            if (explorerMode == 1) {
                robot.setHeading(oppositeHeading(robot.getHeading()));
                resultDirection = IRobot.AHEAD;                 /*Goes back if the junction has been visted previously*/
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
                robot.setHeading(oppositeHeading(robot.getHeading()));
                resultDirection = IRobot.AHEAD;
                return resultDirection;                         /*return statement so that method is exited here*/
            }
            if (passageExits.size() == 1)
            resultDirection = passageExits.get(0);              /*If only one passage exit is surrounding the robot*/
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
                return resultDirection;
            }
            if (passageExits.size() == 1)
                resultDirection = passageExits.get(0);           /*If only one passage exit is surrounding the robot*/
            else{

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
        index = 0;
    }

    /**
     *controlRobot is the main method of this IRobot class
     * If robot is in the first run
     * clear the solution stack in order to clean the memory
     * Decides whether the robot should be exploring or backtracking
     * Increments pollRun every time it is polled
     * If robot is on second or more run
     * Behave as normal for corridor/ dead-end
     * for junctions get the element of the solution stack at the postion of index
     * set the heading to aforementioned stack result
     * increment index
     *
     * @param robot     Object of IRobot class
     */
    public void controlRobot(IRobot robot){
        if(robot.getRuns() == 0){
            if (pollRun == 0 && robot.getRuns() == 0) {
                solutionHeading.clear();
            }
            if(explorerMode == 1)
                exploreControl(robot);
            else backtrackControl(robot);
            pollRun++;
        }
        else{
            int direction;
            int exits = nonwallExits(robot);            /*Behave as normal for corridor and dead-end*/
            if(exits == 1) {
                direction = deadEnd(robot);
            }
            else if(exits == 2){
                direction = corridor(robot);
            }
            else{
                int newHeading = solutionHeading.elementAt(index);  /*Search the solution stack for element of given index*/
                robot.setHeading(newHeading);
                direction = IRobot.AHEAD;
                index++;
            }
            robot.face(direction);
        }
    }

    /**
     *exploreControl is called if robot is in exploring mode
     * It calls the relevant method based on if it is at a dead-end, corridor, junction or a crossroad
     * If it is in a dead-end at the beginning it does not revert into backtracking mode
     * Faces the direction obtained by methods specified above
     * If it is at a junction or crossroad in exploring mode store the result heading in solutionHeading stack
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
                    explorerMode = 0;               /*Will only backtrack from a dead-end if it is not the beginning*/
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
        robot.face(direction);                  /*After facing direction if at crossroad or junction store the heading*/
        if ((exits == 3 || exits == 4) && beenBeforeExits(robot) == 1) {    
            solutionHeading.push(robot.getHeading());
        }
    }

    /**
     *backtrackControl is called if the robot is in backtracking mode
     * If robot is at a dead-end or corridor, it behaves as normal by calling the dead-end/corridor function
     * If the robot is at a junction/crossroad and there is a passage exit available, it will choose that and change into exploring mode
     * It will then remove the top most heading in the solutionHeading stack, as this indicates it was not the right heading
     * If the robot is at a junction/crossroad and there isn't a passage exit, it will retrieve the heading at the top of the recentHeading stack
     * because once the robot fully explores a junction and backtracked to that same junction again it shows that junction does not lead to the target
     * so the robot will take the arrivedHeading for the specified junction/crossroad and head out using the opposite Heading
     * The heading at the top of the recentHeading stack will also be removed in this case because the junction does not lead to the target
     * It will also then remove the heading in the top of solutionHeading stack as it is not the right heading to go in
     *
     * Lastly, after facing robot in specified direction, if it is in a junction/crossroad and there is a passage exit
     * push the current heading into solutionHeading stack, because it can now follow this specified path.
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
        }
        else{

            if (passageExits(robot) > 0) {
                solutionHeading.pop();      /*Remove from stack as it would be the incorrect heading*/
                direction = junctionAndCrossroads(robot);
                explorerMode = 1;       /*If a passage exit exists then it has not been explored*/
            }
            else{

                solutionHeading.pop();      /*Remove from stack as it would be the incorrect heading*/
                int newHeading = oppositeHeading(recentHeading.peek());
                recentHeading.pop();
                robot.setHeading(newHeading);   /*Move in opposite heading relative to arrived heading*/
                direction = IRobot.AHEAD;
            }
        }
        robot.face(direction);
        if (openExits > 2 && passageExits(robot) > 0) {
            solutionHeading.push(robot.getHeading());
        }
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